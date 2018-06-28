package org.tasks.data;

import android.arch.paging.PositionalDataSource;
import android.arch.persistence.room.RoomDatabase;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.tasks.analytics.Tracker;
import timber.log.Timber;

public class LimitOffsetDataSource extends PositionalDataSource<Task> {

  private final String mCountQuery;
  private final String mLimitOffsetQuery;
  private final RoomDatabase mDb;

  public LimitOffsetDataSource(RoomDatabase db, String query) {
    mDb = db;
    mCountQuery = "SELECT COUNT(*) FROM ( " + query + " )";
    mLimitOffsetQuery = "SELECT * FROM ( " + query + " ) LIMIT ? OFFSET ?";
  }

  @WorkerThread
  private int countItems() {
    Cursor cursor;
    try {
      cursor = mDb.query(mCountQuery, null);
    } catch (Exception e) {
      Timber.e(e);
      Tracker.report(e);
      return 0;
    }
    try {
      if (cursor.moveToFirst()) {
        return cursor.getInt(0);
      }
      return 0;
    } finally {
      cursor.close();
    }
  }

  @SuppressWarnings("WeakerAccess")
  protected List<Task> convertRows(Cursor cursor) {
    List<Task> result = new ArrayList<>();
    while (cursor.moveToNext()) {
      result.add(new Task(cursor));
    }
    return result;
  }

  @Nullable
  @WorkerThread
  private List<Task> loadRange(int startPosition, int loadCount) {
    Cursor cursor = mDb.query(mLimitOffsetQuery, new Object[] {loadCount, startPosition});
    //noinspection TryFinallyCanBeTryWithResources
    try {
      return convertRows(cursor);
    } finally {
      cursor.close();
    }
  }

  @Override
  public void loadInitial(
      @NonNull LoadInitialParams params, @NonNull LoadInitialCallback<Task> callback) {
    int totalCount = countItems();
    if (totalCount == 0) {
      callback.onResult(Collections.emptyList(), 0, 0);
      return;
    }

    // bound the size requested, based on known count
    final int firstLoadPosition = computeInitialLoadPosition(params, totalCount);
    final int firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount);

    // convert from legacy behavior
    List<Task> list = loadRange(firstLoadPosition, firstLoadSize);
    if (list != null && list.size() == firstLoadSize) {
      callback.onResult(list, firstLoadPosition, totalCount);
    } else {
      // null list, or size doesn't match request
      // The size check is a WAR for Room 1.0, subsequent versions do the check in Room
      invalidate();
    }
  }

  @WorkerThread
  @Override
  public void loadRange(
      @NonNull LoadRangeParams params, @NonNull LoadRangeCallback<Task> callback) {
    List<Task> list = loadRange(params.startPosition, params.loadSize);
    if (list != null) {
      callback.onResult(list);
    } else {
      invalidate();
    }
  }
}
