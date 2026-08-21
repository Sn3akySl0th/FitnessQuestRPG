package com.fitnessquest.rpg.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.ui.components.AvatarDetail
import com.fitnessquest.rpg.ui.components.AvatarExpression
import com.fitnessquest.rpg.ui.components.AvatarFocus
import com.fitnessquest.rpg.ui.components.AvatarFrame
import com.fitnessquest.rpg.ui.components.AvatarPainter
import com.fitnessquest.rpg.ui.components.lookFor
import com.fitnessquest.rpg.ui.components.toAppearance
import com.fitnessquest.shared.wear.WearPaths
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat(val width: Int, val height: Int, val label: String) {
    WALLPAPER(1080, 1920, "Phone Wallpaper"),
    WATCH_FACE(1080, 1080, "Watch Face Graphic")
}

object AvatarExporter {

    fun generateAvatarBytes(
        context: Context,
        character: CharacterEntity,
        cls: CharacterClass,
        gear: Map<ItemSlot, ItemEntity> = emptyMap()
    ): ByteArray {
        val width = 360f
        val height = 360f
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val androidCanvas = Canvas(bitmap)

        val density = context.resources.displayMetrics.density
        val drawScope = CanvasDrawScope()
        val frame = AvatarFrame(
            u = width / 100f,
            cls = cls,
            look = lookFor(cls),
            gear = gear,
            costume = false,
            highlightMuscles = emptySet(),
            facingBack = false,
            appearance = character.toAppearance(),
            expression = AvatarExpression.BATTLE_READY,
            detail = AvatarDetail.FULL,
            phase = 0f
        )

        drawScope.draw(
            density = Density(density),
            layoutDirection = LayoutDirection.Ltr,
            canvas = androidx.compose.ui.graphics.Canvas(androidCanvas),
            size = Size(width, height)
        ) {
            with(AvatarPainter) {
                draw(frame, focus = AvatarFocus.FULL_BODY)
            }
        }

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
        return baos.toByteArray()
    }

    suspend fun syncAvatarToWear(
        context: Context,
        character: CharacterEntity,
        cls: CharacterClass,
        gear: Map<ItemSlot, ItemEntity> = emptyMap()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = generateAvatarBytes(context, character, cls, gear)

            // 1. DataClient layer
            val dataMapReq = PutDataMapRequest.create(WearPaths.AVATAR_WATCH_FACE).apply {
                dataMap.putByteArray("image", bytes)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }
            val putDataReq = dataMapReq.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(putDataReq).await()

            // 2. Direct instant MessageClient transmission to all connected watch nodes
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            for (node in nodes) {
                runCatching {
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, WearPaths.AVATAR_WATCH_FACE, bytes)
                        .await()
                }
            }
            Unit
        }
    }

    suspend fun exportAvatarGraphic(
        context: Context,
        character: CharacterEntity,
        cls: CharacterClass,
        format: ExportFormat = ExportFormat.WALLPAPER,
        gear: Map<ItemSlot, ItemEntity> = emptyMap()
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val (width, height) = if (format == ExportFormat.WALLPAPER) {
                val metrics = context.resources.displayMetrics
                val screenW = metrics.widthPixels.coerceAtLeast(1080)
                val screenH = metrics.heightPixels.coerceAtLeast(1920)
                screenW to screenH
            } else {
                format.width to format.height
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Full-bleed Background Gradient
            val bgPaint = Paint().apply {
                shader = if (format == ExportFormat.WATCH_FACE) {
                    RadialGradient(
                        width / 2f, height / 2f, width * 0.7f,
                        Color.parseColor("#2A1F3D"), Color.parseColor("#0A0912"),
                        Shader.TileMode.CLAMP
                    )
                } else {
                    LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        intArrayOf(
                            Color.parseColor("#17112B"),
                            Color.parseColor("#0E0B1A"),
                            Color.parseColor("#050408")
                        ),
                        floatArrayOf(0.0f, 0.5f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Watch Face Bezel Border (only on circular watch faces, not phone wallpapers)
            if (format == ExportFormat.WATCH_FACE) {
                val borderPaint = Paint().apply {
                    color = Color.parseColor("#D4AF37")
                    style = Paint.Style.STROKE
                    strokeWidth = 12f
                    isAntiAlias = true
                }
                val inset = 24f
                canvas.drawCircle(width / 2f, height / 2f, (width / 2f) - inset, borderPaint)
            }

            // Safe Zone Layout Calculation
            val isWallpaper = format == ExportFormat.WALLPAPER
            val topMargin = if (isWallpaper) height * 0.12f else 130f
            val titleSize = if (isWallpaper) (width * 0.056f).coerceIn(54f, 80f) else 52f
            val subtitleSize = if (isWallpaper) (width * 0.038f).coerceIn(36f, 54f) else 36f

            // Hero Header Text
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = titleSize
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#F5D77F")
                textSize = subtitleSize
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText(character.name, width / 2f, topMargin, titlePaint)
            canvas.drawText(
                "Level ${character.level} ${cls.label}",
                width / 2f,
                topMargin + (subtitleSize * 1.35f),
                subtitlePaint
            )

            val headerBottom = topMargin + (subtitleSize * 2.0f)

            // RENDER THE FULL DETAILED CHARACTER AVATAR SPRITE
            val avatarWidth = if (format == ExportFormat.WATCH_FACE) 620f else (width * 0.72f).coerceIn(600f, 920f)
            val avatarHeight = avatarWidth * 1.2f
            val avatarBitmap = Bitmap.createBitmap(avatarWidth.toInt(), avatarHeight.toInt(), Bitmap.Config.ARGB_8888)
            val avatarCanvas = Canvas(avatarBitmap)

            val density = context.resources.displayMetrics.density
            val drawScope = CanvasDrawScope()
            val frame = AvatarFrame(
                u = avatarWidth / 100f,
                cls = cls,
                look = lookFor(cls),
                gear = gear,
                costume = false,
                highlightMuscles = emptySet(),
                facingBack = false,
                appearance = character.toAppearance(),
                expression = AvatarExpression.VICTORIOUS,
                detail = AvatarDetail.FULL,
                phase = 0f
            )

            drawScope.draw(
                density = Density(density),
                layoutDirection = LayoutDirection.Ltr,
                canvas = androidx.compose.ui.graphics.Canvas(avatarCanvas),
                size = Size(avatarWidth, avatarHeight)
            ) {
                with(AvatarPainter) {
                    draw(frame, focus = AvatarFocus.FULL_BODY)
                }
            }

            // Draw rendered avatar sprite centered between header and dock safe area
            val dockTop = if (isWallpaper) height * 0.76f else height - 160f
            val availableMidHeight = (dockTop - headerBottom).coerceAtLeast(avatarHeight)
            val avatarLeft = (width - avatarWidth) / 2f
            val avatarTop = if (format == ExportFormat.WATCH_FACE) {
                200f
            } else {
                headerBottom + ((availableMidHeight - avatarHeight) * 0.35f).coerceAtLeast(16f)
            }
            canvas.drawBitmap(avatarBitmap, avatarLeft, avatarTop, Paint().apply { isFilterBitmap = true })

            // Hero Badge / Stats Bar
            val badgePaint = Paint().apply {
                color = Color.parseColor("#2A1F3D")
                style = Paint.Style.FILL
            }
            val badgeBorderPaint = Paint().apply {
                color = Color.parseColor("#5A457D")
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            val badgeHeight = if (format == ExportFormat.WATCH_FACE) 80f else (height * 0.042f).coerceIn(80f, 110f)
            val badgeY = if (format == ExportFormat.WATCH_FACE) {
                height - 160f
            } else {
                (avatarTop + avatarHeight + (height * 0.015f)).coerceAtMost(dockTop - badgeHeight)
            }
            val badgeRect = RectF(
                width * 0.10f,
                badgeY,
                width * 0.90f,
                badgeY + badgeHeight
            )
            canvas.drawRoundRect(badgeRect, 28f, 28f, badgePaint)
            canvas.drawRoundRect(badgeRect, 28f, 28f, badgeBorderPaint)

            val badgeTextSize = if (format == ExportFormat.WATCH_FACE) 30f else (badgeHeight * 0.38f).coerceIn(32f, 44f)
            val badgeTextPaint = Paint().apply {
                color = Color.parseColor("#E0D6F5")
                textSize = badgeTextSize
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "⚔️ STR ${character.strength}  🛡️ DEF ${character.agility}  ❤️ HP ${character.endurance * 10}",
                width / 2f,
                badgeY + (badgeHeight * 0.65f),
                badgeTextPaint
            )

            // Save to Pictures Directory
            val picturesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "FitnessQuest"
            )
            if (!picturesDir.exists()) picturesDir.mkdirs()

            val fileName = "FitnessQuest_Hero_${character.name}_${format.name.lowercase()}_${System.currentTimeMillis()}.png"
            val outputFile = File(picturesDir, fileName)

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Sync to Wear OS Watch Data Layer if Watch Face format
            if (format == ExportFormat.WATCH_FACE) {
                runCatching {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val bytes = baos.toByteArray()

                    val dataMapReq = PutDataMapRequest.create(WearPaths.AVATAR_WATCH_FACE).apply {
                        dataMap.putByteArray("image", bytes)
                        dataMap.putLong("timestamp", System.currentTimeMillis())
                    }
                    val putDataReq = dataMapReq.asPutDataRequest().setUrgent()
                    Wearable.getDataClient(context).putDataItem(putDataReq).await()
                }
            }

            // Notify MediaStore Gallery
            MediaScannerConnection.scanFile(
                context,
                arrayOf(outputFile.absolutePath),
                arrayOf("image/png"),
                null
            )

            val contentUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outputFile
                )
            } catch (_: Exception) {
                Uri.fromFile(outputFile)
            }

            contentUri
        }
    }

    fun launchSetWallpaperIntent(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                setDataAndType(uri, "image/*")
                putExtra("mimeType", "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Set Hero Wallpaper / Watch Face"))
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Hero Watch Face Graphic"))
        }
    }
}
