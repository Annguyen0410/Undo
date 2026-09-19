package com.zhiend.regretnote.ui.paywall

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhiend.regretnote.ui.components.PanelDivider
import com.zhiend.regretnote.ui.components.PrimaryActionButton
import com.zhiend.regretnote.ui.components.QuietButton
import com.zhiend.regretnote.ui.components.SectionLabel
import com.zhiend.regretnote.ui.theme.ScreenPadding
import com.zhiend.regretnote.ui.theme.Spacing

/**
 * Full-screen paywall. Loads the configured RevenueCat offering (or falls back
 * to a demo plan set), lets the user pick a package, then purchases or restores.
 * It closes itself once premium becomes active.
 *
 * The subscribe button and the small print live in a fixed bar at the bottom:
 * on a tall phone the three plan rows used to push the primary action off the
 * first screen, which is the one thing a paywall cannot afford.
 */
@Composable
fun PaywallScreen(
    onClose: () -> Unit,
    viewModel: PaywallViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val activity = LocalContext.current as? Activity

    LaunchedEffect(isPremium) {
        if (isPremium) onClose()
    }

    BackHandler { onClose() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar: back chevron + title.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Undo Premium",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val state = uiState) {
                    PaywallUiState.Loading -> LoadingContent()
                    is PaywallUiState.Error -> ErrorContent(state.message, onRetry = viewModel::load)
                    is PaywallUiState.Ready -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = ScreenPadding),
                    ) {
                        PaywallContent(state = state, onSelect = viewModel::select)
                        Spacer(Modifier.height(Spacing.md))
                    }
                }
            }

            (uiState as? PaywallUiState.Ready)?.let { state ->
                PurchaseBar(
                    state = state,
                    purchaseState = purchaseState,
                    onPurchase = { activity?.let(viewModel::purchase) },
                    onRestore = viewModel::restore,
                )
            }
        }
    }
}

@Composable
private fun PaywallContent(
    state: PaywallUiState.Ready,
    onSelect: (String) -> Unit,
) {
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = "Reflect deeper.\nChange sooner.",
        style = MaterialTheme.typography.headlineLarge,
    )
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = "Undo Premium keeps your whole history and shows you the patterns inside it.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(Spacing.lg))
    state.plans.forEach { plan ->
        PlanCard(
            plan = plan,
            selected = plan.id == state.selectedId,
            onClick = { onSelect(plan.id) },
        )
        Spacer(Modifier.height(Spacing.sm))
    }

    if (state.isDemo) {
        Spacer(Modifier.height(Spacing.xxs))
        DemoNotice()
    }

    Spacer(Modifier.height(Spacing.lg))
    SectionLabel("What you get")
    Spacer(Modifier.height(Spacing.sm))
    FeatureList()
}

/**
 * A plan row. Selection is signalled three ways at once — border, tint and a
 * filled tick — because a thin radio circle on a dark canvas is easy to miss.
 */
@Composable
private fun PlanCard(
    plan: PaywallPlan,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val border by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        },
        animationSpec = tween(160),
        label = "plan-border",
    )
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(160),
        label = "plan-bg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .border(if (selected) 1.5.dp else 1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape,
                )
                .border(
                    width = 1.5.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (plan.bestValue) {
                    Spacer(Modifier.width(Spacing.xs))
                    BestValuePill()
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = plan.period,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = plan.price,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            plan.freeTrialDays?.let { days ->
                if (days > 0) {
                    Text(
                        text = "$days days free",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun BestValuePill() {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = "BEST VALUE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Fixed bottom bar: the subscribe action, the escape hatch and the small print. */
@Composable
private fun PurchaseBar(
    state: PaywallUiState.Ready,
    purchaseState: PurchaseState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    val selected = state.plans.firstOrNull { it.id == state.selectedId } ?: state.plans.first()
    val busy = purchaseState is PurchaseState.Purchasing
    val label = when {
        busy -> "Processing…"
        selected.freeTrialDays != null && selected.freeTrialDays > 0 ->
            "Start ${selected.freeTrialDays}-day free trial"
        else -> "Subscribe · ${selected.price}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)),
    ) {
        PanelDivider()
        Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
            Spacer(Modifier.height(Spacing.sm))
            PrimaryActionButton(
                label = label,
                onClick = onPurchase,
                busy = busy,
            )
            when (purchaseState) {
                is PurchaseState.Error -> {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = purchaseState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is PurchaseState.Success -> {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "You're Premium. Enjoy your full journal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> Unit
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                QuietButton(label = "Restore purchases", onClick = onRestore)
            }
            Text(
                text = "Payment is charged to your Google account at confirmation. " +
                    "Subscriptions renew automatically and can be cancelled anytime in Play Store settings.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

/**
 * Developer aid: shown only when no RevenueCat key is baked into the build, i.e.
 * when the plans above are the local demo set.
 */
@Composable
private fun DemoNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
            .padding(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "Demo mode — no RevenueCat key in this build, so these are placeholder plans " +
                "and tapping subscribe unlocks premium locally.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun FeatureList() {
    // CSV export is deliberately absent: your own journal leaves and re-enters the
    // app for free, whether or not you ever pay. What is paid is the reading of it
    // at scale — decades of history, the trend, the theme.
    val features = listOf(
        "Full-history regret map & insights",
        "Monthly trends over time",
        "Custom reminder time",
        "Midnight Reflection theme",
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        features.forEach { feature ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp),
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(feature, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Loading plans…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        TextButton(onClick = onRetry) { Text("Try again") }
    }
}
