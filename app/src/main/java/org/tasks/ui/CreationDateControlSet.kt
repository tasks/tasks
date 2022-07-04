package org.tasks.ui

import android.view.ViewGroup
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.databinding.ControlSetCreationDateBinding
import org.tasks.preferences.Preferences
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class CreationDateControlSet : TaskEditControlFragment() {
    @Inject lateinit var locale: Locale
    @Inject lateinit var preferences: Preferences

    private lateinit var date: TextView

    override fun bind(parent: ViewGroup?) =
        ControlSetCreationDateBinding.inflate(layoutInflater, parent, true).let {
            val formatter = SimpleDateFormat( "yyyy-MM-dd HH:mm", locale)
            date = it.creationDate
            var dateRecords = context?.getString( R.string.sort_created_group, formatter.format( viewModel.creationDate!!)) + "\n" +
                    context?.getString( R.string.sort_modified_group, formatter.format( viewModel.modificationDate!!))
            if (viewModel.completionDate!! != 0L)
                dateRecords += "\n" + context?.getString( R.string.sort_completion_group, formatter.format( viewModel.completionDate!!))
            date.text = dateRecords
            it.root
        }

    override val icon = R.drawable.ic_outline_info_24px

    override fun controlId() = TAG

    companion object {
        const val TAG = R.string.TEA_ctrl_creation_date
    }
}