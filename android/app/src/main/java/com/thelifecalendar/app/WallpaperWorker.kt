package com.thelifecalendar.app

import android.app.WallpaperManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Draws today's wallpaper from the saved config, applies it, and schedules the next midnight run. */
class WallpaperWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val ctx = applicationContext
        val cfg = Prefs.load(ctx)
        try {
            val (w, h) = deviceSize(ctx, cfg.device)
            val bmp = Renderer.render(cfg, w, h)
            val wm = WallpaperManager.getInstance(ctx)
            val flag = when (cfg.target) {
                Target.LOCK -> WallpaperManager.FLAG_LOCK
                Target.HOME -> WallpaperManager.FLAG_SYSTEM
                Target.BOTH -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
            }
            wm.setBitmap(bmp, null, true, flag)
            bmp.recycle()
        } catch (e: Exception) {
            // still reschedule so one failure doesn't stop the daily updates
        }
        if (cfg.autoUpdate) {
            val next = if (inputData.getString(KEY_SLOT) == "a") "b" else "a"
            Scheduler.scheduleMidnight(ctx, next)
        }
        Result.success()
    }

    companion object {
        const val KEY_SLOT = "slot"
    }
}

object Scheduler {
    private fun name(slot: String) = "daily_$slot"

    /** Applies the wallpaper right now and (if enabled) starts the daily midnight cycle. */
    fun applyNow(ctx: Context, cfg: WallpaperConfig) {
        val wm = WorkManager.getInstance(ctx)
        wm.cancelUniqueWork(name("a"))
        wm.cancelUniqueWork(name("b"))
        // slot "b" => the worker schedules the next run as "a"
        val req = OneTimeWorkRequestBuilder<WallpaperWorker>()
            .setInputData(workDataOf(WallpaperWorker.KEY_SLOT to "b"))
            .build()
        wm.enqueueUniqueWork("apply_now", ExistingWorkPolicy.REPLACE, req)
    }

    /** Queues one run shortly after the next local midnight. Slots alternate so a run never replaces itself. */
    fun scheduleMidnight(ctx: Context, slot: String) {
        val now = LocalDateTime.now()
        val next = now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(2)
        val delay = Duration.between(now, next).toMillis().coerceAtLeast(60_000L)
        val data: Data = workDataOf(WallpaperWorker.KEY_SLOT to slot)
        val req = OneTimeWorkRequestBuilder<WallpaperWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(name(slot), ExistingWorkPolicy.REPLACE, req)
    }
}

