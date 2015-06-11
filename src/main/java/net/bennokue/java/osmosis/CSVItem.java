/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/* TODO Benno Javadoc */
package net.bennokue.java.osmosis;

/**
 *
 * @author bennokue
 */
public class CSVItem {

    public final long OSM_ID;
    public final double OSM_LAT;
    public final double OSM_LON;
    public final String DATA;

    public CSVItem(long osmId, double osmLat, double osmLon, String data) {
        this.OSM_ID = osmId;
        this.OSM_LAT = osmLat;
        this.OSM_LON = osmLon;
        this.DATA = data;
    }

    public double getDistance(double latitude, double longitude) {
        return distFrom(this.OSM_LAT, this.OSM_LON, latitude, longitude);
    }

    /**
     * Distance funciton from
     * http://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-java
     */
    public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float dist = (float) (earthRadius * c);

        return dist;
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) {
            return false;
        }
        if (that == this) {
            return true;
        }
        if (!that.getClass().equals(getClass())) {
            return false;
        }

        CSVItem thatObject = (CSVItem) that;

        return this.OSM_ID == thatObject.OSM_ID;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (int) (this.OSM_ID ^ (this.OSM_ID >>> 32));
        return hash;
    }
}
