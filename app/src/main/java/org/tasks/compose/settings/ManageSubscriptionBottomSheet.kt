package org.tasks.compose.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class ManageSubscriptionBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onModifySubscription()
        fun onCancelSubscription()
    }

    @Inject lateinit var theme: Theme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                TasksTheme(
                    theme = theme.themeBase.index,
                    primary = theme.themeColor.primaryColor,
                ) {
                    ManageSubscriptionSheetContent(
                        onUpgrade = {},
                        onModify = {
                            dismiss()
                            (parentFragment as? Listener)?.onModifySubscription()
                        },
                        onCancel = {
                            dismiss()
                            (parentFragment as? Listener)?.onCancelSubscription()
                        },
                        showUpgrade = false,
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            }
        }
    }
}
