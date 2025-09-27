package com.example.focusflight

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusflight.data.Achievement
import com.example.focusflight.data.FlightLogEntry
import com.example.focusflight.data.FlightRoute
import com.example.focusflight.data.SeatClass
import com.example.focusflight.ui.theme.FocusFlightTheme
import com.example.focusflight.util.AmbientSoundPlayer
import com.example.focusflight.util.FocusModeManager
import com.example.focusflight.util.HapticsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            FocusFlightTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FocusFlightApp()
                }
            }
        }
    }
}

@Composable
fun FocusFlightApp(viewModel: FocusViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val ambientPlayer = remember { AmbientSoundPlayer() }
    val haptics = remember { HapticsHelper(context) }
    var previousPhase by remember { mutableStateOf(uiState.phase) }

    LaunchedEffect(Unit) {
        viewModel.updateDndAccess(FocusModeManager.hasDndAccess(context))
    }

    LaunchedEffect(uiState.phase, uiState.soundEnabled) {
        when (uiState.phase) {
            FocusPhase.FLYING -> {
                if (uiState.soundEnabled) ambientPlayer.start() else ambientPlayer.stop()
                playSeatbeltTone()
                if (uiState.hapticsEnabled) haptics.pulseStrong()
                FocusModeManager.setFocusMode(context, true)
            }
            else -> ambientPlayer.stop()
        }
        if (previousPhase == FocusPhase.FLYING && uiState.phase != FocusPhase.FLYING) {
            FocusModeManager.setFocusMode(context, false)
            if (uiState.hapticsEnabled) haptics.pulseSoft()
        }
        previousPhase = uiState.phase
    }

    DisposableEffect(Unit) {
        onDispose {
            ambientPlayer.stop()
            FocusModeManager.setFocusMode(context, false)
        }
    }

    KeepScreenOnEffect(uiState.phase == FocusPhase.FLYING)

    Crossfade(targetState = uiState.phase, label = "phase") { phase ->
        when (phase) {
            FocusPhase.PLANNING -> PlanningScreen(
                uiState = uiState,
                onRouteSelected = viewModel::selectRoute,
                onCategorySelected = viewModel::selectCategory,
                onStartFlight = viewModel::startSession,
                onToggleSound = viewModel::toggleSound,
                onToggleHaptics = viewModel::toggleHaptics,
                onRequestDnd = { FocusModeManager.requestDndAccess(context) },
                onRefreshDnd = { viewModel.updateDndAccess(FocusModeManager.hasDndAccess(context)) }
            )
            FocusPhase.FLYING -> InFlightScreen(
                uiState = uiState,
                onCancel = viewModel::cancelSession
            )
            FocusPhase.LANDED -> LandingScreen(
                uiState = uiState,
                onContinue = viewModel::acknowledgeLanding
            )
        }
    }
}

@Composable
fun PlanningScreen(
    uiState: FocusUiState,
    onRouteSelected: (FlightRoute) -> Unit,
    onCategorySelected: (SeatClass) -> Unit,
    onStartFlight: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onRequestDnd: () -> Unit,
    onRefreshDnd: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Focus Flight", style = MaterialTheme.typography.titleLarge)
                        Text("Plan your next mission", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    FilledIconButton(onClick = onRefreshDnd) {
                        Icon(imageVector = Icons.Default.AirplanemodeActive, contentDescription = "Refresh focus mode")
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                TextButton(
                    onClick = onStartFlight,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.onPrimary)
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Start Flight",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 120.dp)
        ) {
            item {
                PlanningHeroSection(uiState = uiState)
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Choose your route",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(uiState.availableRoutes) { route ->
                        RouteCard(
                            route = route,
                            selected = route.id == uiState.selectedRoute.id,
                            onClick = { onRouteSelected(route) }
                        )
                    }
                }
            }
            item {
                CategorySelection(
                    selected = uiState.selectedCategory,
                    onCategorySelected = onCategorySelected
                )
            }
            item {
                FlightSettingsCard(
                    soundEnabled = uiState.soundEnabled,
                    hapticsEnabled = uiState.hapticsEnabled,
                    dndGranted = uiState.dndAccessGranted,
                    onToggleSound = onToggleSound,
                    onToggleHaptics = onToggleHaptics,
                    onRequestDnd = onRequestDnd
                )
            }
            item {
                AchievementRow(achievements = uiState.achievements)
            }
            if (uiState.history.isNotEmpty()) {
                item {
                    HistoryList(uiState.history)
                }
            }
            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
fun PlanningHeroSection(uiState: FocusUiState) {
    val route = uiState.selectedRoute
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Color(0x660C1F33))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.world_map),
                    contentDescription = "World map",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                MapOverlay(
                    routes = uiState.availableRoutes,
                    selectedRoute = route,
                    completedRouteIds = uiState.history.filter { it.completed }.map { it.routeId }.toSet()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${route.origin} ✈ ${route.destination}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Flight time ${route.durationMinutes} min • ${route.distanceMiles} miles",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Total miles focused: ${uiState.totalMiles}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun MapOverlay(
    routes: List<FlightRoute>,
    selectedRoute: FlightRoute,
    completedRouteIds: Set<String>
) {
    Canvas(modifier = Modifier
        .matchParentSize()
        .padding(12.dp)) {
        val width = size.width
        val height = size.height
        routes.forEach { route ->
            val start = latLonToPoint(route.originLatitude, route.originLongitude, width, height)
            val end = latLonToPoint(route.destinationLatitude, route.destinationLongitude, width, height)
            val color = when {
                route.id == selectedRoute.id -> Color(0xFF4DA3FF)
                route.id in completedRouteIds -> Color(0xFF8FD14F)
                else -> Color.White.copy(alpha = 0.35f)
            }
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = if (route.id == selectedRoute.id) 6f else 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f))
            )
            drawCircle(color, radius = if (route.id == selectedRoute.id) 10f else 6f, center = start)
            drawCircle(color, radius = if (route.id == selectedRoute.id) 10f else 6f, center = end)
        }
    }
}

private fun latLonToPoint(lat: Float, lon: Float, width: Float, height: Float): Offset {
    val x = (lon + 180f) / 360f * width
    val y = (90f - lat) / 180f * height
    return Offset(x, y)
}

@Composable
fun RouteCard(route: FlightRoute, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), label = "route"
    )
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(width = 180.dp, height = 160.dp)
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = background)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = route.origin, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Icon(imageVector = Icons.Default.AirplanemodeActive, contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = route.destination, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${route.durationMinutes} min", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CategorySelection(selected: SeatClass, onCategorySelected: (SeatClass) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(text = "Seat class", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(SeatClass.values()) { seat ->
                val isSelected = seat == selected
                AssistChip(
                    onClick = { onCategorySelected(seat) },
                    label = { Text(seat.displayName) },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = selected.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun FlightSettingsCard(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    dndGranted: Boolean,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onRequestDnd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingRow(
                title = "Cabin ambience",
                subtitle = "Loop soothing cabin noise during focus",
                checked = soundEnabled,
                onToggle = onToggleSound
            )
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            SettingRow(
                title = "Haptic moments",
                subtitle = "Feel gentle vibrations on take-off and landing",
                checked = hapticsEnabled,
                onToggle = onToggleHaptics
            )
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Focus Mode", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (dndGranted) "Do Not Disturb ready" else "Grant permission to silence alerts",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = onRequestDnd) {
                    Text(if (dndGranted) "Settings" else "Grant")
                }
            }
        }
    }
}

@Composable
fun SettingRow(title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
fun AchievementRow(achievements: List<Achievement>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Achievements", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(achievements) { achievement ->
                AchievementBadge(achievement)
            }
        }
    }
}

@Composable
fun AchievementBadge(achievement: Achievement) {
    val color = if (achievement.achieved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(colors = CardDefaults.cardColors(containerColor = color)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.height(4.dp))
                Text(achievement.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = achievement.description,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 160.dp)
        )
    }
}

@Composable
fun HistoryList(history: List<FlightLogEntry>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.History, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Flight log", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(12.dp))
        history.take(5).forEach { entry ->
            HistoryCard(entry)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun HistoryCard(entry: FlightLogEntry) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }
    val statusColor = if (entry.completed) Color(0xFF8FD14F) else Color(0xFFF07167)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.routeLabel, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${entry.durationMinutes} min • ${entry.category.displayName}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(formatter.format(Date(entry.timestamp)), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, shape = MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (entry.completed) "Landed" else "Diverted", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.weight(1f))
                Text("${entry.milesEarned} miles", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun InFlightScreen(uiState: FocusUiState, onCancel: () -> Unit) {
    val route = uiState.selectedRoute
    val totalSeconds = route.durationMinutes * 60
    val progress = 1f - (uiState.remainingSeconds / totalSeconds.toFloat())
    BackHandler(enabled = true) { }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("En route to ${route.destination}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatTime(uiState.remainingSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Focus mode engaged", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        FlightProgressMap(route = route, progress = progress)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = onCancel) {
                Text("Emergency land")
            }
        }
    }
}

@Composable
fun FlightProgressMap(route: FlightRoute, progress: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.world_map),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Canvas(modifier = Modifier.matchParentSize()) {
                val start = latLonToPoint(route.originLatitude, route.originLongitude, size.width, size.height)
                val end = latLonToPoint(route.destinationLatitude, route.destinationLongitude, size.width, size.height)
                drawLine(
                    color = Color(0x55FFFFFF),
                    start = start,
                    end = end,
                    strokeWidth = 6f
                )
                val planeX = start.x + (end.x - start.x) * progress
                val planeY = start.y + (end.y - start.y) * progress
                drawCircle(color = Color.White, radius = 12f, center = Offset(planeX, planeY))
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text("${route.origin} → ${route.destination}", style = MaterialTheme.typography.titleMedium)
                Text("${(progress * route.distanceMiles).toInt()} / ${route.distanceMiles} miles", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun LandingScreen(uiState: FocusUiState, onContinue: () -> Unit) {
    val entry = uiState.lastFlight
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Landed!", style = MaterialTheme.typography.displaySmall)
        Spacer(modifier = Modifier.height(16.dp))
        entry?.let {
            TicketCard(entry = it, successful = uiState.lastFlightCompleted)
            Spacer(modifier = Modifier.height(16.dp))
        }
        TextButton(onClick = onContinue) {
            Text("Plan next flight")
        }
    }
}

@Composable
fun TicketCard(entry: FlightLogEntry, successful: Boolean) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Focus Ticket", style = MaterialTheme.typography.titleMedium)
                Icon(imageVector = Icons.Default.AirplanemodeActive, contentDescription = null)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(entry.routeLabel, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Seat: ${entry.category.displayName}", style = MaterialTheme.typography.bodyMedium)
            Text("Duration: ${entry.durationMinutes} minutes", style = MaterialTheme.typography.bodyMedium)
            Text("Miles earned: ${entry.milesEarned}", style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${if (successful) "On-time arrival" else "Diverted"}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatter.format(Date(entry.timestamp)), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun KeepScreenOnEffect(keepScreenOn: Boolean) {
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        val window = view.context.findActivity()?.window
        if (keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
}

private fun playSeatbeltTone() {
    runCatching {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60).startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
    }
}

private fun android.content.Context.findActivity(): androidx.activity.ComponentActivity? {
    return when (this) {
        is androidx.activity.ComponentActivity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
