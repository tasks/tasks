package org.tasks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.tasks.databinding.FragmentTaskEditEmptyBinding
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingFragment
import org.tasks.themes.ThemeColor
import javax.inject.Inject

class EmptyTaskEditFragment : InjectingFragment() {

    @Inject lateinit var themeColor: ThemeColor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentTaskEditEmptyBinding.inflate(inflater)

        themeColor.apply(binding.toolbar.toolbar)

        return binding.root
    }

    override fun inject(component: FragmentComponent) = component.inject(this)
}