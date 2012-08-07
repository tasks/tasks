/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.location;

public class GeoPoint {

    private int latitude, longitude;

    public GeoPoint(int latitude, int longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getLatitudeE6() {
        return latitude;
    }

    public int getLongitudeE6() {
        return longitude;
    }

}
