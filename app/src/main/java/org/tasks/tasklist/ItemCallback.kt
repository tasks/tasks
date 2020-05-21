package org.tasks.tasklist

import androidx.recyclerview.widget.DiffUtil
import org.tasks.data.TaskContainer

internal class ItemCallback : DiffUtil.ItemCallback<TaskContainer>() {
    override fun areItemsTheSame(old: TaskContainer, new: TaskContainer) = old.id == new.id

    override fun areContentsTheSame(old: TaskContainer, new: TaskContainer) = old == new
}