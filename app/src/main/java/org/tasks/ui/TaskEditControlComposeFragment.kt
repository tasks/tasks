package org.tasks.ui

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R

abstract class TaskEditControlComposeFragment : TaskEditControlFragment() {

    override fun bind(parent: ViewGroup?) =
        (parent?.findViewById(R.id.compose_view) as ComposeView).apply {
            setContent {
                MdcTheme {
                    Body()
                }
            }
        }

    @Composable
    protected abstract fun Body()

    override val rootLayout = R.layout.control_set_template_compose
}