package com.zhiend.regretnote.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.zhiend.regretnote.insight.MonthCount

/**
 * Column chart of check-ins per month (premium "monthly trend" feature).
 *
 * Columns are rounded and painted in the app accent, the axis line and ticks are
 * gone, and only a faint horizontal guideline remains — enough to read a height
 * difference without turning the panel into a spreadsheet.
 *
 * Month labels are the chart's own bottom axis. Laying them out in a Row below
 * the chart looked right in code and drifted on the device: the y-axis steals
 * width from the *plot* area, not from the Row, so every label after the first
 * sat progressively to the right of its column.
 */
@Composable
fun MonthlyTrendChart(trend: List<MonthCount>, modifier: Modifier = Modifier) {
    if (trend.isEmpty()) return
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(trend) {
        modelProducer.runTransaction {
            columnModel { series(trend.map { it.count.toFloat() }) }
        }
    }

    val columnColor = MaterialTheme.colorScheme.primary
    val guidelineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column(modifier = modifier) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        rememberLineComponent(
                            fill = Fill(columnColor),
                            thickness = 18.dp,
                            shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp),
                        ),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(
                    line = null,
                    tick = null,
                    guideline = rememberLineComponent(fill = Fill(guidelineColor), thickness = 1.dp),
                    label = rememberTextComponent(style = labelStyle),
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    line = null,
                    tick = null,
                    guideline = null,
                    label = rememberTextComponent(style = labelStyle),
                    itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned() },
                    valueFormatter = CartesianValueFormatter { _, value, _ ->
                        trend.getOrNull(value.roundToInt())?.label.orEmpty()
                    },
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        )
    }
}
