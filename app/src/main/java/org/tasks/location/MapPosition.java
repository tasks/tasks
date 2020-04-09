package org.tasks.location;

import android.os.Parcel;
import android.os.Parcelable;

public class MapPosition implements Parcelable {

  public static final Parcelable.Creator<MapPosition> CREATOR =
      new Parcelable.Creator<MapPosition>() {
        @Override
        public MapPosition createFromParcel(Parcel in) {
          return new MapPosition(in);
        }

        @Override
        public MapPosition[] newArray(int size) {
          return new MapPosition[size];
        }
      };
  private final double latitude;
  private final double longitude;
  private final float zoom;

  public MapPosition(double latitude, double longitude) {
    this(latitude, longitude, 15.0f);
  }

  public MapPosition(double latitude, double longitude, float zoom) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.zoom = zoom;
  }

  private MapPosition(Parcel in) {
    latitude = in.readDouble();
    longitude = in.readDouble();
    zoom = in.readFloat();
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  float getZoom() {
    return zoom;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeDouble(latitude);
    dest.writeDouble(longitude);
    dest.writeFloat(zoom);
  }
}
