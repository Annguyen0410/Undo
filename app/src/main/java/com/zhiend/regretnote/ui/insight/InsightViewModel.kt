package com.zhiend.regretnote.ui.insight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhiend.regretnote.UndoApplication
import com.zhiend.regretnote.insight.InsightEngine
import com.zhiend.regretnote.insight.InsightRange
import com.zhiend.regretnote.insight.InsightReport
import com.zhiend.regretnote.purchase.RevenueCatManager
import com.zhiend.regretnote.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Insights screen state.
 *
 * Nothing here holds entries. The counts come from `GROUP BY` queries, the
 * streak comes from the (cheap) list of distinct dates, and the week strip from
 * the same list — so this screen costs the same whether the journal holds three
 * notes or fifty thousand.
 */
class InsightViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as UndoApplication).container.repository

    private val today = Dates.today()
    private val weekStart = InsightEngine.weekStart(today)

    private val monthlyCounts = repository.observeMonthlyCounts()

    /** Dates that hold at least one note — the week strip and the streak. */
    val writtenDates: StateFlow<Set<String>> = repository.observeDates()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val totalNotes: StateFlow<Int> = repository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val streak: StateFlow<Int> = repository.observeDates()
        .map { InsightEngine.streak(it, today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val thisWeek: StateFlow<InsightReport> = combine(
        repository.observeCategoryCounts(weekStart, today),
        repository.observeCountInRange(weekStart, today),
        monthlyCounts,
    ) { counts, total, months ->
        InsightEngine.report(
            range = InsightRange.THIS_WEEK,
            categoryStats = InsightEngine.categoryStats(counts),
            totalEntries = total,
            daysCovered = Dates.daysBetween(weekStart, today) + 1,
            monthlyTrend = InsightEngine.monthTrend(months, today),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyReport(InsightRange.THIS_WEEK))

    val allTime: StateFlow<InsightReport> = combine(
        repository.observeCategoryCounts(Dates.MIN_ISO, today),
        repository.observeCount(),
        repository.observeFirstDate(),
        monthlyCounts,
    ) { counts, total, first, months ->
        InsightEngine.report(
            range = InsightRange.ALL_TIME,
            categoryStats = InsightEngine.categoryStats(counts),
            totalEntries = total,
            daysCovered = first?.let { Dates.daysBetween(it, today) + 1 } ?: 0,
            monthlyTrend = InsightEngine.monthTrend(months, today),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyReport(InsightRange.ALL_TIME))

    val isPremium: StateFlow<Boolean> = RevenueCatManager.isPremium

    private fun emptyReport(range: InsightRange) = InsightEngine.report(
        range = range,
        categoryStats = emptyList(),
        totalEntries = 0,
        daysCovered = 0,
    )
}
