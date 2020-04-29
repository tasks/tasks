package org.tasks.data;

import static com.mapbox.api.geocoding.v5.GeocodingCriteria.TYPE_ADDRESS;
import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.data.Place.TABLE_NAME;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.astrid.helper.UUIDHelper;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fortuna.ical4j.model.property.Geo;
import org.tasks.R;
import org.tasks.location.MapPosition;
import org.tasks.themes.CustomIcons;

@Entity(tableName = TABLE_NAME, indices = @Index(name = "place_uid", value = "uid", unique = true))
public class Place implements Serializable, Parcelable {

  public static final String KEY = "place";
  static final String TABLE_NAME = "places";
  public static final Table TABLE = new Table(TABLE_NAME);

  public static final StringProperty UID = new StringProperty(TABLE, "uid");

  public static final Parcelable.Creator<Place> CREATOR =
      new Parcelable.Creator<Place>() {
        @Override
        public Place createFromParcel(Parcel source) {
          return new Place(source);
        }

        @Override
        public Place[] newArray(int size) {
          return new Place[size];
        }
      };
  private static final Pattern pattern = Pattern.compile("(\\d+):(\\d+):(\\d+\\.\\d+)");
  private static final Pattern COORDS =
      Pattern.compile("^\\d+°\\d+'\\d+\\.\\d+\"[NS] \\d+°\\d+'\\d+\\.\\d+\"[EW]$");

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "place_id")
  private transient long id;

  @ColumnInfo(name = "uid")
  private String uid;

  @ColumnInfo(name = "name")
  private String name;

  @ColumnInfo(name = "address")
  private String address;

  @ColumnInfo(name = "phone")
  private String phone;

  @ColumnInfo(name = "url")
  private String url;

  @ColumnInfo(name = "latitude")
  private double latitude;

  @ColumnInfo(name = "longitude")
  private double longitude;

  @ColumnInfo(name = "place_color")
  private int color;

  @ColumnInfo(name = "place_icon")
  private int icon = -1;

  public Place() {}

  @Ignore
  public Place(Place o) {
    id = o.id;
    uid = o.uid;
    name = o.name;
    address = o.address;
    phone = o.phone;
    url = o.url;
    latitude = o.latitude;
    longitude = o.longitude;
    color = o.color;
    icon = o.icon;
  }

  @Ignore
  public Place(Parcel parcel) {
    id = parcel.readLong();
    uid = parcel.readString();
    name = parcel.readString();
    address = parcel.readString();
    phone = parcel.readString();
    url = parcel.readString();
    latitude = parcel.readDouble();
    longitude = parcel.readDouble();
    color = parcel.readInt();
    icon = parcel.readInt();
  }

  private static String formatCoordinate(double coordinates, boolean latitude) {
    String output =
        android.location.Location.convert(Math.abs(coordinates), Location.FORMAT_SECONDS);
    Matcher matcher = pattern.matcher(output);
    if (matcher.matches()) {
      return String.format(
          "%s°%s'%s\"%s",
          matcher.group(1),
          matcher.group(2),
          matcher.group(3),
          latitude ? (coordinates > 0 ? "N" : "S") : (coordinates > 0 ? "E" : "W"));
    } else {
      return Double.toString(coordinates);
    }
  }

  public static Place newPlace(Geo geo) {
    Place place = newPlace();
    place.setLatitude(geo.getLatitude().doubleValue());
    place.setLongitude(geo.getLongitude().doubleValue());
    return place;
  }

  @Nullable
  public static Place newPlace(@Nullable MapPosition mapPosition) {
    if (mapPosition == null) {
      return null;
    }

    Place place = newPlace();
    place.setLatitude(mapPosition.getLatitude());
    place.setLongitude(mapPosition.getLongitude());
    return place;
  }

  public static Place newPlace(CarmenFeature feature) {
    String address = feature.placeName();
    List<String> types = feature.placeType();
    Place place = newPlace();
    place.setName(
        types != null && types.contains(TYPE_ADDRESS)
            ? String.format("%s %s", feature.address(), feature.text())
            : feature.text());
    place.setAddress(address);
    place.setLatitude(feature.center().latitude());
    place.setLongitude(feature.center().longitude());
    return place;
  }

  public static Place newPlace() {
    Place place = new Place();
    place.setUid(UUIDHelper.newUUID());
    return place;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

  public int getIcon() {
    return icon == -1 ? CustomIcons.getPLACE() : icon;
  }

  public void setIcon(int icon) {
    this.icon = icon;
  }

  public String getDisplayName() {
    if (!isNullOrEmpty(name) && !COORDS.matcher(name).matches()) {
      return name;
    }
    if (!isNullOrEmpty(address)) {
      return address;
    }
    return String.format(
        "%s %s", formatCoordinate(getLatitude(), true), formatCoordinate(getLongitude(), false));
  }

  public String getDisplayAddress() {
    return isNullOrEmpty(address) ? null : address.replace(String.format("%s, ", name), "");
  }

  public void open(Context context) {
    if (context == null) {
      return;
    }
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(getGeoUri()));
    PackageManager pm = context.getPackageManager();
    List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
    if (resolveInfos.isEmpty()) {
      Toast.makeText(context, R.string.no_application_found_link, Toast.LENGTH_SHORT).show();
    } else {
      context.startActivity(intent);
    }
  }

  private String getGeoUri() {
    return String.format("geo:%s,%s?q=%s", latitude, longitude, Uri.encode(getDisplayName()));
  }

  public MapPosition getMapPosition() {
    return new MapPosition(latitude, longitude);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Place)) {
      return false;
    }
    Place place = (Place) o;
    return id == place.id
        && Double.compare(place.latitude, latitude) == 0
        && Double.compare(place.longitude, longitude) == 0
        && color == place.color
        && icon == place.icon
        && Objects.equals(uid, place.uid)
        && Objects.equals(name, place.name)
        && Objects.equals(address, place.address)
        && Objects.equals(phone, place.phone)
        && Objects.equals(url, place.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, uid, name, address, phone, url, latitude, longitude, color, icon);
  }

  @Override
  public String toString() {
    return "Place{"
        + "id="
        + id
        + ", uid='"
        + uid
        + '\''
        + ", name='"
        + name
        + '\''
        + ", address='"
        + address
        + '\''
        + ", phone='"
        + phone
        + '\''
        + ", url='"
        + url
        + '\''
        + ", latitude="
        + latitude
        + ", longitude="
        + longitude
        + ", color="
        + color
        + ", icon="
        + icon
        + '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeLong(id);
    out.writeString(uid);
    out.writeString(name);
    out.writeString(address);
    out.writeString(phone);
    out.writeString(url);
    out.writeDouble(latitude);
    out.writeDouble(longitude);
    out.writeInt(color);
    out.writeInt(icon);
  }
}
