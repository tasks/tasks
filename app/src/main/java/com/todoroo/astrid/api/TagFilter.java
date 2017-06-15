package com.todoroo.astrid.api;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.tasks.R;

public class TagFilter extends Filter {

    private static final int TAG = R.drawable.ic_label_24dp;

    private String uuid;

    private TagFilter() {
        super();
    }

    public TagFilter(TagData tagData) {
        super(tagData.getName(), queryTemplate(tagData.getUuid()), getValuesForNewTask(tagData));
        uuid = tagData.getUuid();
        tint = tagData.getColor();
        icon = TAG;
    }

    public String getUuid() {
        return uuid;
    }

    private static QueryTemplate queryTemplate(String uuid) {
        Criterion fullCriterion = Criterion.and(
                Field.field("mtags." + Metadata.KEY.name).eq(TaskToTagMetadata.KEY),
                Field.field("mtags." + TaskToTagMetadata.TAG_UUID.name).eq(uuid),
                Field.field("mtags." + Metadata.DELETION_DATE.name).eq(0),
                TaskDao.TaskCriteria.activeAndVisible());
        return new QueryTemplate().join(Join.inner(Metadata.TABLE.as("mtags"), Task.UUID.eq(Field.field("mtags." + TaskToTagMetadata.TASK_UUID.name))))
                .where(fullCriterion);
    }

    private static ContentValues getValuesForNewTask(TagData tagData) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Metadata.KEY.name, TaskToTagMetadata.KEY);
        contentValues.put(TaskToTagMetadata.TAG_NAME.name, tagData.getName());
        contentValues.put(TaskToTagMetadata.TAG_UUID.name, tagData.getUuid());
        return contentValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(uuid);
    }

    @Override
    protected void readFromParcel(Parcel source) {
        super.readFromParcel(source);
        uuid = source.readString();
    }

    /**
     * Parcelable Creator Object
     */
    public static final Parcelable.Creator<TagFilter> CREATOR = new Parcelable.Creator<TagFilter>() {

        /**
         * {@inheritDoc}
         */
        @Override
        public TagFilter createFromParcel(Parcel source) {
            TagFilter item = new TagFilter();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TagFilter[] newArray(int size) {
            return new TagFilter[size];
        }

    };

    @Override
    public boolean supportsSubtasks() {
        return true;
    }
}
