package com.ethica.survey;

import java.util.Calendar;

public class LatLng {

    private final int BR;
    public final double latitude;
    public final double longitude;
    public final double accuracy;
    public final double speed;
    public final Calendar timestamp;

    public LatLng(int versionCode, double latitude, double longitude, double accu,
                        double speed, Calendar timestamp) {
        this.BR = versionCode;
        if ((-180.0D <= longitude) && (longitude < 180.0D))
            this.longitude = longitude;
        else
            this.longitude = (((longitude - 180.0D) % 360.0D + 360.0D) % 360.0D - 180.0D);
        this.latitude = Math.max(-90.0D, Math.min(90.0D, latitude));
        this.accuracy = accu;
        this.speed = speed;
        this.timestamp = timestamp;
    }

    LatLng(int versionCode, double latitude, double longitude) {
        this(versionCode, latitude, longitude, 0.0, 0.0, null);
    }

    public LatLng(double latitude, double longitude) {
        this(1, latitude, longitude);
    }

    int getVersionCode() {
        return this.BR;
    }

    public int hashCode() {
        // TODO: This is outdated. It does not consider speed, accu and timestamp
        int j = 1;
        long l = Double.doubleToLongBits(this.latitude);
        j = 31 * j + (int)(l ^ l >>> 32);
        l = Double.doubleToLongBits(this.longitude);
        j = 31 * j + (int)(l ^ l >>> 32);
        return j;
    }

    public boolean equals(Object o) {
        // TODO: This is outdated. It does not consider speed, accu and timestamp
        if (this == o)
            return true;
        if (!(o instanceof LatLng))
            return false;
        LatLng localLatLng = (LatLng)o;
        return (Double.doubleToLongBits(this.latitude) == Double
                .doubleToLongBits(localLatLng.latitude))
                && (Double.doubleToLongBits(this.longitude) == Double
                        .doubleToLongBits(localLatLng.longitude));
    }

    public String toString() {
        if (timestamp == null) {
            return "(" + this.latitude + "," + this.longitude + ")";
        } else {
            return "(" + Study170GeofenceController.sSimpleDateFormat.format(timestamp.getTime()) + "," +
                    this.latitude + "," +
                    this.longitude + "," +
                    this.accuracy + "," +
                    this.speed + ")";
        }
    }
}
