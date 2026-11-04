package com.thelifecalendar.app

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0A0C),
                    surface = Color(0xFF141416),
                    primary = Color(0xFFFF5B35)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0C)
                ) {
                    App()
                }
            }
        }
    }
}

private val ACCENTS = listOf(0xFFFF5B35, 0xFF30D158, 0xFFFFD60A, 0xFF0A84FF, 0xFFFF2D92)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(Prefs.load(ctx)) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }

    fun update(n: WallpaperConfig) {
        cfg = n
        Prefs.save(ctx, n)
        // Automatically sync with running wallpaper service
        JinseiWallpaperService.notifyUpdate(ctx)
    }

    val (pw, ph) = deviceSize(ctx, cfg.device)

    LaunchedEffect(cfg) {
        preview = withContext(Dispatchers.Default) {
            Renderer.render(cfg, 400, (400f * ph / pw).toInt())
        }
    }

    val error: String? = when (cfg.mode) {
        Mode.GOAL -> {
            val s = parseDate(cfg.goalStart)
            val e = parseDate(cfg.goalEnd)
            when {
                cfg.goalTitle.isBlank() -> "Goal title is required"
                s == null || e == null -> "Enter valid dates (YYYY-MM-DD)"
                !e.isAfter(s) -> "Deadline must be after start date"
                else -> null
            }
        }
        Mode.LIFE -> {
            val d = parseDate(cfg.dob)
            if (d == null || d.isAfter(LocalDate.now())) "Enter a valid date of birth" else null
        }
        Mode.YEAR -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Title
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Jinsei",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Text(
                "Minimalist wallpaper for OnePlus 13s (ColorOS 17)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
        }

        // Mode Tabs
        TabRow(
            selectedTabIndex = cfg.mode.ordinal,
            containerColor = Color(0xFF141417),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[cfg.mode.ordinal]),
                    color = Color(cfg.accent)
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Mode.values().forEach { m ->
                Tab(
                    selected = cfg.mode == m,
                    onClick = { update(cfg.copy(mode = m)) },
                    text = {
                        Text(
                            m.label,
                            fontWeight = if (cfg.mode == m) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (cfg.mode == m) Color.White else Color(0xFF8E8E93)
                        )
                    }
                )
            }
        }

        // Preview Frame (220dp height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111114))
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            preview?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Wallpaper preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF26262B), RoundedCornerShape(14.dp))
                )
            }
        }

        // Configuration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (cfg.mode) {
                    Mode.LIFE -> {
                        DateInputRow("Date of Birth", cfg.dob) { update(cfg.copy(dob = it)) }
                        CompactField(
                            label = "Lifespan (years)",
                            value = cfg.lifespan.toString(),
                            maxLen = 3
                        ) { v -> v.toIntOrNull()?.let { update(cfg.copy(lifespan = it.coerceIn(1, 120))) } }
                    }
                    Mode.YEAR -> {
                        CompactField(
                            label = "Year (leave blank for current)",
                            value = if (cfg.year == 0) "" else cfg.year.toString(),
                            placeholder = LocalDate.now().year.toString(),
                            maxLen = 4
                        ) { v -> update(cfg.copy(year = v.toIntOrNull() ?: 0)) }
                    }
                    Mode.GOAL -> {
                        OutlinedTextField(
                            value = cfg.goalTitle,
                            onValueChange = { update(cfg.copy(goalTitle = it)) },
                            label = { Text("Goal Title") },
                            placeholder = { Text("e.g. metamorphosis") },
                            singleLine = true,
                            maxLines = 1,
                            isError = cfg.goalTitle.isBlank(),
                            colors = customFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DateInputRow("Start Date", cfg.goalStart) { update(cfg.copy(goalStart = it)) }
                        DateInputRow("Deadline", cfg.goalEnd) { update(cfg.copy(goalEnd = it)) }
                    }
                }

                if (error != null) {
                    Text(
                        error,
                        color = Color(0xFFFF453A),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Appearance & Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Background Theme (Dark / Light)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("BACKGROUND THEME (PLAIN FLAT)")
                    ScrollableChips(
                        items = Theme.values().toList(),
                        selected = cfg.theme,
                        label = { it.label }
                    ) {
                        update(cfg.copy(theme = it))
                    }
                }

                // Phone Target
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("PHONE MODEL")
                    ScrollableChips(
                        items = Device.values().toList(),
                        selected = cfg.device,
                        label = { it.label }
                    ) {
                        update(cfg.copy(device = it))
                    }
                }

                // Accent Color
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("TODAY'S ACCENT DOT")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ACCENTS.forEach { a ->
                            val argb = a.toInt()
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (cfg.accent == argb) 3.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { update(cfg.copy(accent = argb)) }
                            )
                        }
                    }
                }
            }
        }

        // Daily Auto-Update Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Daily Auto-Update",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        "Advances dots seamlessly at midnight",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = cfg.autoUpdate,
                    onCheckedChange = { update(cfg.copy(autoUpdate = it)) }
                )
            }
        }

        // Primary Action: Directly Apply Wallpaper (Guarantees ColorOS Lock Screen Dark Icons)
        Button(
            enabled = error == null,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(cfg.accent),
                contentColor = Color.White
            ),
            onClick = {
                Prefs.save(ctx, cfg)
                JinseiWallpaperService.notifyUpdate(ctx)

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
                    if (cfg.autoUpdate) {
                        Scheduler.applyNow(ctx, cfg)
                    }
                    Toast.makeText(
                        ctx,
                        "Applied to Lock Screen! Status bar icons adapted to black.",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(ctx, JinseiWallpaperService::class.java)
                        )
                    }
                    try {
                        ctx.startActivity(intent)
                    } catch (e2: Exception) {
                        Toast.makeText(ctx, "Error applying: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Text(
                "Apply to Lock Screen",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }

        // Secondary Action: Live Wallpaper Preview
        Button(
            enabled = error == null,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E1E24),
                contentColor = Color.White
            ),
            onClick = {
                Prefs.save(ctx, cfg)
                JinseiWallpaperService.notifyUpdate(ctx)
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(ctx, JinseiWallpaperService::class.java)
                    )
                }
                try {
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Live wallpaper not available", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text(
                "Open Live Wallpaper Preview",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        // Explanation Tip
        Text(
            "ColorOS 17 Tip: Tap 'Apply to Lock Screen' to apply the wallpaper. ColorOS directly analyzes the background and adapts the lockscreen clock, battery icon, and status bar to dark/black.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF636366),
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = Color(0xFF8E8E93)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ScrollableChips(items: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onPick(item) },
                label = { Text(label(item)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2C2C32),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF1A1A1E),
                    labelColor = Color(0xFF8E8E93)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color(0xFF2A2A30),
                    selectedBorderColor = Color(0xFF5A5A64),
                    enabled = true,
                    selected = item == selected
                )
            )
        }
    }
}

@Composable
private fun CompactField(
    label: String,
    value: String,
    placeholder: String = "",
    maxLen: Int = 4,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.length <= maxLen && v.all { it.isDigit() }) onChange(v) },
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
        singleLine = true,
        maxLines = 1,
        colors = customFieldColors(),
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DateInputRow(label: String, iso: String, onValid: (String) -> Unit) {
    val init = parseDate(iso) ?: LocalDate.now()
    var y by remember { mutableStateOf(init.year.toString()) }
    var m by remember { mutableStateOf(init.monthValue.toString()) }
    var d by remember { mutableStateOf(init.dayOfMonth.toString()) }

    fun parsed(): LocalDate? = runCatching {
        LocalDate.of(y.toInt(), m.toInt(), d.toInt())
    }.getOrNull()

    fun emit() {
        parsed()?.let { onValid(it.toString()) }
    }

    val bad = parsed() == null

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(label.uppercase())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = y,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) { y = it; emit() } },
                label = { Text("YYYY", fontSize = 12.sp, maxLines = 1) },
                singleLine = true,
                maxLines = 1,
                isError = bad,
                colors = customFieldColors(),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.3f)
            )
            OutlinedTextField(
                value = m,
                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) { m = it; emit() } },
                label = { Text("MM", fontSize = 12.sp, maxLines = 1) },
                singleLine = true,
                maxLines = 1,
                isError = bad,
                colors = customFieldColors(),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.0f)
            )
            OutlinedTextField(
                value = d,
                onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) { d = it; emit() } },
                label = { Text("DD", fontSize = 12.sp, maxLines = 1) },
                singleLine = true,
                maxLines = 1,
                isError = bad,
                colors = customFieldColors(),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.0f)
            )
        }
    }
}

@Composable
private fun customFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF5A5A64),
    unfocusedBorderColor = Color(0xFF2A2A30),
    focusedLabelColor = Color(0xFFD4D4D8),
    unfocusedLabelColor = Color(0xFF8E8E93),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF101014),
    unfocusedContainerColor = Color(0xFF101014)
)
