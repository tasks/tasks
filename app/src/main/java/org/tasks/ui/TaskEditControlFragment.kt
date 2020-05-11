package org.tasks.ui

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import butterknife.ButterKnife
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.injection.InjectingFragment

abstract class TaskEditControlFragment : InjectingFragment() {
    protected lateinit var task: Task

    var isNew = false
        private set

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.control_set_template, null)
        val content = view.findViewById<LinearLayout>(R.id.content)
        inflater.inflate(layout, content)
        val icon = view.findViewById<ImageView>(R.id.icon)
        icon.setImageResource(this.icon)
        if (isClickable) {
            content.setOnClickListener { onRowClick() }
        }
        ButterKnife.bind(this, view)
        return view
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        val args = requireArguments()
        task = args.getParcelable(EXTRA_TASK)!!
        isNew = args.getBoolean(EXTRA_IS_NEW)
    }

    protected open fun onRowClick() {}
    protected open val isClickable: Boolean
        get() = false

    protected abstract val layout: Int
    protected abstract val icon: Int
    abstract fun controlId(): Int
    open fun requiresId(): Boolean {
        return false
    }

    abstract fun apply(task: Task)

    open fun hasChanges(original: Task): Boolean {
        return false
    }

    companion object {
        const val EXTRA_TASK = "extra_task"
        const val EXTRA_IS_NEW = "extra_is_new"
    }
}