
package com.ethica.survey;

/**
 * @author Mohammad Hashemian (mohammad@ethicadata.com)
 */
public enum PointsOfInterest {

    ON_CAMPUS ("on_campus", "On Campus"),
    OFF_CAMPUS("off_campus", "Off Campus");

    private LatLng[] mPolygon;
    private String mName;
    private String mFriendlyName;

    private PointsOfInterest(final String name, final String friendlyName) {
        int ordinal = ordinal();
        mName = name;
        mFriendlyName = friendlyName;
        mPolygon = getPolygon(ordinal);
    }

    /**
     * Checks whether the specified function is located inside the polygon or
     * not. The code is borrowed from http://stackoverflow.com/a/7199522/697716
     */
    private boolean containts(final LatLng point) {
        double angle = 0;
        final int edgeNo = mPolygon.length;

        for (int i = 0; i < edgeNo; i++) {
            final LatLng firstPoint = mPolygon[i];
            final LatLng secondPoint = mPolygon[(i + 1) % edgeNo];
            final double y1 = firstPoint.latitude - point.latitude;
            final double x1 = firstPoint.longitude - point.longitude;
            final double y2 = secondPoint.latitude - point.latitude;
            final double x2 = secondPoint.longitude - point.longitude;

            double dtheta, theta1, theta2;
            theta1 = Math.atan2(y1, x1);
            theta2 = Math.atan2(y2, x2);
            dtheta = theta2 - theta1;
            while (dtheta > Math.PI)
                dtheta -= (2 * Math.PI);
            while (dtheta < -Math.PI)
                dtheta += (2 * Math.PI);

            angle += dtheta;
        }

        if (Math.abs(angle) < Math.PI)
            return false;
        else
            return true;
    }

    @Override
    public String toString() {
        return mFriendlyName;
    }

    public String getName() {
        return mName;
    }

    public static PointsOfInterest whereIs(final LatLng ll) {
        for (final PointsOfInterest poi : PointsOfInterest.values()) {
            if (poi == OFF_CAMPUS) {
                continue;
            }
            if (poi.containts(ll)) {
                return poi;
            }
        }
        return OFF_CAMPUS;
    }

    public static LatLng[] getPolygon(int ordinal) {
        switch (ordinal) {
            case 0: // CAMPUS
                return new LatLng[] {
                        new LatLng(52.139707,-106.639843),
                        new LatLng(52.128320,-106.646315),
                        new LatLng(52.128320,-106.623008),
                        new LatLng(52.139707,-106.623008)
                };
            case 1:
            default:
                return new LatLng[] {};
        }
    }

    public static PointsOfInterest fromString(String poiStr) {
        if (poiStr == null) {
            return null;
        }
        if (poiStr.equalsIgnoreCase(ON_CAMPUS.mName)) {
            return PointsOfInterest.ON_CAMPUS;
        } else if (poiStr.equalsIgnoreCase(OFF_CAMPUS.mName)) {
            return PointsOfInterest.OFF_CAMPUS;
        }
        return null;
    }
}
