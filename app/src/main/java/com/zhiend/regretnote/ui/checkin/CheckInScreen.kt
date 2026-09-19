package com.zhiend.regretnote.ui.checkin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhiend.regretnote.data.Category
import com.zhiend.regretnote.data.Entry
import com.zhiend.regretnote.ui.components.AppCard
import com.zhiend.regretnote.ui.components.CategoryTag
import com.zhiend.regretnote.ui.components.IntensityPalette
import com.zhiend.regretnote.ui.components.IntensityTag
import com.zhiend.regretnote.ui.components.PanelDivider
import com.zhiend.regretnote.ui.components.PrimaryActionButton
import com.zhiend.regretnote.ui.components.QuietButton
import com.zhiend.regretnote.ui.components.SectionLabel
import com.zhiend.regretnote.ui.components.SegmentedControl
import com.zhiend.regretnote.ui.components.SelectableChip
import com.zhiend.regretnote.ui.theme.AppGradients
import com.zhiend.regretnote.ui.theme.ScreenPadding
import com.zhiend.regretnote.ui.theme.SerifHero
import com.zhiend.regretnote.ui.theme.Spacing
import com.zhiend.regretnote.util.Dates
import java.util.Calendar
import kotlinx.coroutines.delay

private const val MAX_TEXT_LENGTH = 600

private const val QUESTION = "Is there anything today you wish you'd done differently?"

/** How long the "note deleted" bar stays up before it gives up on the undo. */
private const val UNDO_WINDOW_MILLIS = 6_000L

/** A friendly, time-aware opener for the nightly ritual. */
private fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

/**
 * Home tab: the one nightly question, and as many answers as you want.
 *
 * A day is no longer capped at one entry, so the screen is: the question, then
 * today's notes (each one stamped with the time it was written, each one
 * editable and deletable), then a way to add another. When today is still empty
 * the composer opens straight away — the empty case is still the common case.
 */
@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel,
    onOpenSettings: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenJournal: (String) -> Unit,
) {
    // null until today's notes have been read; `notes` is what the UI draws.
    val loadedNotes by viewModel.notes.collectAsState()
    val notes = loadedNotes.orEmpty()
    val streak by viewModel.streak.collectAsState()
    val memory by viewModel.memory.collectAsState()
    val pendingDraft by viewModel.draft.collectAsState()
    val deleted by viewModel.recentlyDeleted.collectAsState()
    val today = remember { Dates.today() }
    val greeting = remember { greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }

    var editing by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    var intensity by rememberSaveable { mutableIntStateOf(2) }
    /** ISO date of the draft that came back from disk, if any. */
    var restoredDraft by rememberSaveable { mutableStateOf<String?>(null) }

    /**
     * Whether the writer has touched this composer at all. Until they do, the
     * screen must not write to the draft slot *or* clear it — a launch that only
     * ever showed the question must not wipe text someone typed last night.
     */
    var touched by rememberSaveable { mutableStateOf(false) }
    var dismissedMemory by rememberSaveable { mutableStateOf<String?>(null) }

    // The keyboard is raised only when the writer asked for the composer (a tap
    // on "add another note"), never on a cold open — the first thing to read on
    // this screen is the question, not the keyboard.
    var focusWanted by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    /**
     * The composer is on screen when you are editing, or when today is genuinely
     * blank — not when today has simply not been read yet.
     */
    val formVisible = editing || loadedNotes?.isEmpty() == true

    /**
     * True while the form is a *new* note rather than an edit of one that is
     * already saved. Drafts only ever belong to new notes: editing an existing
     * note must not overwrite the text you had started for tonight.
     */
    val composing = editingId == null

    // Coming back after midnight should point at the new day, not yesterday.
    LaunchedEffect(Unit) { viewModel.refreshDay() }

    LaunchedEffect(deleted) {
        if (deleted != null) {
            delay(UNDO_WINDOW_MILLIS)
            viewModel.dismissUndo()
        }
    }

    // A draft that outlived the process. Offered whatever day it was written on:
    // "you stopped mid-sentence last night" is the one message a journal must not
    // drop, and the label says which night it was.
    //
    // Keyed on the composer being visible as well as on the draft, so the text is
    // only ever held in a field that is on screen. Restoring it behind today's
    // note list and then clearing the form on "add another note" would throw the
    // draft away without the writer ever seeing it.
    LaunchedEffect(pendingDraft, composing, formVisible) {
        val draft = pendingDraft
        if (formVisible && composing && draft != null && text.isEmpty()) {
            text = draft.text
            category = draft.category?.let { Category.fromName(it) }
            intensity = draft.intensity
            restoredDraft = draft.date
            touched = true
        }
    }

    // Every edit lands on disk, so nothing half-written is ever lost. Blank text
    // only clears the draft once the writer has actually emptied it themselves.
    LaunchedEffect(text, category, intensity, touched, composing) {
        if (touched && composing) viewModel.saveDraft(text, category, intensity)
    }

    LaunchedEffect(focusWanted, notes) {
        if (focusWanted) {
            delay(120)
            focusRequester.requestFocus()
        }
    }

    fun resetForm() {
        editing = false
        editingId = null
        text = ""
        category = null
        intensity = 2
        restoredDraft = null
        touched = false
        focusWanted = false
    }

    fun startEditing(entry: Entry) {
        editing = true
        editingId = entry.id
        text = entry.text
        category = Category.fromName(entry.category)
        intensity = entry.intensity
        restoredDraft = null
        touched = false
        focusWanted = true
    }

    fun submit() {
        val chosen = category ?: return
        val target = notes.firstOrNull { it.id == editingId }
        if (target != null) {
            viewModel.updateNote(target, text, chosen, intensity)
        } else {
            viewModel.addNote(text, chosen, intensity)
        }
        resetForm()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding),
    ) {
        Spacer(Modifier.height(Spacing.sm))

        // App bar: wordmark, streak, settings.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Undo",
                style = MaterialTheme.typography.headlineMedium.copy(
                    brush = AppGradients.accent(),
                ),
            )
            Spacer(Modifier.weight(1f))
            StreakChip(streak)
            Spacer(Modifier.width(Spacing.xxs))
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        SectionLabel("$greeting · ${Dates.formatDisplay(today)}")
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = QUESTION,
            style = SerifHero,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = if (loadedNotes?.isEmpty() != false) {
                "One honest line is enough. There's no judgment here — just a record."
            } else {
                "Written down, seen clearly. Add as many notes today as it takes."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.lg))

        // Nothing is drawn until today's notes are known, so the screen never
        // flashes the wrong half of itself (composer over an existing stack, or an
        // "add another note" button over an empty day).
        if (loadedNotes != null && !formVisible) {
            TodayNotes(
                notes = notes,
                onEdit = { startEditing(it) },
                onDelete = { viewModel.deleteNote(it) },
            )
            Spacer(Modifier.height(Spacing.md))
            PrimaryActionButton(
                label = if (notes.size == 1) "Add another note" else "Add another note (${notes.size} today)",
                icon = Icons.Outlined.Add,
                onClick = {
                    resetForm()
                    editing = true
                    focusWanted = true
                },
            )
            Spacer(Modifier.height(Spacing.xxs))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                QuietButton(
                    label = "See patterns",
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    onClick = onOpenInsights,
                )
            }
        } else if (loadedNotes != null) {
            CategoryPanel(
                category = category,
                onCategoryChange = {
                    category = it
                    touched = true
                },
                intensity = intensity,
                onIntensityChange = {
                    intensity = it
                    touched = true
                },
            )
            Spacer(Modifier.height(Spacing.md))
            WritingPanel(
                text = text,
                onTextChange = {
                    text = it.take(MAX_TEXT_LENGTH)
                    touched = true
                },
                focusRequester = focusRequester,
            )
            restoredDraft?.let { from ->
                Spacer(Modifier.height(Spacing.xs))
                DraftRestoredBar(
                    from = from,
                    today = today,
                    onDiscard = {
                        resetForm()
                        viewModel.discardDraft()
                    },
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            PrimaryActionButton(
                label = if (editing) "Save changes" else "Save tonight's reflection",
                enabled = text.isNotBlank() && category != null,
                onClick = { submit() },
            )
            if (editing && notes.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xxs))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    QuietButton(label = "Cancel", onClick = { resetForm() })
                }
            }
        }

        // The journal's reason for existing, made visible: a night from years ago
        // coming back on its own. It is placed after today's notes so it never
        // gets between the writer and the question.
        val visibleMemory = memory?.takeIf { it.date != dismissedMemory }
        if (visibleMemory != null) {
            Spacer(Modifier.height(Spacing.xl))
            SectionLabel("From the archive")
            Spacer(Modifier.height(Spacing.xs))
            MemoryCard(
                memory = visibleMemory,
                onOpen = { onOpenJournal(visibleMemory.date) },
                onDismiss = { dismissedMemory = visibleMemory.date },
            )
        }

        // Undo bar. Deletion is immediate and only this handful of seconds can
        // bring the note back, which is the right trade for a journal app.
        AnimatedVisibility(visible = deleted != null, enter = fadeIn(), exit = fadeOut()) {
            Column {
                Spacer(Modifier.height(Spacing.md))
                UndoBar(
                    label = deleted?.let { "\u201C${it.text.take(28)}\u2026\u201D deleted" } ?: "Note deleted",
                    onUndo = { viewModel.undoDelete() },
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
    }
}

/** Today's notes, newest first, each with its own time stamp. */
@Composable
private fun TodayNotes(
    notes: List<Entry>,
    onEdit: (Entry) -> Unit,
    onDelete: (Entry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        notes.forEach { note ->
            NoteCard(
                note = note,
                onEdit = { onEdit(note) },
                onDelete = { onDelete(note) },
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: Entry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(contentPadding = Spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTag(Category.fromName(note.category))
            Spacer(Modifier.width(Spacing.xs))
            IntensityTag(note.intensity)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.EditNote,
                    contentDescription = "Edit this note",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete this note",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = note.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Written at ${Dates.formatClock(note.createdAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun UndoBar(label: String, onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                MaterialTheme.shapes.medium,
            )
            .padding(start = Spacing.md, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        QuietButton(label = "Undo", onClick = onUndo)
    }
}

@Composable
private fun StreakChip(streak: Int) {
    val shape = CircleShape
    Row(
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), shape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = if (streak > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = if (streak == 1) "1 day" else "$streak days",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Step one: what it was about and how heavy it felt. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPanel(
    category: Category?,
    onCategoryChange: (Category) -> Unit,
    intensity: Int,
    onIntensityChange: (Int) -> Unit,
) {
    AppCard {
        SectionLabel("What was it about?")
        Spacer(Modifier.height(Spacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Category.entries.forEach { c ->
                SelectableChip(
                    label = c.label,
                    selected = category == c,
                    accent = c.color,
                    onClick = { onCategoryChange(c) },
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        PanelDivider()
        Spacer(Modifier.height(Spacing.md))
        SectionLabel("How heavy does it feel?")
        Spacer(Modifier.height(Spacing.sm))
        SegmentedControl(
            options = listOf("Light", "Medium", "Heavy"),
            selectedIndex = intensity - 1,
            onSelect = { onIntensityChange(it + 1) },
            accentFor = { index -> IntensityPalette.color(index + 1) },
        )
    }
}

/** A quiet line under the composer: this text came back from an earlier session. */
@Composable
private fun DraftRestoredBar(
    from: String,
    today: String,
    onDiscard: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (from == today) {
                "Draft from earlier restored."
            } else {
                "Unfinished note from ${Dates.formatLong(from)} restored."
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        QuietButton(label = "Discard draft", onClick = onDiscard)
    }
}

/**
 * One evening from years ago, served back on the home screen.
 *
 * This is what the unlimited journal is for: a note you have not thought about
 * since you wrote it, arriving on its own. It reads the newest note of that night
 * and says how many others came with it.
 */
@Composable
private fun MemoryCard(
    memory: Memory,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(Spacing.xs))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memory.label,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = Dates.formatLong(memory.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Hide this memory",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = memory.lead.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryTag(Category.fromName(memory.lead.category))
            Spacer(Modifier.width(Spacing.xs))
            IntensityTag(memory.lead.intensity)
            if (memory.extraNotes > 0) {
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = "+${memory.extraNotes} more that night",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        QuietButton(
            label = "Open that night",
            icon = Icons.AutoMirrored.Outlined.ArrowForward,
            onClick = onOpen,
        )
    }
}

/** Step two: the writing itself. */
@Composable
private fun WritingPanel(
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    AppCard {
        SectionLabel("Write it down")
        Spacer(Modifier.height(Spacing.sm))
        ReflectionField(
            text = text,
            onTextChange = onTextChange,
            focusRequester = focusRequester,
        )
    }
}

/**
 * Writing on dark paper. The field needs to look like a field: it is a recessed
 * well with a hairline that lights up in the accent colour on focus, and the
 * character count sits inside it. The limit is per note, not per day — you can
 * write as many notes as you like.
 */
@Composable
private fun ReflectionField(
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = MaterialTheme.shapes.medium
    val border by animateColorAsState(
        targetValue = if (focused) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        },
        animationSpec = tween(160),
        label = "field-border",
    )
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interaction,
        minLines = 4,
        maxLines = 8,
        decorationBox = { innerTextField ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, border, shape)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            ) {
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = "e.g. I wish I'd told them I appreciated the help.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                    innerTextField()
                }
                Text(
                    text = "${text.length}/$MAX_TEXT_LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = Spacing.xxs),
                )
            }
        },
    )
}
