package com.thelifecalendar.app

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.time.LocalDate

enum class Mode(val label: String) { LIFE("Life"), YEAR("Year"), GOAL("Goal") }

enum class Theme(val label: String) { DARK("Dark"), LIGHT("Light") }

enum class Target(val label: String) {
    LOCK("Lock screen"),
    HOME("Home screen"),
    BOTH("Both")
}

enum class Device(val label: String, val w: Int, val h: Int) {
    ONEPLUS_13S("OnePlus 13s", 1216, 2640),
    ONEPLUS_13("OnePlus 13", 1440, 3168),
    AUTO("This phone", 0, 0)
}

/** Every user-editable field lives here, so new options can be added in one place later. */
data class WallpaperConfig(
    val mode: Mode = Mode.YEAR,
    val theme: Theme = Theme.DARK,
    // Life calendar
    val dob: String = "2000-01-01",
    val lifespan: Int = 90,
    // Year calendar (0 = always the current year)
    val year: Int = 0,
    // Goal calendar
    val goalTitle: String = "My Goal",
    val goalStart: String = LocalDate.now().toString(),
    val goalEnd: String = LocalDate.now().plusDays(90).toString(),
    // Shared
    val device: Device = Device.ONEPLUS_13S,
    val target: Target = Target.LOCK,
    val accent: Int = 0xFFFF5B35.toInt(),
    val autoUpdate: Boolean = true
)

fun parseDate(s: String): LocalDate? = runCatching { LocalDate.parse(s) }.getOrNull()

private inline fun <reified T : Enum<T>> enumOr(s: String?, def: T): T =
    runCatching { enumValueOf<T>(s!!) }.getOrDefault(def)

object Prefs {
    const val PREFS_NAME = "wallpaper_config"

    fun getPreferences(ctx: Context): android.content.SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(ctx: Context): WallpaperConfig {
        val p = getPreferences(ctx)
        val d = WallpaperConfig()
        return WallpaperConfig(
            mode = enumOr(p.getString("mode", null), d.mode),
            theme = enumOr(p.getString("theme", null), d.theme),
            dob = p.getString("dob", d.dob) ?: d.dob,
            lifespan = p.getInt("lifespan", d.lifespan),
            year = p.getInt("year", d.year),
            goalTitle = p.getString("goalTitle", d.goalTitle) ?: d.goalTitle,
            goalStart = p.getString("goalStart", d.goalStart) ?: d.goalStart,
            goalEnd = p.getString("goalEnd", d.goalEnd) ?: d.goalEnd,
            device = enumOr(p.getString("device", null), d.device),
            target = enumOr(p.getString("target", null), d.target),
            accent = p.getInt("accent", d.accent),
            autoUpdate = p.getBoolean("autoUpdate", d.autoUpdate)
        )
    }

    fun save(ctx: Context, c: WallpaperConfig) {
        getPreferences(ctx).edit()
            .putString("mode", c.mode.name)
            .putString("theme", c.theme.name)
            .putString("dob", c.dob)
            .putInt("lifespan", c.lifespan)
            .putInt("year", c.year)
            .putString("goalTitle", c.goalTitle)
            .putString("goalStart", c.goalStart)
            .putString("goalEnd", c.goalEnd)
            .putString("device", c.device.name)
            .putString("target", c.target.name)
            .putInt("accent", c.accent)
            .putBoolean("autoUpdate", c.autoUpdate)
            .apply()
    }
}

/** Pixel size of the wallpaper for the chosen device profile. */
fun deviceSize(ctx: Context, d: Device): Pair<Int, Int> {
    if (d != Device.AUTO) return d.w to d.h
    val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= 30) {
        val b = wm.maximumWindowMetrics.bounds
        b.width() to b.height()
    } else {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        dm.widthPixels to dm.heightPixels
    }
}
