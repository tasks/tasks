package org.tasks.location;

import com.todoroo.astrid.data.Metadata;

import java.io.Serializable;

public class Geofence implements Serializable {
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
}