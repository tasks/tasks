package org.tasks.location;

import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.astrid.data.Metadata;

import java.io.Serializable;

public class Geofence implements Serializable, Parcelable {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final int radius;
    private long taskId;
    private long metadataId;

    public Geofence(Metadata metadata) {
        this(metadata.getValue(GeofenceFields.PLACE),
                metadata.getValue(GeofenceFields.LATITUDE),
                metadata.getValue(GeofenceFields.LONGITUDE),
                metadata.getValue(GeofenceFields.RADIUS));
        metadataId = metadata.getId();
        taskId = metadata.getTask();
    }

    public Geofence(String name, double latitude, double longitude, int radius) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public Metadata toMetadata() {
        Metadata metadata = new Metadata();
        metadata.setKey(GeofenceFields.METADATA_KEY);
        metadata.setValue(GeofenceFields.PLACE, name);
        metadata.setValue(GeofenceFields.LATITUDE, latitude);
        metadata.setValue(GeofenceFields.LONGITUDE, longitude);
        metadata.setValue(GeofenceFields.RADIUS, radius);
        return metadata;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getRadius() {
        return radius;
    }

    public long getMetadataId() {
        return metadataId;
    }

    public long getTaskId() {
        return taskId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Geofence geofence = (Geofence) o;

        if (Double.compare(geofence.latitude, latitude) != 0) return false;
        if (Double.compare(geofence.longitude, longitude) != 0) return false;
        if (radius != geofence.radius) return false;
        if (taskId != geofence.taskId) return false;
        if (metadataId != geofence.metadataId) return false;
        return !(name != null ? !name.equals(geofence.name) : geofence.name != null);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name != null ? name.hashCode() : 0;
        temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + radius;
        result = 31 * result + (int) (taskId ^ (taskId >>> 32));
        result = 31 * result + (int) (metadataId ^ (metadataId >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Geofence{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", radius=" + radius +
                ", taskId=" + taskId +
                ", metadataId=" + metadataId +
                '}';
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeDouble(latitude);
        out.writeDouble(longitude);
        out.writeInt(radius);
    }

    public static final Parcelable.Creator<Geofence> CREATOR = new Parcelable.Creator<Geofence>() {
        @Override
        public Geofence createFromParcel(Parcel source) {
            String name = source.readString();
            double latitude = source.readDouble();
            double longitude = source.readDouble();
            int radius = source.readInt();
            return new Geofence(name, latitude, longitude, radius);
        }

        @Override
        public Geofence[] newArray(int size) {
            return new Geofence[size];
        }
    };
}