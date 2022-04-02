package org.tasks.ui

import android.view.ViewGroup
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.databinding.ControlSetDeadlineBinding
import org.tasks.locale.Locale
import org.tasks.preferences.Preferences
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class CreationDateControlSet : TaskEditControlFragment() {
    @Inject lateinit var locale: Locale
    @Inject lateinit var preferences: Preferences

    private lateinit var date: TextView

    override fun bind(parent: ViewGroup?) =
        ControlSetDeadlineBinding.inflate(layoutInflater, parent, true).let {
            var formatter: SimpleDateFormat = SimpleDateFormat( "yyyy-MM-dd HH:mm")
            date = it.dueDate
            date.text = formatter.format( viewModel.creationDate!!);
            it.root
        }

    override val icon = R.drawable.ic_outline_schedule_24px

    override fun controlId() = TAG

    companion object {
        const val TAG = R.string.TEA_ctrl_creation_date
    }
}