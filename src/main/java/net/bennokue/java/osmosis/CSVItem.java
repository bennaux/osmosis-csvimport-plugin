package net.bennokue.java.osmosis;

/**
 * This class lets the CSVImportPlugin store lines from the CSV file to input at
 * a cache. Only relevant information will be stored: <ul><li>The OSM
 * id</li><li>The OSM latitude</li><li>The OSM longitude</li><li>The data to
 * import to a tag</li></ul>
 *
 * The fields are all public to keeps things (and expectations) as simple as
 * possible.
 *
 * @author bennokue
 */
public class CSVItem {

    /**
     * The OSM ID of the line.
     */
    public final long OSM_ID;
    /**
     * The OSM latitude of the line.
     */
    public final double OSM_LAT;
    /**
     * The OSM longitude of the line.
     */
    public final double OSM_LON;
    /**
     * The data element that should be imported to a tag.
     */
    public final String DATA;

    /**
     * Simple standard constructor.
     *
     * @param osmId The OSM ID of the line.
     * @param osmLat The OSM latitude of the line.
     * @param osmLon The OSM longitude of the line.
     * @param data The data element that should be imported to a tag.
     */
    public CSVItem(long osmId, double osmLat, double osmLon, String data) {
        this.OSM_ID = osmId;
        this.OSM_LAT = osmLat;
        this.OSM_LON = osmLon;
        this.DATA = data;
    }

    /**
     * Calculate the distance (meters) from the OSM node stored at this object
     * to any lat/lon point.
     *
     * @param latitude Latitude of the other point.
     * @param longitude Longitude of the other point.
     * @return Distance from the OSM node stored in this object to an other
     * point in meters.
     */
    public double getDistance(double latitude, double longitude) {
        return distFrom(this.OSM_LAT, this.OSM_LON, latitude, longitude);
    }

    /**
     * Distance funciton from
     * <a href="http://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-java">here</a>,
     * calculates the distance between two lat/lon pairs.
     *
     * @param lat1 Latitude of point 1.
     * @param lng1 Longitude of point 1.
     * @param lat2 Latitude of point 2.
     * @param lng2 Longitude of point 2.
     * @return The distance between the two points in meters.
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
    /*
     * equals() to be used in HashSets and dergleichen. It only uses the OSM_ID
     * to determine equality, no position data, since OSM nodes can be moved.
     */
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
    /*
     * to be used in HashSets and dergleichen. It only uses the OSM_ID to
     * determine equality, no position data, since OSM nodes can be moved.
     */
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (int) (this.OSM_ID ^ (this.OSM_ID >>> 32));
        return hash;
    }
}
