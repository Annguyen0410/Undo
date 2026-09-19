package com.zhiend.regretnote.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.zhiend.regretnote.insight.CategoryStat
import com.zhiend.regretnote.ui.theme.NumericValue

/**
 * Donut chart of regret distribution by category, colored per category,
 * with the total check-in count centered in the hole.
 */
@Composable
fun CategoryDonut(stats: List<CategoryStat>, modifier: Modifier = Modifier) {
    if (stats.isEmpty()) return
    val modelProducer = remember { PieChartModelProducer() }
    val chart = rememberPieChart(
        sliceProvider = PieChart.SliceProvider.series(
            stats.map { stat ->
                PieChart.Slice(fill = Fill(stat.category.color))
            },
        ),
        innerSize = PieSize.Inner.fixed(92.dp),
        spacing = 6.dp,
    )
    LaunchedEffect(stats) {
        modelProducer.runTransaction {
            pieSeries { series(stats.map { it.count.toFloat() }) }
        }
    }
    val total = stats.sumOf { it.count }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        PieChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxSize(),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$total",
                style = NumericValue,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (total == 1) "regret" else "regrets",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
