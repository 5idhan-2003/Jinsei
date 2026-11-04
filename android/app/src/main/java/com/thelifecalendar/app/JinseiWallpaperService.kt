package com.thelifecalendar.app

import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.time.LocalDate

class JinseiWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return JinseiEngine()
    }

    inner class JinseiEngine : Engine() {
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
                unregisterReceiver(updateReceiver)
            } catch (e: Exception) {
                // Ignore
            }
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
                // Check if midnight passed; update dots seamlessly without user intervention
                val today = LocalDate.now()
                if (today != lastDrawnDate) {
                    drawFrame()
                }
            }
        }

        // Return null so ColorOS and Android Monet never extract colors to alter your phone theme!
        override fun onComputeColors(): WallpaperColors? {
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
        }
    }

    companion object {
        const val ACTION_UPDATE = "com.thelifecalendar.app.UPDATE_WALLPAPER"

        fun notifyUpdate(context: Context) {
            context.sendBroadcast(Intent(ACTION_UPDATE))
        }
    }
}

