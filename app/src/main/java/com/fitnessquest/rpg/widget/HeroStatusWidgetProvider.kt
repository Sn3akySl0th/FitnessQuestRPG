package com.fitnessquest.rpg.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.fitnessquest.rpg.FitQuestApp
import com.fitnessquest.rpg.MainActivity
import com.fitnessquest.rpg.R
import com.fitnessquest.rpg.data.db.AppDatabase
import com.fitnessquest.rpg.data.db.CharacterEntity
import com.fitnessquest.rpg.data.db.ItemEntity
import com.fitnessquest.rpg.data.db.ItemSlot
import com.fitnessquest.rpg.domain.CharacterClass
import com.fitnessquest.rpg.domain.GameMath
import com.fitnessquest.rpg.ui.components.AvatarDetail
import com.fitnessquest.rpg.ui.components.AvatarExpression
import com.fitnessquest.rpg.ui.components.AvatarFocus
import com.fitnessquest.rpg.ui.components.AvatarFrame
import com.fitnessquest.rpg.ui.components.AvatarPainter
import com.fitnessquest.rpg.ui.components.lookFor
import com.fitnessquest.rpg.ui.components.toAppearance
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HeroStatusWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        updateWidgets(context.applicationContext, manager, widgetIds)
    }

    override fun onEnabled(context: Context) {
        refresh(context)
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun refresh(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val widgetIds = manager.getAppWidgetIds(
                ComponentName(appContext, HeroStatusWidgetProvider::class.java)
            )
            updateWidgets(appContext, manager, widgetIds)
        }

        private fun updateWidgets(
            context: Context,
            manager: AppWidgetManager,
            widgetIds: IntArray
        ) {
            if (widgetIds.isEmpty()) return
            scope.launch {
                try {
                    val db = AppDatabase.get(context)
                    val character = db.characterDao().get() ?: CharacterEntity()
                    val container = (context.applicationContext as FitQuestApp).container
                    val gear = container.repository.equippedGear(character)
                    val maxEnergy = container.repository.getMaxEnergy()

                    val readiness = readinessFor(character)
                    val avatarBitmap = renderAvatar(context, character, gear)

                    widgetIds.forEach { widgetId ->
                        manager.updateAppWidget(widgetId, remoteViews(context, character, readiness, avatarBitmap, maxEnergy))
                    }
                } catch (e: Exception) {
                    // silent fail
                }
            }
        }

        private fun remoteViews(
            context: Context,
            character: CharacterEntity,
            readiness: WidgetReadiness,
            avatar: Bitmap?,
            maxEnergy: Int = GameMath.MAX_ENERGY
        ): RemoteViews {
            val energy = character.energy.coerceIn(0, maxEnergy)
            val xpToNext = GameMath.xpToNextLevel(character.level)
            val xpProgress = if (xpToNext > 0) (character.xp.toFloat() / xpToNext * 100).toInt() else 0

            return RemoteViews(context.packageName, R.layout.widget_hero_status).apply {
                setTextViewText(R.id.widgetTitle, character.name.ifBlank { "FitQuest Hero" })
                setTextViewText(R.id.widgetLevel, "Level ${character.level} ${character.characterClass?.label ?: "Adventurer"}")
                setTextViewText(R.id.widgetGold, "💰 ${character.gold}")
                
                setProgressBar(R.id.widgetXpBar, 100, xpProgress.coerceIn(0, 100), false)
                setProgressBar(R.id.widgetEnergyBar, maxEnergy, energy, false)
                
                setTextViewText(R.id.widgetStreak, "🔥 ${character.streak}d Streak")
                
                if (avatar != null) {
                    setImageViewBitmap(R.id.widgetAvatar, avatar)
                }

                setOnClickPendingIntent(R.id.widgetRoot, openAppIntent(context))
            }
        }

        private fun renderAvatar(
            context: Context,
            character: CharacterEntity,
            gear: Map<ItemSlot, ItemEntity>
        ): Bitmap? {
            return try {
                val density = context.resources.displayMetrics.density
                // Standard avatar unit scale (100x120 area)
                val width = (80 * density).toInt()
                val height = (96 * density).toInt() 
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                
                val drawScope = CanvasDrawScope()
                val clazz = character.characterClass ?: CharacterClass.WARRIOR
                
                val frame = AvatarFrame(
                    u = width / 100f, 
                    cls = clazz,
                    look = lookFor(clazz),
                    gear = gear,
                    costume = false,
                    highlightMuscles = emptySet(),
                    facingBack = false,
                    appearance = character.toAppearance(),
                    expression = AvatarExpression.CALM,
                    detail = AvatarDetail.FULL,
                    phase = 0f
                )

                drawScope.draw(
                    density = Density(density),
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = androidx.compose.ui.graphics.Canvas(canvas),
                    size = Size(width.toFloat(), height.toFloat())
                ) {
                    with(AvatarPainter) {
                        // Drawing with no focus transform to show the full 120u height
                        draw(frame, focus = AvatarFocus.FULL_BODY)
                    }
                }
                bitmap
            } catch (e: Exception) {
                null
            }
        }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun readinessFor(character: CharacterEntity): WidgetReadiness {
            if (character.lastWorkoutDay == 0L) {
                return WidgetReadiness("Ready", "Start your first quest")
            }

            val daysSince = (LocalDate.now().toEpochDay() - character.lastWorkoutDay).toInt()
            return when {
                daysSince <= 0 && character.streak >= 5 -> WidgetReadiness("Deload", "Protect the streak")
                daysSince <= 1 && character.energy >= 65 -> WidgetReadiness("Heroic", "Energy is high")
                daysSince >= 3 -> WidgetReadiness("Ready", "Return to training")
                else -> WidgetReadiness("Normal", "Steady quest pace")
            }
        }
    }
}

private data class WidgetReadiness(
    val label: String,
    val message: String
)
