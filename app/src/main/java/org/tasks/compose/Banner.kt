package org.tasks.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.tasks.R
import org.tasks.Tasks.Companion.IS_GENERIC

@ExperimentalAnimationApi
@Composable
fun AnimatedBanner(
    isVisible: MutableState<Boolean>,
    dismiss: () -> Unit,
    subscribe: () -> Unit,
) {
    var show by rememberSaveable { mutableStateOf(false) }
    if (isVisible.value) {
        LaunchedEffect(key1 = isVisible, block = {
            delay(500)
            show = true
        })
    } else {
        show = false
    }
    AnimatedVisibility(
        visible = show,
        enter = expandVertically(
            expandFrom = Alignment.Top
        ),
        exit = shrinkVertically(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(
                    id = if (IS_GENERIC) {
                        R.string.enjoying_tasks
                    } else {
                        R.string.tasks_needs_your_support
                    }
                ),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    id = if (IS_GENERIC) {
                        R.string.tasks_needs_your_support
                    } else {
                        R.string.support_development_subscribe
                    }
                ),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = dismiss) {
                    Text(text = stringResource(id = R.string.dismiss))
                }
                TextButton(onClick = subscribe) {
                    Text(
                        text = stringResource(
                            id = if (IS_GENERIC) {
                                R.string.TLA_menu_donate
                            } else {
                                R.string.button_subscribe
                            }
                        )
                    )
                }
            }
            Divider()
        }
    }
}