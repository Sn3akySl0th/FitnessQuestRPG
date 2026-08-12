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

    suspend fun exportAvatarGraphic(
        context: Context,
        character: CharacterEntity,
        cls: CharacterClass,
        format: ExportFormat = ExportFormat.WALLPAPER,
        gear: Map<ItemSlot, ItemEntity> = emptyMap()
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val width = format.width
            val height = format.height

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background Gradient
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
                        Color.parseColor("#1B1429"), Color.parseColor("#0A0912"),
                        Shader.TileMode.CLAMP
                    )
                }
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Outer Frame Border
            val borderPaint = Paint().apply {
                color = Color.parseColor("#D4AF37")
                style = Paint.Style.STROKE
                strokeWidth = if (format == ExportFormat.WATCH_FACE) 12f else 16f
                isAntiAlias = true
            }
            val inset = if (format == ExportFormat.WATCH_FACE) 24f else 36f
            if (format == ExportFormat.WATCH_FACE) {
                canvas.drawCircle(width / 2f, height / 2f, (width / 2f) - inset, borderPaint)
            } else {
                canvas.drawRoundRect(RectF(inset, inset, width - inset, height - inset), 48f, 48f, borderPaint)
            }

            // Hero Header Text
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = if (format == ExportFormat.WATCH_FACE) 52f else 64f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#F5D77F")
                textSize = if (format == ExportFormat.WATCH_FACE) 36f else 44f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val startY = if (format == ExportFormat.WALLPAPER) 260f else 140f
            canvas.drawText(character.name, width / 2f, startY, titlePaint)
            canvas.drawText("Level ${character.level} ${cls.label}", width / 2f, startY + (if (format == ExportFormat.WATCH_FACE) 48f else 64f), subtitlePaint)

            // RENDER THE FULL DETAILED CHARACTER AVATAR SPRITE
            val avatarWidth = if (format == ExportFormat.WATCH_FACE) 620f else 840f
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

            // Draw rendered avatar sprite onto main graphic canvas
            val avatarLeft = (width - avatarWidth) / 2f
            val avatarTop = if (format == ExportFormat.WATCH_FACE) 200f else 380f
            canvas.drawBitmap(avatarBitmap, avatarLeft, avatarTop, Paint().apply { isFilterBitmap = true })

            // Hero Badge / Stats Bar
            val badgePaint = Paint().apply {
                color = Color.parseColor("#2A1F3D")
                style = Paint.Style.FILL
            }
            val badgeY = if (format == ExportFormat.WATCH_FACE) height - 160f else startY + 1140f
            val badgeRect = RectF(
                width * 0.12f,
                badgeY,
                width * 0.88f,
                badgeY + (if (format == ExportFormat.WATCH_FACE) 80f else 100f)
            )
            canvas.drawRoundRect(badgeRect, 24f, 24f, badgePaint)

            val badgeTextPaint = Paint().apply {
                color = Color.parseColor("#E0D6F5")
                textSize = if (format == ExportFormat.WATCH_FACE) 30f else 36f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "⚔️ STR ${character.strength}  🛡️ DEF ${character.agility}  ❤️ HP ${character.endurance * 10}",
                width / 2f,
                badgeY + (if (format == ExportFormat.WATCH_FACE) 52f else 62f),
                badgeTextPaint
            )

            // Save to Pictures Directory
            val picturesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "FitQuest"
            )
            if (!picturesDir.exists()) picturesDir.mkdirs()

            val fileName = "FitQuest_Hero_${character.name}_${format.name.lowercase()}_${System.currentTimeMillis()}.png"
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
