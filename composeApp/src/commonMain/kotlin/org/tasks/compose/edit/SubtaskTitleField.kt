package org.tasks.compose.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource
import org.tasks.data.SubtaskRow
import org.tasks.data.travellingWith
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.no_title

private const val DisabledAlpha = 0.38f

private const val FocusAttempts = 5

@Composable
internal fun RowScope.SubtaskTitleField(
    key: String,
    title: String?,
    textStyle: TextStyle,
    focused: Boolean,
    caretLanding: CaretLanding?,
    onEditingChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onAddAnother: () -> Unit,
    onRemove: () -> Unit,
    onMoveFocus: (Int) -> Unit,
    onCaretPlaced: () -> Unit,
    onFocused: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var field by remember(key) {
        mutableStateOf(title.orEmpty().let { TextFieldValue(it, TextRange(it.length)) })
    }
    var layout by remember(key) { mutableStateOf<TextLayoutResult?>(null) }
    val bringCaretIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(layout, field.selection, editing) {
        if (!editing) {
            return@LaunchedEffect
        }
        val caret = layout?.cursorRectAt(field.selection.end) ?: return@LaunchedEffect
        try {
            bringCaretIntoView.bringIntoView(caret)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }
    val external = title.orEmpty()
    LaunchedEffect(external, editing) {
        if (editing || external == field.text) {
            return@LaunchedEffect
        }
        val caret = field.selection.end.coerceAtMost(external.length)
        field = TextFieldValue(external, TextRange(caret))
    }
    LaunchedEffect(caretLanding) {
        val landing = caretLanding ?: return@LaunchedEffect
        if (!focusRequester.take()) {
            onCaretPlaced()
            return@LaunchedEffect
        }
        val offset = when (landing) {
            CaretLanding.LineEnd -> layout?.getLineEnd(0, visibleEnd = true)
                ?: field.text.length
            CaretLanding.TextEnd -> field.text.length
            CaretLanding.TextStart -> 0
        }
        field = field.copy(selection = TextRange(offset))
        onCaretPlaced()
    }
    BasicTextField(
        value = field,
        onValueChange = {
            field = it
            onTitleChange(it.text)
        },
        onTextLayout = { layout = it },
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        textStyle = textStyle,
        decorationBox = { title ->
            Box {
                if (field.text.isEmpty() && !editing) {
                    Text(
                        text = stringResource(Res.string.no_title),
                        style = textStyle.copy(
                            color = textStyle.color.copy(alpha = DisabledAlpha),
                        ),
                    )
                }
                title()
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (field.text.isNotBlank()) {
                    onAddAnother()
                }
            },
        ),
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 12.dp)
            .bringIntoViewRequester(bringCaretIntoView)
            .focusRequester(focusRequester)
            .onFocusChanged {
                editing = it.isFocused
                onEditingChange(it.isFocused)
                if (it.isFocused) {
                    onFocused()
                }
            }
            .onPreviewKeyEvent { event ->
                val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                if (enter && !event.isShiftPressed) {
                    if (event.type == KeyEventType.KeyDown && field.text.isNotBlank()) {
                        onAddAnother()
                    }
                    return@onPreviewKeyEvent true
                }
                if (event.key == Key.Backspace && field.text.isEmpty()) {
                    if (event.type == KeyEventType.KeyDown) {
                        onRemove()
                    }
                    return@onPreviewKeyEvent true
                }
                val step = when (event.key) {
                    Key.DirectionUp -> -1
                    Key.DirectionDown -> 1
                    else -> return@onPreviewKeyEvent false
                }
                val lines = layout
                if (lines != null) {
                    val line = lines.lineAt(
                        if (step < 0) field.selection.start else field.selection.end
                    )
                    val leaving = if (step < 0) line == 0 else line == lines.lineCount - 1
                    if (!leaving) {
                        return@onPreviewKeyEvent false
                    }
                }
                if (event.type == KeyEventType.KeyDown) {
                    onMoveFocus(step)
                }
                true
            },
    )

    LaunchedEffect(key, focused) {
        if (focused) {
            focusRequester.take()
        }
    }
}

private fun TextLayoutResult.cursorRectAt(offset: Int): Rect =
    getCursorRect(offset.coerceIn(0, layoutInput.text.length))

private fun TextLayoutResult.lineAt(offset: Int): Int =
    getLineForOffset(offset.coerceIn(0, layoutInput.text.length))

private suspend fun FocusRequester.take(): Boolean {
    repeat(FocusAttempts) {
        if (runCatching { requestFocus() }.isSuccess) {
            return true
        }
        withFrameNanos { }
    }
    return false
}

internal fun List<SubtaskRow>.nextEditable(from: Int, step: Int): Int? =
    generateSequence(from + step) { it + step }
        .takeWhile { it in indices }
        .firstOrNull { !this[it].node.deleted }

internal fun List<SubtaskRow>.caretAfterRemoving(
    from: Int,
    keepsNested: Boolean,
): Pair<String, CaretLanding>? {
    nextEditable(from, -1)?.let { return this[it].key to CaretLanding.TextEnd }
    val last = if (keepsNested || from !in indices) from else travellingWith(from + 1, this[from]).last
    nextEditable(last, 1)?.let { return this[it].key to CaretLanding.TextStart }
    return null
}

internal fun List<SubtaskRow>.caretAfterMoving(from: Int, step: Int): Pair<String, CaretLanding>? =
    nextEditable(from, step)?.let {
        this[it].key to if (step > 0) CaretLanding.LineEnd else CaretLanding.TextEnd
    }

internal enum class CaretLanding {
    LineEnd,

    TextEnd,

    TextStart,
}

internal data class CaretArrival(val key: String, val landing: CaretLanding)

internal fun List<SubtaskRow>.canTakeCaret(key: String): Boolean =
    any { it.key == key && !it.node.deleted }

@Stable
internal class CaretHandoff {
    var arriving by mutableStateOf<CaretArrival?>(null)
        private set

    fun handTo(key: String, landing: CaretLanding) {
        arriving = CaretArrival(key, landing)
    }

    fun landingFor(key: String): CaretLanding? = arriving?.takeIf { it.key == key }?.landing

    fun placed() {
        arriving = null
    }

    fun standDownIfGone(rows: List<SubtaskRow>) {
        val target = arriving ?: return
        if (!rows.canTakeCaret(target.key)) {
            arriving = null
        }
    }
}

@Composable
internal fun rememberCaretHandoff(rows: List<SubtaskRow>): CaretHandoff {
    val handoff = remember { CaretHandoff() }
    LaunchedEffect(handoff.arriving, rows) { handoff.standDownIfGone(rows) }
    return handoff
}
