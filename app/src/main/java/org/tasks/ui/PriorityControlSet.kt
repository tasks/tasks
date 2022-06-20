package org.tasks.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.data.Task
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware

@AndroidEntryPoint
class PriorityControlSet : TaskEditControlComposeFragment() {

    @Composable
    override fun Body() {
        val priority = viewModel.priority.collectAsStateLifecycleAware()
        PriorityRow(
            selected = priority.value,
            onClick = { viewModel.priority.value = it })
    }

    override val icon = R.drawable.ic_outline_flag_24px

    override fun controlId() = TAG

    companion object {
        const val TAG = R.string.TEA_ctrl_importance_pref
    }
}

@Composable
fun PriorityRow(
    selected: Int,
    onClick: (Int) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(
                top = dimensionResource(id = R.dimen.half_keyline_first),
                bottom = dimensionResource(id = R.dimen.half_keyline_first),
                end = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.TEA_importance_label),
            style = MaterialTheme.typography.body1,
        )
        Spacer(modifier = Modifier.weight(1f))
        for (i in Task.Priority.HIGH..Task.Priority.NONE) {
            PriorityButton(priority = i, selected = selected, onClick = onClick)
        }
    }
}

@Composable
fun PriorityButton(
    @Task.Priority priority: Int,
    selected: Int,
    onClick: (Int) -> Unit,
) {
    val color = when (priority) {
        in Int.MIN_VALUE..Task.Priority.HIGH -> colorResource(id = R.color.red_500)
        Task.Priority.MEDIUM -> colorResource(id = R.color.amber_500)
        Task.Priority.LOW -> colorResource(id = R.color.blue_500)
        else -> colorResource(R.color.grey_500)
    }
    RadioButton(
        selected = priority == selected,
        onClick = { onClick(priority) },
        colors = RadioButtonDefaults.colors(
            selectedColor = color,
            unselectedColor = color,
        ),
    )
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PriorityPreview() {
    MdcTheme {
        PriorityRow(selected = Task.Priority.MEDIUM)
    }
}