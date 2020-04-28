package com.todoroo.astrid.api;

import android.os.Parcel;
import com.google.common.primitives.Longs;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.data.Task;
import java.util.List;
import org.tasks.data.Tag;

public class IdListFilter extends Filter {

  public static final Creator<IdListFilter> CREATOR =
      new Creator<IdListFilter>() {

        @Override
        public IdListFilter createFromParcel(Parcel source) {
          return new IdListFilter(source);
        }

        @Override
        public IdListFilter[] newArray(int size) {
          return new IdListFilter[size];
        }
      };

  private List<Long> ids;

  public IdListFilter(List<Long> ids) {
    super("", getQueryTemplate(ids));
    this.ids = ids;
  }

  private IdListFilter(Parcel source) {
    readFromParcel(source);
  }

  private static QueryTemplate getQueryTemplate(List<Long> ids) {
    return new QueryTemplate()
        .join(Join.left(Tag.TABLE, Tag.TASK.eq(Task.ID)))
        .where(Task.ID.in(ids));
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeLongArray(Longs.toArray(ids));
  }

  @Override
  protected void readFromParcel(Parcel source) {
    super.readFromParcel(source);
    long[] ids = new long[source.readInt()];
    source.setDataPosition(source.dataPosition() - 1);
    source.readLongArray(ids);
    this.ids = Longs.asList(ids);
  }
}
