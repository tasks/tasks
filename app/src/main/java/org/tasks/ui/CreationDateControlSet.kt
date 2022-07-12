package org.tasks.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.preferences.Preferences
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class CreationDateControlSet : TaskEditControlComposeFragment() {
    @Inject lateinit var locale: Locale

    @Composable
    override fun Body() {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", locale)
            viewModel.creationDate?.let {
                Text(
                    text = stringResource(id = R.string.sort_created_group, formatter.format(it))
                )
            }
            viewModel.modificationDate?.let {
                Text(
                    text = stringResource(id = R.string.sort_modified_group, formatter.format(it))
                )
            }
            viewModel.completionDate?.takeIf { it > 0 }?.let {
                Text(
                    text = stringResource(id = R.string.sort_completion_group, formatter.format(it))
                )
            }
        }
    }

    override val icon = R.drawable.ic_outline_info_24px

    override fun controlId() = TAG

    companion object {
        const val TAG = R.string.TEA_ctrl_creation_date
    }
}