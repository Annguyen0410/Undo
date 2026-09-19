package com.zhiend.regretnote.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.zhiend.regretnote.data.Category
import com.zhiend.regretnote.data.Entry
import com.zhiend.regretnote.ui.components.AppCard
import com.zhiend.regretnote.ui.components.CategoryTag
import com.zhiend.regretnote.ui.components.EmptyState
import com.zhiend.regretnote.ui.components.IntensityTag
import com.zhiend.regretnote.ui.components.QuietButton
import com.zhiend.regretnote.ui.components.SectionLabel
import com.zhiend.regretnote.ui.components.SelectableChip
import com.zhiend.regretnote.ui.theme.ScreenPadding
import com.zhiend.regretnote.ui.theme.Spacing
import com.zhiend.regretnote.util.Dates
import com.zhiend.regretnote.util.groupedByDay

/** How close to the end of the loaded window triggers the next page. */
private const val PREFETCH_DISTANCE = 6

/**
 * Journal tab: the whole record, searched and paged.
 *
 * Search and the category filter are pinned above the list — with years of notes
 * the last thing you want is to scroll back to the top to change the filter. The
 * list itself loads 50 notes at a time and groups them by day, and a day header
 * says how many notes that day holds. Groups are built from the *accumulated*
 * list, so a day that straddles a page boundary simply gains its notes.
 *
 * "Jump to month" is the answer to the archive problem: the journal is meant to
 * hold decades, and no one scrolls to 2011.
 */
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onGoToCheckIn: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val months by viewModel.months.collectAsState()
    val listState = rememberLazyListState()
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisible ->
                val total = listState.layoutInfo.totalItemsCount
                if (total > 0 && lastVisible >= total - PREFETCH_DISTANCE) viewModel.loadMore()
            }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            months = months,
            current = state.ceiling,
            onSelect = { yearMonth ->
                showMonthPicker = false
                viewModel.jumpToMonth(yearMonth)
            },
            onNewest = {
                showMonthPicker = false
                viewModel.showNewest()
            },
            onDismiss = { showMonthPicker = false },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        JournalControls(
            query = state.query,
            category = state.category,
            onQueryChange = viewModel::setQuery,
            onCategoryChange = viewModel::setCategory,
        )

        when {
            // Nothing at all yet.
            state.total == 0 && !state.loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "Nothing here yet.",
                    body = "Your reflections will collect here, one note at a time — as many a night as you like, for as many years as you keep going.",
                    actionLabel = "Write tonight's note",
                    onAction = onGoToCheckIn,
                )
            }

            // Filtered down to nothing.
            state.empty && state.filtered -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "No notes match.",
                    body = "Nothing in your journal matches this search. Try a shorter phrase or another category.",
                    actionLabel = "Clear search and filter",
                    onAction = viewModel::clearFilters,
                )
            }

            // Jumped to a window that has nothing in it.
            state.empty && state.jumped -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "Nothing written then.",
                    body = "There are no notes in this stretch of the journal. Pick another month, or go back to the most recent notes.",
                    actionLabel = "Back to newest",
                    onAction = viewModel::showNewest,
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = ScreenPadding,
                    end = ScreenPadding,
                    top = Spacing.sm,
                    bottom = Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                item(key = "header") {
                    JournalHeader(
                        matching = state.matching,
                        total = state.total,
                        firstDate = state.firstDate,
                        filtered = state.filtered,
                        jumped = state.jumped,
                        ceilingLabel = state.ceiling?.let { Dates.monthYearLabel(it) },
                        onClear = viewModel::clearFilters,
                        onJump = { showMonthPicker = true },
                        onNewest = viewModel::showNewest,
                    )
                }

                val groups = state.notes.groupedByDay()
                groups.forEachIndexed { index, group ->
                    item(key = "date-${group.date}") {
                        Spacer(Modifier.height(if (index == 0) Spacing.md else Spacing.lg))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.xxs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionLabel(Dates.formatDisplay(group.date))
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = if (group.notes.size == 1) "1 note" else "${group.notes.size} notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                    }
                    items(group.notes, key = { it.id }) { note ->
                        EntryCard(note)
                    }
                }

                item(key = "footer") {
                    ListFooter(
                        loading = state.loading,
                        hasMore = state.hasMore,
                        shown = state.notes.size,
                        matching = state.matching,
                    )
                }
            }
        }
    }
}

/**
 * Search + filters. Pinned, because with decades of notes the alternative is
 * scrolling thousands of rows to reach the search box.
 */
@Composable
private fun JournalControls(
    query: String,
    category: Category?,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (Category?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
            .padding(horizontal = ScreenPadding),
    ) {
        Spacer(Modifier.height(Spacing.sm))
        SearchField(query = query, onQueryChange = onQueryChange)
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            SelectableChip(
                label = "All",
                selected = category == null,
                onClick = { onCategoryChange(null) },
            )
            Category.entries.forEach { c ->
                SelectableChip(
                    label = c.label,
                    selected = category == c,
                    accent = c.color,
                    onClick = { onCategoryChange(if (category == c) null else c) },
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = if (focused) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                },
                shape = shape,
            )
            .padding(start = Spacing.sm, end = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(Spacing.xs))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            interactionSource = interaction,
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search your notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun JournalHeader(
    matching: Int,
    total: Int,
    firstDate: String?,
    filtered: Boolean,
    jumped: Boolean,
    ceilingLabel: String?,
    onClear: () -> Unit,
    onJump: () -> Unit,
    onNewest: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Journal")
            Spacer(Modifier.weight(1f))
            // The archive's one navigation control. A twenty-year journal is not
            // scrollable, it is searchable and jumpable.
            QuietButton(
                label = if (jumped) ceilingLabel ?: "Month" else "Jump to month",
                icon = Icons.Outlined.DateRange,
                onClick = onJump,
            )
        }
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = "Every night, remembered.",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(Spacing.xxs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when {
                    filtered -> "$matching of $total notes"
                    jumped -> "$total notes · up to $ceilingLabel"
                    firstDate != null -> "$total notes · since ${Dates.monthYearLabel(firstDate)}"
                    else -> "$total notes"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            when {
                filtered -> QuietButton(label = "Clear", onClick = onClear)
                jumped -> QuietButton(label = "Newest", onClick = onNewest)
            }
        }
        Spacer(Modifier.height(Spacing.xxs))
    }
}

/**
 * Every month that holds a note, newest first. Not a calendar picker: months with
 * nothing in them are not destinations, and with decades of notes the list is a
 * map of the journal rather than a date form.
 */
@Composable
private fun MonthPickerDialog(
    months: List<MonthOption>,
    current: String?,
    onSelect: (String) -> Unit,
    onNewest: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = ScreenPadding),
        title = { Text("Jump to a month") },
        text = {
            if (months.isEmpty()) {
                Text(
                    text = "No notes yet, so there is nowhere to jump to.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    item(key = "newest") {
                        MonthRow(
                            label = "Newest notes",
                            detail = null,
                            selected = current == null,
                            onClick = onNewest,
                        )
                    }
                    items(months, key = { it.yearMonth }) { month ->
                        MonthRow(
                            label = month.label,
                            detail = if (month.count == 1) "1 note" else "${month.count} notes",
                            selected = false,
                            onClick = { onSelect(month.yearMonth) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun MonthRow(
    label: String,
    detail: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

/** Spinner while a page is on its way, and a full stop when there is nothing left. */
@Composable
private fun ListFooter(
    loading: Boolean,
    hasMore: Boolean,
    shown: Int,
    matching: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.md, bottom = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
            )

            !hasMore && shown > 0 -> Text(
                text = if (shown == 1) {
                    "That's the whole journal — 1 note."
                } else {
                    "That's all $matching notes."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EntryCard(note: Entry) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTag(Category.fromName(note.category))
            Spacer(Modifier.width(Spacing.xs))
            IntensityTag(note.intensity)
            Spacer(Modifier.weight(1f))
            Text(
                text = Dates.formatClock(note.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = note.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
