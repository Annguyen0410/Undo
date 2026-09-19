package com.zhiend.regretnote.ui.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhiend.regretnote.insight.InsightReport
import com.zhiend.regretnote.insight.MonthCount
import com.zhiend.regretnote.ui.components.AppCard
import com.zhiend.regretnote.ui.components.CategoryDonut
import com.zhiend.regretnote.ui.components.PanelDivider
import com.zhiend.regretnote.ui.components.PrimaryActionButton
import com.zhiend.regretnote.ui.components.SectionLabel
import com.zhiend.regretnote.ui.components.SegmentedControl
import com.zhiend.regretnote.ui.components.MonthlyTrendChart
import com.zhiend.regretnote.ui.theme.NumericValue
import com.zhiend.regretnote.ui.theme.ScreenPadding
import com.zhiend.regretnote.ui.theme.Spacing
import com.zhiend.regretnote.util.Dates
import java.util.Calendar

private val RangeTabs = listOf("This week", "All time", "Trend")

/**
 * Insight tab: the "so what" screen — where your regrets cluster.
 *
 * The header, the three stat tiles, the streak card and the empty-state card
 * used to be four separate surfaces stacked on top of each other, and the
 * streak number appeared twice. Stats and streak are now one panel, "top focus"
 * became "this week" (a number that never overflows its tile), and the range
 * switch is the same segmented control used elsewhere in the app.
 */
@Composable
fun InsightScreen(
    viewModel: InsightViewModel,
    onOpenPaywall: () -> Unit,
) {
    val totalNotes by viewModel.totalNotes.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val thisWeek by viewModel.thisWeek.collectAsState()
    val allTime by viewModel.allTime.collectAsState()
    val writtenDates by viewModel.writtenDates.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val today = remember { Dates.today() }
    val last7 = remember(writtenDates) {
        (6 downTo 0).map { offset -> writtenDates.contains(Dates.addDays(today, -offset)) }
    }
    val dayLetters = remember(today) {
        (6 downTo 0).map { offset ->
            dayLetter(Dates.parse(Dates.addDays(today, -offset)).get(Calendar.DAY_OF_WEEK))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding),
    ) {
        Spacer(Modifier.height(Spacing.sm))
        SectionLabel("Insights")
        Spacer(Modifier.height(Spacing.xxs))
        Text("Your reflections", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            "Patterns, not punishment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))

        ProgressPanel(
            totalEntries = totalNotes,
            streak = streak,
            thisWeekCount = thisWeek.totalEntries,
            last7 = last7,
            dayLetters = dayLetters,
        )

        Spacer(Modifier.height(Spacing.lg))

        if (totalNotes == 0) {
            BlankSlate()
            Spacer(Modifier.height(Spacing.xl))
            return@Column
        }

        if (isPremium) {
            SegmentedControl(
                options = RangeTabs,
                selectedIndex = tab,
                onSelect = { tab = it },
            )
            Spacer(Modifier.height(Spacing.md))
            when (tab) {
                0 -> ReportSection(thisWeek)
                1 -> ReportSection(allTime)
                else -> TrendSection(allTime.monthlyTrend)
            }
        } else {
            ReportSection(thisWeek)
            Spacer(Modifier.height(Spacing.md))
            UpsellPanel(onOpenPaywall)
        }

        Spacer(Modifier.height(Spacing.xl))
    }
}

/** Numbers and the week strip in one panel — the "how am I doing?" block. */
@Composable
private fun ProgressPanel(
    totalEntries: Int,
    streak: Int,
    thisWeekCount: Int,
    last7: List<Boolean>,
    dayLetters: List<Char>,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stat(
                value = "$totalEntries",
                label = "Check-ins",
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            StatDivider()
            Stat(
                value = "$streak",
                label = "Day streak",
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            StatDivider()
            Stat(
                value = "$thisWeekCount",
                label = "This week",
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.md))
        PanelDivider()
        Spacer(Modifier.height(Spacing.md))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (streak == 1) "1-day streak" else "$streak-day streak",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = if (streak == 0) {
                        "Check in tonight to start one."
                    } else {
                        "Honest reflection, day after day."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.md))
            WeekStrip(last7 = last7, dayLetters = dayLetters)
        }
    }
}

@Composable
private fun Stat(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = NumericValue,
            color = accent,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    )
}

/** Seven dots — filled when the night is written down, ringed for tonight. */
@Composable
private fun WeekStrip(last7: List<Boolean>, dayLetters: List<Char>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        last7.forEachIndexed { index, checked ->
            val isToday = index == last7.lastIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(
                            if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape,
                        )
                        .border(
                            width = 1.dp,
                            color = if (isToday && !checked) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                            },
                            shape = CircleShape,
                        ),
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = dayLetters[index].toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

private fun dayLetter(dayOfWeek: Int): Char = when (dayOfWeek) {
    Calendar.SUNDAY -> 'S'
    Calendar.MONDAY -> 'M'
    Calendar.TUESDAY -> 'T'
    Calendar.WEDNESDAY -> 'W'
    Calendar.THURSDAY -> 'T'
    Calendar.FRIDAY -> 'F'
    else -> 'S'
}

@Composable
private fun ReportSection(report: InsightReport) {
    if (report.totalEntries == 0) {
        AppCard {
            Text(
                text = "No check-ins in this range yet. Keep showing up — patterns need a few days.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    AppCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(report.range.label)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${report.totalEntries} ${if (report.totalEntries == 1) "check-in" else "check-ins"} · " +
                    "${report.daysCovered} ${if (report.daysCovered == 1) "day" else "days"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(208.dp),
            contentAlignment = Alignment.Center,
        ) {
            CategoryDonut(
                stats = report.categoryStats,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(Spacing.md))
        CategoryLegend(report)

        Spacer(Modifier.height(Spacing.md))
        InsightSentence(report)
    }
}

/** Legend as two columns — eight rows in a single column made the card very tall. */
@Composable
private fun CategoryLegend(report: InsightReport) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        report.categoryStats.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                pair.forEach { stat ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(stat.category.color, CircleShape),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = stat.category.label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        Text(
                            text = "${stat.count} · ${stat.percentage}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InsightSentence(report: InsightReport) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f))
            .padding(Spacing.md),
    ) {
        Text(
            text = report.sentence,
            style = MaterialTheme.typography.bodyLarge,
        )
        report.followUp?.let { followUp ->
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = followUp,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TrendSection(trend: List<MonthCount>) {
    AppCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Outlined.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                "Monthly trend",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = "Check-ins per month, last 6 months.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        MonthlyTrendChart(
            trend = trend,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Premium teaser: tinted panel rather than another neutral card. */
@Composable
private fun UpsellPanel(onOpenPaywall: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                MaterialTheme.shapes.large,
            )
            .padding(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text("See the full picture", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Unlock full history, monthly trends, custom reminder time and the Midnight Reflection theme.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        PrimaryActionButton(label = "Unlock Premium", onClick = onOpenPaywall)
    }
}

@Composable
private fun BlankSlate() {
    AppCard {
        Text(
            text = "Your map is blank — for now.",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "After a couple of weeks of nightly check-ins, this screen will show you where your regrets cluster — and what to do about it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
