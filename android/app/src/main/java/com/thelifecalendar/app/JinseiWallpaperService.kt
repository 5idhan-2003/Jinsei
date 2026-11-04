package com.thelifecalendar.app

import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.time.LocalDate

class JinseiWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return JinseiEngine()
    }

    inner class JinseiEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var lastDrawnDate: LocalDate? = null
        private var width: Int = 1216
        private var height: Int = 2640

        private val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                drawFrame()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            surfaceHolder.setFormat(PixelFormat.RGBA_8888)

            // Listen directly to preferences for instant, in-process configuration updates
            try {
                Prefs.getPreferences(applicationContext)
                    .registerOnSharedPreferenceChangeListener(this)
            } catch (e: Exception) {
                // Ignore
            }

            val filter = IntentFilter(ACTION_UPDATE)
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(updateReceiver, filter)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                Prefs.getPreferences(applicationContext)
                    .unregisterOnSharedPreferenceChangeListener(this)
            } catch (e: Exception) {
                // Ignore
            }
            try {
                unregisterReceiver(updateReceiver)
            } catch (e: Exception) {
                // Ignore
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            drawFrame()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = w
            height = h
            drawFrame()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                // Always render current state when screen wakes up or lock screen is displayed
                drawFrame()
            }
        }

        override fun onComputeColors(): WallpaperColors? {
            if (Build.VERSION.SDK_INT >= 27) {
                val cfg = Prefs.load(applicationContext)
                val today = LocalDate.now()
                // Render sample bitmap so Android framework natively calculates Palette and Dark Text Hints
                val sampleBmp = Renderer.render(cfg, 300, 600, today)
                val colors = WallpaperColors.fromBitmap(sampleBmp)
                sampleBmp.recycle()
                return colors
            }
            return null
        }

        fun drawFrame() {
            val holder = surfaceHolder ?: return
            val today = LocalDate.now()
            lastDrawnDate = today

            val cfg = Prefs.load(applicationContext)
            val w = if (width > 0) width else 1216
            val h = if (height > 0) height else 2640

            val bmp = Renderer.render(cfg, w, h, today)
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    canvas.drawBitmap(bmp, 0f, 0f, null)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            bmp.recycle()

            if (Build.VERSION.SDK_INT >= 27) {
                try {
                    notifyColorsChanged()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    companion object {
        const val ACTION_UPDATE = "com.thelifecalendar.app.UPDATE_WALLPAPER"

        fun notifyUpdate(context: Context) {
            val intent = Intent(ACTION_UPDATE).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }
}
