package com.thelifecalendar.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** What to draw: derived from the config and today's date. */
private class Spec(
    val total: Int,
    /** index of today's dot; -1 = not started, >= total = finished */
    val todayIdx: Int,
    val cols: Int,
    val widthFrac: Float,
    val dotRatio: Float,
    val title: String?,
    val statsLeft: String,
    val statsRest: String,
    val bg: Int,
    val past: Int,
    val future: Int
)

object Renderer {
    // Rich, deep AMOLED-friendly dark palette
    private const val BLACK = 0xFF000000.toInt()
    private const val DEEP_CHARCOAL = 0xFF0E0E10.toInt() // Darker, sleeker background
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val GRAY_TEXT = 0xFF8E8E93.toInt()
    private const val FUTURE_DOT = 0xFF262628.toInt() // Muted subtle future dots
    private const val FUTURE_LIFE = 0xFF1E1E22.toInt()

    private fun stats(total: Int, idx: Int, unit: String): Pair<String, String> {
        val elapsed = (idx + 1).coerceIn(0, total)
        val left = total - elapsed
        val pct = elapsed * 100 / total
        return "$left$unit left" to "  \u00B7  $pct%"
    }

    private fun spec(cfg: WallpaperConfig, today: LocalDate): Spec = when (cfg.mode) {
        Mode.YEAR -> {
            val y = if (cfg.year == 0) today.year else cfg.year
            val total = Year.of(y).length()
            val idx = when {
                y < today.year -> total
                y > today.year -> -1
                else -> today.dayOfYear - 1
            }
            val (a, b) = stats(total, idx, "d")
            Spec(total, idx, 15, 0.72f, 0.34f, null, a, b, BLACK, WHITE, FUTURE_DOT)
        }
        Mode.GOAL -> {
            val start = parseDate(cfg.goalStart) ?: today
            var end = parseDate(cfg.goalEnd) ?: start.plusDays(1)
            if (!end.isAfter(start)) end = start.plusDays(1)
            val total = ChronoUnit.DAYS.between(start, end).toInt()
            val idx = ChronoUnit.DAYS.between(start, today).toInt()
            val cols = if (total <= 210) 15 else ceil(sqrt(total * 0.75)).toInt()
            val (a, b) = stats(total, idx, "d")
            Spec(total, idx, cols, 0.63f, 0.31f, cfg.goalTitle, a, b, DEEP_CHARCOAL, WHITE, FUTURE_DOT)
        }
        Mode.LIFE -> {
            val dob = parseDate(cfg.dob) ?: today
            val total = cfg.lifespan.coerceIn(1, 120) * 52
            val idx = (ChronoUnit.DAYS.between(dob, today) / 7).toInt()
            val (a, b) = stats(total, idx, " weeks")
            Spec(total, idx, 52, 0.70f, 0.38f, null, a, b, BLACK, WHITE, FUTURE_LIFE)
        }
    }

    fun render(
        cfg: WallpaperConfig,
        w: Int,
        h: Int,
        today: LocalDate = LocalDate.now()
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = spec(cfg, today)
        c.drawColor(s.bg)

        val wf = w.toFloat()
        val hf = h.toFloat()
        val rows = ceil(s.total.toDouble() / s.cols).toInt()

        val titleSize = wf * 0.030f
        val statsSize = wf * 0.024f
        val titleH = if (s.title != null) titleSize + wf * 0.038f else 0f
        val statsH = wf * 0.030f + statsSize

        var step = wf * s.widthFrac / s.cols

        // Shift down so it sits comfortably BELOW clock + lock screen widgets
        // Widgets on OnePlus 13s end around 38-40% down the screen.
        // Starting at 44% (Goal) or 40% (Year/Life) gives clean, intentional negative space!
        val minTopFrac = if (s.cols == 52) 0.38f else 0.44f
        var top = max(hf * minTopFrac, hf * 0.52f - (titleH + step * rows + statsH) / 2f)

        // Ensure clearance from bottom lockscreen player / shortcuts
        step = min(step, (hf * 0.86f - top - titleH - statsH) / rows)
        top = max(hf * minTopFrac, min(top, hf * 0.52f - (titleH + step * rows + statsH) / 2f))

        val gridTop = top + titleH
        val gridBottom = gridTop + step * rows
        val x0 = (wf - step * s.cols) / 2f
        val radius = step * s.dotRatio

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val face = Typeface.create("sans-serif", Typeface.NORMAL)

        if (s.title != null) {
            paint.typeface = face
            paint.textSize = titleSize
            paint.color = GRAY_TEXT
            val tw = paint.measureText(s.title)
            c.drawText(s.title, (wf - tw) / 2f, top + titleSize, paint)
        }

        for (i in 0 until s.total) {
            val col = i % s.cols
            val row = i / s.cols
            val cx = x0 + step * (col + 0.5f)
            val cy = gridTop + step * (row + 0.5f)
            paint.color = when {
                i < s.todayIdx -> s.past
                i == s.todayIdx -> cfg.accent
                else -> s.future
            }
            c.drawCircle(cx, cy, radius, paint)
        }

        paint.typeface = face
        paint.textSize = statsSize
        val wLeft = paint.measureText(s.statsLeft)
        val wRest = paint.measureText(s.statsRest)
        val sx = (wf - (wLeft + wRest)) / 2f
        val sy = gridBottom + wf * 0.030f + statsSize
        paint.color = cfg.accent
        c.drawText(s.statsLeft, sx, sy, paint)
        paint.color = GRAY_TEXT
        c.drawText(s.statsRest, sx + wLeft, sy, paint)

        return bmp
    }
}
