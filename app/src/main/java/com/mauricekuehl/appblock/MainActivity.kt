package com.mauricekuehl.appblock

import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mauricekuehl.appblock.data.BlockSchedule
import com.mauricekuehl.appblock.data.InstalledApp
import com.mauricekuehl.appblock.data.InstalledAppsRepository
import com.mauricekuehl.appblock.data.ScheduleEvaluator
import com.mauricekuehl.appblock.data.ScheduleRepository
import com.mauricekuehl.appblock.service.AppBlockAccessibilityService
import com.mauricekuehl.appblock.ui.theme.AppBlockTheme
import java.time.DayOfWeek

class MainActivity : ComponentActivity() {
    private val accessibilityEnabled = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scheduleRepository = ScheduleRepository(this)

        setContent {
            var schedule by remember { mutableStateOf(scheduleRepository.load()) }

            AppBlockTheme {
                AppBlockScreen(
                    schedule = schedule,
                    accessibilityEnabled = accessibilityEnabled.value,
                    onScheduleChange = { updated ->
                        schedule = updated
                        scheduleRepository.save(updated)
                    },
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accessibilityEnabled.value = isAccessibilityServiceEnabled(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AppBlockScreen(
    schedule: BlockSchedule,
    accessibilityEnabled: Boolean,
    onScheduleChange: (BlockSchedule) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    val context = LocalContext.current
    val appsRepository = remember { InstalledAppsRepository(context) }
    var showAppPicker by remember { mutableStateOf(false) }
    val selectedApps = remember(schedule.blockedPackages) {
        schedule.blockedPackages
            .map { InstalledApp(appsRepository.labelFor(it), it) }
            .sortedBy { it.label.lowercase() }
    }
    val currentlyBlocking = ScheduleEvaluator.isActive(schedule)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("App Block", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (currentlyBlocking) "Block window is active" else "Protect your focus time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (schedule.enabled) "On" else "Off") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                StatusCard(
                    schedule = schedule,
                    accessibilityEnabled = accessibilityEnabled,
                    onEnabledChange = { onScheduleChange(schedule.copy(enabled = it)) },
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                )
            }

            item {
                ScheduleCard(schedule = schedule, onScheduleChange = onScheduleChange)
            }

            item {
                BlockedAppsCard(
                    selectedApps = selectedApps,
                    onAddApps = { showAppPicker = true },
                    onRemoveApp = { packageName ->
                        onScheduleChange(schedule.copy(blockedPackages = schedule.blockedPackages - packageName))
                    },
                )
            }

            item {
                Text(
                    text = "Changes are saved automatically. The block screen appears only inside the selected time window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            initiallySelected = schedule.blockedPackages,
            onDismiss = { showAppPicker = false },
            onConfirm = { packages ->
                onScheduleChange(schedule.copy(blockedPackages = packages))
                showAppPicker = false
            },
        )
    }
}

@Composable
private fun StatusCard(
    schedule: BlockSchedule,
    accessibilityEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (accessibilityEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (accessibilityEnabled) "Blocking is ready" else "One permission needed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (accessibilityEnabled) {
                            "App Block can detect and stop selected apps."
                        } else {
                            "Enable App Block in Accessibility settings so it can detect app launches."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(checked = schedule.enabled, onCheckedChange = onEnabledChange)
            }
            if (!accessibilityEnabled) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onOpenAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Accessibility settings")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleCard(
    schedule: BlockSchedule,
    onScheduleChange: (BlockSchedule) -> Unit,
) {
    val context = LocalContext.current

    OutlinedCard {
        Column(Modifier.padding(20.dp)) {
            SectionTitle(Icons.Rounded.Schedule, "Block schedule")
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeButton(
                    label = "From",
                    minute = schedule.startMinute,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showTimePicker(context, schedule.startMinute) {
                            onScheduleChange(schedule.copy(startMinute = it))
                        }
                    },
                )
                TimeButton(
                    label = "Until",
                    minute = schedule.endMinute,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showTimePicker(context, schedule.endMinute) {
                            onScheduleChange(schedule.copy(endMinute = it))
                        }
                    },
                )
            }
            Spacer(Modifier.height(18.dp))
            Text("Days", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                DayOfWeek.entries.forEach { day ->
                    FilterChip(
                        selected = day in schedule.days,
                        onClick = {
                            val days = if (day in schedule.days) schedule.days - day else schedule.days + day
                            onScheduleChange(schedule.copy(days = days))
                        },
                        label = { Text(day.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
            if (schedule.startMinute > schedule.endMinute) {
                Text(
                    text = "This window ends the following day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TimeButton(label: String, minute: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(3.dp))
            Text(formatTime(minute), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BlockedAppsCard(
    selectedApps: List<InstalledApp>,
    onAddApps: () -> Unit,
    onRemoveApp: (String) -> Unit,
) {
    OutlinedCard {
        Column(Modifier.padding(vertical = 20.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(Icons.Rounded.Apps, "Blocked apps", Modifier.weight(1f))
                FilledTonalButton(onClick = onAddApps) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Choose")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (selectedApps.isEmpty()) {
                Text(
                    text = "No apps selected yet. Choose the apps that should stay out of reach.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            } else {
                selectedApps.forEachIndexed { index, app ->
                    if (index > 0) HorizontalDivider(Modifier.padding(start = 72.dp))
                    AppRow(app = app, selected = true, onClick = { onRemoveApp(app.packageName) })
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(initialColor(app.label)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            IconButton(onClick = onClick) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove ${app.label}")
            }
        } else {
            Checkbox(checked = false, onCheckedChange = { onClick() })
        }
    }
}

@Composable
private fun AppPickerDialog(
    initiallySelected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val context = LocalContext.current
    val apps = remember { InstalledAppsRepository(context).loadLaunchableApps() }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initiallySelected) }
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps else apps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose apps") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search installed apps") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {}),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${selected.size} selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        val isSelected = app.packageName in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (isSelected) selected - app.packageName else selected + app.packageName
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(initialColor(app.label)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    app.label.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = app.label,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    selected = if (isSelected) selected - app.packageName else selected + app.packageName
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) {
                Text("Save selection")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

private fun showTimePicker(context: Context, initialMinute: Int, onSelected: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onSelected(hour * 60 + minute) },
        initialMinute / 60,
        initialMinute % 60,
        true,
    ).show()
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, AppBlockAccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()

    return enabledServices.split(':')
        .mapNotNull(ComponentName::unflattenFromString)
        .any { it == expected }
}

private fun initialColor(label: String): Color {
    val colors = listOf(
        Color(0xFF386A50),
        Color(0xFF53634C),
        Color(0xFF5E5A7D),
        Color(0xFF7D5260),
        Color(0xFF44627A),
    )
    return colors[(label.hashCode() and Int.MAX_VALUE) % colors.size]
}
