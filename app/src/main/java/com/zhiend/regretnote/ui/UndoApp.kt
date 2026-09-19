package com.zhiend.regretnote.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhiend.regretnote.ui.checkin.CheckInScreen
import com.zhiend.regretnote.ui.checkin.CheckInViewModel
import com.zhiend.regretnote.ui.components.CosmicBackground
import com.zhiend.regretnote.ui.components.PanelDivider
import com.zhiend.regretnote.ui.insight.InsightScreen
import com.zhiend.regretnote.ui.insight.InsightViewModel
import com.zhiend.regretnote.ui.paywall.PaywallScreen
import com.zhiend.regretnote.ui.settings.SettingsScreen
import com.zhiend.regretnote.ui.settings.SettingsViewModel
import com.zhiend.regretnote.ui.theme.RegretNoteTheme
import com.zhiend.regretnote.ui.theme.Spacing
import com.zhiend.regretnote.ui.timeline.TimelineScreen
import com.zhiend.regretnote.ui.timeline.TimelineViewModel

private data class NavItem(
    val label: String,
    val icon: ImageVector,
)

private val NavItems = listOf(
    NavItem("Check-in", Icons.Outlined.EditNote),
    NavItem("Journal", Icons.AutoMirrored.Outlined.Article),
    NavItem("Insights", Icons.Outlined.Insights),
)

/**
 * Root of the UI: three tabs plus the paywall overlay and the settings screen.
 * Each tab owns its own ViewModel — no shared god-ViewModel.
 */
@Composable
fun UndoApp() {
    val checkInViewModel: CheckInViewModel = viewModel()
    val timelineViewModel: TimelineViewModel = viewModel()
    val insightViewModel: InsightViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val isPremium by settingsViewModel.isPremium.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val midnight = isPremium && settings.midnightTheme

    RegretNoteTheme(midnight = midnight) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var showPaywall by rememberSaveable { mutableStateOf(false) }
        var showSettings by rememberSaveable { mutableStateOf(false) }

        BackHandler(enabled = showPaywall) { showPaywall = false }

        // Settings and the paywall are drawn *over* the tabs, so the Check-in
        // composer stays composed beneath them — and a focused text field keeps the
        // keyboard on screen, over the overlay, swallowing every tap in its lower
        // half. Opening an overlay drops the focus instead.
        val focusManager = LocalFocusManager.current
        LaunchedEffect(showSettings, showPaywall) {
            if (showSettings || showPaywall) focusManager.clearFocus()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            CosmicBackground(modifier = Modifier.fillMaxSize())
            Scaffold(
                containerColor = Color.Transparent,
                // Without an explicit contentColor, a transparent Scaffold leaves
                // LocalContentColor unset and every Text that relies on the default
                // renders (almost) black on the near-black canvas — which is exactly
                // how the section headings came out invisible on the device.
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    AppNavBar(current = tab, onSelect = { tab = it })
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    when (tab) {
                        0 -> CheckInScreen(
                            viewModel = checkInViewModel,
                            onOpenSettings = { showSettings = true },
                            onOpenInsights = { tab = 2 },
                            // A memory is only worth resurfacing if you can go and
                            // read the rest of that night.
                            onOpenJournal = { date ->
                                timelineViewModel.jumpToDate(date)
                                tab = 1
                            },
                        )
                        1 -> TimelineScreen(
                            viewModel = timelineViewModel,
                            onGoToCheckIn = { tab = 0 },
                        )
                        else -> InsightScreen(
                            viewModel = insightViewModel,
                            onOpenPaywall = { showPaywall = true },
                        )
                    }
                }
            }
        }

        if (showPaywall) {
            PaywallScreen(onClose = { showPaywall = false })
        }

        if (showSettings) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onOpenPaywall = {
                    showSettings = false
                    showPaywall = true
                },
                onBack = { showSettings = false },
            )
        }
    }
}

/**
 * Bottom navigation. Three tabs, a hairline on top, and a soft capsule behind
 * the *icon* of the active tab only — no full-width indicator, no elevation
 * band. The bar is deliberately short: it is navigation, not content.
 */
@Composable
private fun AppNavBar(current: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        PanelDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = 6.dp),
        ) {
            NavItems.forEachIndexed { index, item ->
                val selected = current == index
                val tint by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    },
                    animationSpec = tween(180),
                    label = "nav-tint",
                )
                val capsule by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(180),
                    label = "nav-capsule",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelect(index) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 28.dp)
                            .clip(CircleShape)
                            .background(capsule),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
