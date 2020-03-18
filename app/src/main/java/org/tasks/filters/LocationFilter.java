package org.tasks.filters;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.HashMap;
import java.util.Map;
import org.tasks.R;
import org.tasks.data.Geofence;
import org.tasks.data.Place;

public class LocationFilter extends Filter {

  public static final Parcelable.Creator<LocationFilter> CREATOR =
      new Parcelable.Creator<LocationFilter>() {

        @Override
        public LocationFilter createFromParcel(Parcel source) {
          return new LocationFilter(source);
        }

        /** {@inheritDoc} */
        @Override
        public LocationFilter[] newArray(int size) {
          return new LocationFilter[size];
        }
      };

  private Place place;

  private LocationFilter(Parcel source) {
    super();
    readFromParcel(source);
    place = source.readParcelable(getClass().getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(place, 0);
  }

  public LocationFilter(Place place) {
    super(place.getDisplayName(), queryTemplate(place), getValuesForNewTask(place));
    this.place = place;
    tint = place.getColor();
    icon = place.getIcon();
  }

  public Place getPlace() {
    return place;
  }

  public String getUid() {
    return place.getUid();
  }

  private static QueryTemplate queryTemplate(Place place) {
    return new QueryTemplate()
        .join(Join.inner(Geofence.TABLE, Task.ID.eq(Geofence.TASK)))
        .join(Join.inner(Place.TABLE, Place.UID.eq(Geofence.PLACE)))
        .where(getCriterion(place));
  }

  private static Criterion getCriterion(Place place) {
    return Criterion.and(TaskDao.TaskCriteria.activeAndVisible(), Place.UID.eq(place.getUid()));
  }

  private static Map<String, Object> getValuesForNewTask(Place place) {
    Map<String, Object> result = new HashMap<>();
    result.put(Place.KEY, place.getUid());
    return result;
  }

  @Override
  public int getBeginningMenu() {
    return R.menu.menu_location_actions;
  }

  @Override
  public int getMenu() {
    return R.menu.menu_location_list_fragment;
  }

  @Override
  public boolean areItemsTheSame(@NonNull FilterListItem other) {
    return other instanceof LocationFilter
        && place.getUid().equals(((LocationFilter) other).getPlace().getUid());
  }

  @Override
  public boolean areContentsTheSame(@NonNull FilterListItem other) {
    return place.equals(((LocationFilter) other).getPlace()) && count == other.count;
  }

  public void openMap(Context context) {
    place.open(context);
  }
}
