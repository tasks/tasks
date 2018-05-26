package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.HashMap;
import java.util.Map;
import org.tasks.R;
import org.tasks.data.Tag;
import org.tasks.data.TagData;

public class TagFilter extends Filter {

  /** Parcelable Creator Object */
  public static final Parcelable.Creator<TagFilter> CREATOR =
      new Parcelable.Creator<TagFilter>() {

        /** {@inheritDoc} */
        @Override
        public TagFilter createFromParcel(Parcel source) {
          TagFilter item = new TagFilter();
          item.readFromParcel(source);
          return item;
        }

        /** {@inheritDoc} */
        @Override
        public TagFilter[] newArray(int size) {
          return new TagFilter[size];
        }
      };

  private static final int TAG = R.drawable.ic_label_24dp;
  private String uuid;

  private TagFilter() {
    super();
  }

  public TagFilter(TagData tagData) {
    super(tagData.getName(), queryTemplate(tagData.getRemoteId()), getValuesForNewTask(tagData));
    uuid = tagData.getRemoteId();
    tint = tagData.getColor();
    icon = TAG;
  }

  private static QueryTemplate queryTemplate(String uuid) {
    return new QueryTemplate()
        .join(Join.inner(Tag.TABLE.as("mtags"), Task.UUID.eq(Field.field("mtags.task_uid"))))
        .where(
            Criterion.and(
                Field.field("mtags.tag_uid").eq(uuid), TaskDao.TaskCriteria.activeAndVisible()));
  }

  private static Map<String, Object> getValuesForNewTask(TagData tagData) {
    Map<String, Object> values = new HashMap<>();
    values.put(Tag.KEY, tagData.getName());
    return values;
  }

  public String getUuid() {
    return uuid;
  }

  /** {@inheritDoc} */
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

  @Override
  public boolean supportsSubtasks() {
    return true;
  }
}
