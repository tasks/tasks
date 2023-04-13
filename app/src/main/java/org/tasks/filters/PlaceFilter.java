package org.tasks.filters;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.data.Geofence;
import org.tasks.data.Place;
import org.tasks.data.TaskDao;
import org.tasks.themes.CustomIcons;

import java.util.HashMap;
import java.util.Map;

public class PlaceFilter extends Filter {

  public static final Parcelable.Creator<PlaceFilter> CREATOR =
      new Parcelable.Creator<PlaceFilter>() {

        @Override
        public PlaceFilter createFromParcel(Parcel source) {
          return new PlaceFilter(source);
        }

        /** {@inheritDoc} */
        @Override
        public PlaceFilter[] newArray(int size) {
          return new PlaceFilter[size];
        }
      };

  private final Place place;
  private static final Table G2 = Geofence.TABLE.as("G2");
  private static final Field G2_PLACE = Field.field("G2.place");
  private static final Field G2_TASK = Field.field("G2.task");
  private static final Table P2 = Place.TABLE.as("P2");
  private static final Field P2_UID = Field.field("P2.uid");

  private PlaceFilter(Parcel source) {
    super();
    readFromParcel(source);
    place = source.readParcelable(getClass().getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeParcelable(place, 0);
  }

  public PlaceFilter(Place place) {
    super(place.getDisplayName(), queryTemplate(place), getValuesForNewTask(place));
    this.place = place;
    id = place.getId();
    tint = place.getColor();
    icon = place.getIcon();
    if (icon == -1) {
      icon = CustomIcons.PLACE;
    }
    order = place.getOrder();
  }

  public Place getPlace() {
    return place;
  }

  public String getUid() {
    return place.getUid();
  }

  private static QueryTemplate queryTemplate(Place place) {
    return new QueryTemplate()
        .join(Join.inner(G2, Task.ID.eq(G2_TASK)))
        .join(Join.inner(P2, P2_UID.eq(G2_PLACE)))
        .where(Criterion.and(TaskDao.TaskCriteria.activeAndVisible(), G2_PLACE.eq(place.getUid())));
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
  public boolean areContentsTheSame(@NonNull FilterListItem other) {
    return place.equals(((PlaceFilter) other).getPlace()) && count == other.count;
  }

  public void openMap(Context context) {
    place.open(context);
  }
}
