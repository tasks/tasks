package org.tasks.tasklist

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView

class BannerAdapter : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    var content: (@Composable () -> Unit)? = null

    var showBanner: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    notifyItemInserted(0)
                } else {
                    notifyItemRemoved(0)
                }
            }
        }

    class BannerViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)

    override fun getItemCount() = if (showBanner) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = BannerViewHolder(
        ComposeView(parent.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    )

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.composeView.setContent {
            content?.invoke()
        }
    }
}
