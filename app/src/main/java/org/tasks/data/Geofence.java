package org.tasks.data;

import static org.tasks.data.Geofence.TABLE_NAME;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import java.io.Serializable;
import org.tasks.R;
import org.tasks.preferences.Preferences;

@Entity(tableName = TABLE_NAME, indices = @Index(name = "geo_task", value = "task"))
public class Geofence implements Serializable, Parcelable {

  public static final String TABLE_NAME = "geofences";
  public static final Table TABLE = new Table(TABLE_NAME);

  public static final LongProperty TASK = new LongProperty(TABLE, "task");
  public static final StringProperty PLACE = new StringProperty(TABLE, "place");

  public static final Parcelable.Creator<Geofence> CREATOR =
      new Parcelable.Creator<Geofence>() {
        @Override
        public Geofence createFromParcel(Parcel source) {
          return new Geofence(source);
        }

        @Override
        public Geofence[] newArray(int size) {
          return new Geofence[size];
        }
      };

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "geofence_id")
  private transient long id;

  @ColumnInfo(name = "task")
  private transient long task;

  @ColumnInfo(name = "place")
  private String place;

  @ColumnInfo(name = "radius")
  private int radius;

  @ColumnInfo(name = "arrival")
  private boolean arrival;

  @ColumnInfo(name = "departure")
  private boolean departure;

  public Geofence() {}

  @Ignore
  public Geofence(long task, String place, boolean arrival, boolean departure, int radius) {
    this(place, arrival, departure, radius);
    this.task = task;
  }

  @Ignore
  public Geofence(String place, Preferences preferences) {
    this.place = place;
    int defaultReminders =
        preferences.getIntegerFromString(R.string.p_default_location_reminder_key, 1);
    arrival = defaultReminders == 1 || defaultReminders == 3;
    departure = defaultReminders == 2 || defaultReminders == 3;
    radius = preferences.getInt(R.string.p_default_location_radius, 250);
  }

  @Ignore
  public Geofence(String place, boolean arrival, boolean departure, int radius) {
    this.place = place;
    this.arrival = arrival;
    this.departure = departure;
    this.radius = radius;
  }

  @Ignore
  public Geofence(Geofence o) {
    id = o.id;
    task = o.task;
    place = o.place;
    radius = o.radius;
    arrival = o.arrival;
    departure = o.departure;
  }

  @Ignore
  public Geofence(Parcel parcel) {
    id = parcel.readLong();
    task = parcel.readLong();
    place = parcel.readString();
    radius = parcel.readInt();
    arrival = parcel.readInt() == 1;
    departure = parcel.readInt() == 1;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getTask() {
    return task;
  }

  public void setTask(long task) {
    this.task = task;
  }

  public String getPlace() {
    return place;
  }

  public void setPlace(String place) {
    this.place = place;
  }

  public int getRadius() {
    return radius;
  }

  public void setRadius(int radius) {
    this.radius = radius;
  }

  public boolean isArrival() {
    return arrival;
  }

  public void setArrival(boolean arrival) {
    this.arrival = arrival;
  }

  public boolean isDeparture() {
    return departure;
  }

  public void setDeparture(boolean departure) {
    this.departure = departure;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Geofence location = (Geofence) o;

    if (id != location.id) {
      return false;
    }
    if (task != location.task) {
      return false;
    }
    if (radius != location.radius) {
      return false;
    }
    if (arrival != location.arrival) {
      return false;
    }
    if (departure != location.departure) {
      return false;
    }
    return place != null ? place.equals(location.place) : location.place == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + (int) (task ^ (task >>> 32));
    result = 31 * result + (place != null ? place.hashCode() : 0);
    result = 31 * result + radius;
    result = 31 * result + (arrival ? 1 : 0);
    result = 31 * result + (departure ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Location{"
        + "id="
        + id
        + ", task="
        + task
        + ", place='"
        + place
        + '\''
        + ", radius="
        + radius
        + ", arrival="
        + arrival
        + ", departure="
        + departure
        + '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeLong(id);
    out.writeLong(task);
    out.writeString(place);
    out.writeInt(radius);
    out.writeInt(arrival ? 1 : 0);
    out.writeInt(departure ? 1 : 0);
  }
}
