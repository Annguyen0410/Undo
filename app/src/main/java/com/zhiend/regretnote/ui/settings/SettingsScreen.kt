package com.zhiend.regretnote.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhiend.regretnote.BuildConfig
import com.zhiend.regretnote.purchase.RevenueCatManager
import com.zhiend.regretnote.ui.components.AppCard
import com.zhiend.regretnote.ui.components.PanelDivider
import com.zhiend.regretnote.ui.components.PrimaryActionButton
import com.zhiend.regretnote.ui.components.SectionLabel
import com.zhiend.regretnote.ui.theme.ScreenPadding
import com.zhiend.regretnote.ui.theme.Spacing
import com.zhiend.regretnote.util.Dates

/**
 * Premium & settings as a full screen. Reached by tapping Settings; shows a
 * back chevron and covers the whole app (including the bottom navigation).
 *
 * The rows used to be one flat column of identically styled cards, so nothing
 * told you where one topic ended and the next began, and the nightly reminder
 * was split across two cards. They are now grouped under section labels, and
 * the reminder time only appears when the reminder is actually on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenPaywall: () -> Unit,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val totalNotes by viewModel.totalNotes.collectAsState()
    val firstDate by viewModel.firstDate.collectAsState()

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var exportStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var importStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var debugStatus by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(uri) { ok ->
                exportStatus = if (ok) "Exported — check your Downloads." else "Export failed."
            }
        }
    }
    // Deliberately every file type, not `text/csv`: which MIME type a storage
    // provider reports for a CSV is not reliable across devices, and greying out
    // the user's own backup file is a far worse failure than showing a few extra
    // files in the picker. The parser is what validates the contents.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importCsv(uri) { outcome ->
                importStatus = when {
                    outcome == null -> "Import failed — that file couldn't be read."
                    outcome.added == 0 && outcome.duplicates == 0 && outcome.skipped == 0 ->
                        "Nothing in that file looked like a journal entry."
                    else -> buildString {
                        append(if (outcome.added == 1) "Imported 1 note" else "Imported ${outcome.added} notes")
                        if (outcome.duplicates > 0) {
                            append(" · skipped ${outcome.duplicates} already here")
                        }
                        if (outcome.skipped > 0) {
                            append(" · ignored ${outcome.skipped} unreadable")
                            append(if (outcome.skipped == 1) " row" else " rows")
                        }
                    }
                }
            }
        }
    }
    // Asked for only when the user actually wants reminders, not on first launch.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler { onBack() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTopBar(title = "Settings", onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = ScreenPadding)
                    .padding(bottom = Spacing.xl),
            ) {
                PremiumPanel(isPremium = isPremium, onOpenPaywall = onOpenPaywall)

                SettingsSection("Reminders") {
                    AppCard {
                        SettingRow(
                            icon = Icons.Outlined.Schedule,
                            title = "Nightly reminder",
                            subtitle = if (settings.reminderEnabled) {
                                "Every day at ${Dates.formatTime(settings.reminderHour, settings.reminderMinute)}"
                            } else {
                                "A gentle nudge before bed."
                            },
                            trailing = {
                                Switch(
                                    checked = settings.reminderEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) requestNotificationPermission()
                                        viewModel.updateSettings { it.copy(reminderEnabled = enabled) }
                                    },
                                )
                            },
                        )
                        if (settings.reminderEnabled) {
                            Spacer(Modifier.height(Spacing.sm))
                            PanelDivider()
                            Spacer(Modifier.height(Spacing.sm))
                            SettingRow(
                                title = "Reminder time",
                                subtitle = if (isPremium) {
                                    "Set any hour you like."
                                } else {
                                    "Custom time is a Premium feature."
                                },
                                trailing = {
                                    if (isPremium) {
                                        TrailingButton(
                                            label = Dates.formatTime(
                                                settings.reminderHour,
                                                settings.reminderMinute,
                                            ),
                                            onClick = { showTimePicker = true },
                                        )
                                    } else {
                                        LockedTag()
                                    }
                                },
                            )
                        }
                    }
                }

                SettingsSection("Appearance") {
                    AppCard {
                        SettingRow(
                            icon = Icons.Outlined.DarkMode,
                            title = "Midnight Reflection",
                            subtitle = if (isPremium) {
                                "A colder, near-monochrome palette for late nights."
                            } else {
                                "A Premium feature."
                            },
                            trailing = {
                                if (isPremium) {
                                    Switch(
                                        checked = settings.midnightTheme,
                                        onCheckedChange = { enabled ->
                                            viewModel.updateSettings { it.copy(midnightTheme = enabled) }
                                        },
                                    )
                                } else {
                                    LockedTag()
                                }
                            },
                        )
                    }
                }

                SettingsSection("Data") {
                    AppCard {
                        // The archive's size, stated plainly: this journal is built
                        // to hold decades, so it says how much it is holding.
                        SettingRow(
                            icon = Icons.Outlined.DateRange,
                            title = "Your journal",
                            subtitle = journalSubtitle(totalNotes, firstDate),
                            trailing = {},
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        PanelDivider()
                        Spacer(Modifier.height(Spacing.sm))
                        // Moving your own notes in and out is not a paid feature.
                        // The journal is deliberately kept out of cloud backup (see
                        // res/xml/data_extraction_rules.xml), so this file is the
                        // backup — gating it would mean holding a user's own words
                        // hostage, and paying to get their diary back.
                        SettingRow(
                            icon = Icons.Outlined.Download,
                            title = "Export journal",
                            subtitle = "Save every entry as a CSV file you own.",
                            trailing = {
                                TrailingButton(
                                    label = "Export",
                                    onClick = {
                                        exportStatus = null
                                        exportLauncher.launch("undo-export.csv")
                                    },
                                )
                            },
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        PanelDivider()
                        Spacer(Modifier.height(Spacing.sm))
                        SettingRow(
                            icon = Icons.Outlined.Upload,
                            title = "Import journal",
                            subtitle = "Restore a CSV export. Notes already here are skipped.",
                            trailing = {
                                TrailingButton(
                                    label = "Import",
                                    onClick = {
                                        importStatus = null
                                        importLauncher.launch(arrayOf("*/*"))
                                    },
                                )
                            },
                        )
                    }
                    exportStatus?.let { StatusLine(it) }
                    importStatus?.let { StatusLine(it) }
                }

                if (BuildConfig.DEBUG) {
                    SettingsSection("Developer") {
                        var debugPremium by rememberSaveable {
                            mutableStateOf(RevenueCatManager.isPremium.value)
                        }
                        AppCard {
                            SettingRow(
                                icon = Icons.Outlined.Lock,
                                title = "Simulate premium",
                                subtitle = "Preview premium features without a store setup.",
                                trailing = {
                                    Switch(
                                        checked = debugPremium,
                                        onCheckedChange = {
                                            debugPremium = it
                                            RevenueCatManager.setDebugPremiumOverride(it)
                                        },
                                    )
                                },
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            PanelDivider()
                            Spacer(Modifier.height(Spacing.sm))
                            // A fresh journal is empty by design, so the timeline, the donut
                            // and the monthly trend have nothing to draw. This fills in sample
                            // history for demos, screenshots and videos; it never touches
                            // existing days.
                            SettingRow(
                                icon = Icons.Outlined.DateRange,
                                title = "Sample journal",
                                subtitle = "Back-fill 90 days of entries for demos.",
                                trailing = {
                                    TrailingButton(
                                        label = "Fill",
                                        onClick = {
                                            debugStatus = null
                                            viewModel.seedSampleJournal { added ->
                                                debugStatus = if (added == 0) {
                                                    "Sample journal already complete."
                                                } else {
                                                    "Added $added sample entries."
                                                }
                                            }
                                        },
                                    )
                                },
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            PanelDivider()
                            Spacer(Modifier.height(Spacing.sm))
                            // Twenty years and ~8,700 notes, to prove the claim that
                            // nothing loads the journal into memory: paging, search,
                            // insights and the archive memory all have to stay instant
                            // on a journal this size.
                            SettingRow(
                                icon = Icons.Outlined.History,
                                title = "Twenty years",
                                subtitle = "Seed ~20 years of history to stress-test lazy loading.",
                                trailing = {
                                    TrailingButton(
                                        label = "Seed",
                                        onClick = {
                                            debugStatus = "Seeding 20 years…"
                                            viewModel.seedDecades(20) { added ->
                                                debugStatus = if (added == 0) {
                                                    "Two decades already in place."
                                                } else {
                                                    "Added $added notes across 20 years."
                                                }
                                            }
                                        },
                                    )
                                },
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            PanelDivider()
                            Spacer(Modifier.height(Spacing.sm))
                            SettingRow(
                                icon = Icons.Outlined.DeleteOutline,
                                title = "Clear journal",
                                subtitle = "Deletes every entry, including your own.",
                                trailing = {
                                    TrailingButton(
                                        label = "Clear",
                                        destructive = true,
                                        onClick = {
                                            debugStatus = null
                                            viewModel.clearJournal { removed ->
                                                debugStatus = "Deleted $removed entries."
                                            }
                                        },
                                    )
                                },
                            )
                        }
                        debugStatus?.let { StatusLine(it) }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSettings {
                            it.copy(
                                reminderHour = state.hour,
                                reminderMinute = state.minute,
                            )
                        }
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }
}

// --- Building blocks ---------------------------------------------------------

@Composable
private fun ScreenTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** Section label + the cards that belong to it. */
@Composable
private fun SettingsSection(label: String, content: @Composable () -> Unit) {
    Column {
        Spacer(Modifier.height(Spacing.lg))
        SectionLabel(label)
        Spacer(Modifier.height(Spacing.xs))
        content()
    }
}

/** "4,311 notes since Aug 2006" — the archive describing itself. */
private fun journalSubtitle(totalNotes: Int, firstDate: String?): String = when {
    totalNotes == 0 -> "Empty so far — your first note is tonight's."
    firstDate == null -> "$totalNotes notes"
    totalNotes == 1 -> "1 note, written ${Dates.formatLong(firstDate)}"
    else -> "$totalNotes notes since ${Dates.formatLong(firstDate)}"
}

@Composable
private fun StatusLine(text: String) {
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * The premium banner: a tinted panel rather than a card, so it reads as a
 * status block instead of just another setting.
 */
@Composable
private fun PremiumPanel(isPremium: Boolean, onOpenPaywall: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                MaterialTheme.shapes.large,
            )
            .padding(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.size(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) "Undo Premium is active" else "Undo Premium",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (isPremium) {
                        "Thanks for supporting the app."
                    } else {
                        "Full history, trends and Midnight Reflection."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isPremium) {
                Spacer(Modifier.size(Spacing.xs))
                ActiveBadge()
            }
        }

        if (!isPremium) {
            Spacer(Modifier.height(Spacing.md))
            PrimaryActionButton(
                label = "See plans",
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                onClick = onOpenPaywall,
            )
        }
    }
}

@Composable
private fun ActiveBadge() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = "Active",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f))
                    .padding(9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(Spacing.sm))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(Spacing.sm))
        trailing()
    }
}

/** Compact pill action for a row's trailing slot. */
@Composable
private fun TrailingButton(
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val container = if (destructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val content: Color = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

@Composable
private fun LockedTag() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = "Locked",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = "Premium",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
