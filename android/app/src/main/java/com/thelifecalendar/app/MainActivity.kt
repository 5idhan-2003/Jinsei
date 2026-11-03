package com.thelifecalendar.app

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) { App() }
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
                s == null || e == null -> "Enter valid dates"
                !e.isAfter(s) -> "Deadline must be after the start date"
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Jinsei", style = MaterialTheme.typography.headlineMedium)

        TabRow(selectedTabIndex = cfg.mode.ordinal) {
            Mode.values().forEach { m ->
                Tab(
                    selected = cfg.mode == m,
                    onClick = { update(cfg.copy(mode = m)) },
                    text = { Text(m.label) }
                )
            }
        }

        preview?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Wallpaper preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        when (cfg.mode) {
            Mode.LIFE -> {
                DateRow("Date of birth", cfg.dob) { update(cfg.copy(dob = it)) }
                NumField(
                    "Lifespan (years)",
                    cfg.lifespan.toString(),
                    Modifier.fillMaxWidth(),
                    maxLen = 3
                ) { v -> v.toIntOrNull()?.let { update(cfg.copy(lifespan = it.coerceIn(1, 120))) } }
            }
            Mode.YEAR -> {
                NumField(
                    "Year (leave empty = current year)",
                    if (cfg.year == 0) "" else cfg.year.toString(),
                    Modifier.fillMaxWidth(),
                    maxLen = 4
                ) { v -> update(cfg.copy(year = v.toIntOrNull() ?: 0)) }
            }
            Mode.GOAL -> {
                OutlinedTextField(
                    value = cfg.goalTitle,
                    onValueChange = { update(cfg.copy(goalTitle = it)) },
                    label = { Text("Goal") },
                    placeholder = { Text("e.g. New York City Marathon") },
                    singleLine = true,
                    isError = cfg.goalTitle.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                DateRow("Start date", cfg.goalStart) { update(cfg.copy(goalStart = it)) }
                DateRow("Deadline", cfg.goalEnd) { update(cfg.copy(goalEnd = it)) }
            }
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Label("Phone")
        ChipRow(Device.values().toList(), cfg.device, { it.label }) { update(cfg.copy(device = it)) }

        Label("Apply to")
        ChipRow(Target.values().toList(), cfg.target, { it.label }) { update(cfg.copy(target = it)) }

        Label("Accent color")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ACCENTS.forEach { a ->
                val argb = a.toInt()
                Box(
                    modifier = Modifier
                        .size(36.dp)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Update automatically every day")
            Switch(checked = cfg.autoUpdate, onCheckedChange = { update(cfg.copy(autoUpdate = it)) })
        }

        Button(
            enabled = error == null,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                Scheduler.applyNow(ctx, cfg)
                Toast.makeText(
                    ctx,
                    if (cfg.autoUpdate) "Wallpaper set. It will refresh every midnight." else "Wallpaper set.",
                    Toast.LENGTH_LONG
                ).show()
            }
        ) { Text(if (cfg.autoUpdate) "Set wallpaper & start daily updates" else "Set wallpaper now") }

        Text(
            "Tip: allow this app to run unrestricted in battery settings so the midnight update is never skipped.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChipRow(items: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onPick(item) },
                label = { Text(label(item)) }
            )
        }
    }
}

@Composable
private fun NumField(
    label: String,
    value: String,
    modifier: Modifier,
    maxLen: Int = 4,
    isError: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.length <= maxLen && v.all { it.isDigit() }) onChange(v) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

/** Year / Month / Day inputs. Only valid dates are saved. */
@Composable
private fun DateRow(label: String, iso: String, onValid: (String) -> Unit) {
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
        Label(label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Year", y, Modifier.weight(1.4f), 4, bad) { y = it; emit() }
            NumField("Month", m, Modifier.weight(1f), 2, bad) { m = it; emit() }
            NumField("Day", d, Modifier.weight(1f), 2, bad) { d = it; emit() }
        }
    }
}

