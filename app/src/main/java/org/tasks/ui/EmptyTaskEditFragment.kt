package org.tasks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.todoroo.astrid.api.Filter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.databinding.FragmentTaskEditEmptyBinding
import org.tasks.themes.ColorProvider
import org.tasks.themes.ThemeColor
import javax.inject.Inject

@AndroidEntryPoint
class EmptyTaskEditFragment : Fragment() {

    @Inject lateinit var themeColor: ThemeColor
    @Inject lateinit var colorProvider: ColorProvider

    companion object {
        const val EXTRA_FILTER = "extra_filter"

        fun newEmptyTaskEditFragment(filter: Filter): EmptyTaskEditFragment {
            val arguments = Bundle()
            arguments.putParcelable(EXTRA_FILTER, filter)
            val fragment = EmptyTaskEditFragment()
            fragment.arguments = arguments
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTaskEditEmptyBinding.inflate(inflater)

        val tint = arguments?.getParcelable<Filter>(EXTRA_FILTER)?.tint

        val color = colorProvider.getThemeColor(if (tint == null || tint == 0) {
            themeColor.primaryColor
        } else {
            tint
        })

        color.apply(binding.toolbar.toolbar)

        return binding.root
    }
}