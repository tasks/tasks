package org.tasks.dialogs

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.ImportTasksViewModel
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class ImportTasksDialog : DialogFragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: ImportTasksViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val uri = requireArguments().getString(ARG_URI)!!.toUri()

        return ComposeView(requireContext()).apply {
            setContent {
                TasksTheme(
                    theme = theme.themeBase.index,
                    primary = theme.themeColor.primaryColor,
                ) {
                    org.tasks.compose.ImportTasksDialog(
                        uri = uri,
                        viewModel = viewModel,
                        onFinished = { dismiss() }
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_URI = "uri"

        fun newImportTasksDialog(uri: Uri): ImportTasksDialog {
            return ImportTasksDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_URI, uri.toString())
                }
            }
        }
    }
}
