package org.tasks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.tasks.databinding.FragmentTaskEditEmptyBinding

class EmptyTaskEditFragment : Fragment() {

    companion object {
        fun newEmptyTaskEditFragment(): EmptyTaskEditFragment {
            return EmptyTaskEditFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return FragmentTaskEditEmptyBinding.inflate(inflater).root
    }
}