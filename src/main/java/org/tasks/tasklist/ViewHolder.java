package org.tasks.tasklist;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.CheckableImageView;

import org.tasks.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * View Holder saves a lot of findViewById lookups.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class ViewHolder {
    @BindView(R.id.rowBody) public ViewGroup rowBody;
    @BindView(R.id.title) public TextView nameView;
    @BindView(R.id.completeBox) public CheckableImageView completeBox;
    @BindView(R.id.due_date) public TextView dueDate;
    @BindView(R.id.tag_block) public TextView tagBlock;
    @BindView(R.id.taskActionContainer) public View taskActionContainer;
    @BindView(R.id.taskActionIcon) public ImageView taskActionIcon;

    public Task task;
    public String tagsString; // From join query, not part of the task model
    public boolean hasFiles; // From join query, not part of the task model
    public boolean hasNotes;
    private final int fontSize;

    public ViewHolder(ViewGroup view, boolean showFullTaskTitle, int fontSize) {
        this.fontSize = fontSize;
        ButterKnife.bind(this, view);

        task = new Task();

        if (showFullTaskTitle) {
            nameView.setMaxLines(Integer.MAX_VALUE);
            nameView.setSingleLine(false);
            nameView.setEllipsize(null);
        }

        view.setTag(this);
        for(int i = 0; i < view.getChildCount(); i++) {
            view.getChildAt(i).setTag(this);
        }
    }

    public void bindView(TodorooCursor<Task> cursor) {
        tagsString = cursor.get(TaskAdapter.TAGS);
        hasFiles = cursor.get(TaskAdapter.FILE_ID_PROPERTY) > 0;
        hasNotes = cursor.get(TaskAdapter.HAS_NOTES_PROPERTY) > 0;

        // TODO: see if this is a performance issue
        task = new Task(cursor);
    }

    public void setMinimumHeight(int minRowHeight) {
        if (fontSize < 16) {
            rowBody.setMinimumHeight(0);
            completeBox.setMinimumHeight(0);
        } else {
            rowBody.setMinimumHeight(minRowHeight);
            completeBox.setMinimumHeight(minRowHeight);
        }
    }
}
