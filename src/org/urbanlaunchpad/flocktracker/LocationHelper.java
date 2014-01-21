package org.urbanlaunchpad.flocktracker;

import android.location.Location;

public final class LocationHelper {

	/**
     * Get the latitude and longitude and altitude from the Location object returned by
     * Location Services.
     *
     * @param currentLocation A Location object containing the current location
     * @return The latitude and longitude of the current location, or null if no
     * location is available.
     */
    public static String getLatLngAlt(Location currentLocation) {
        // If the location is valid
        if (currentLocation != null) {

            // Return the latitude and longitude as strings
            return currentLocation.getLatitude() + "," + currentLocation.getLongitude() + "," + 
            		currentLocation.getAltitude();
        } else {

            // Otherwise, return the empty string
            return "";
        }
    }
    
    public static String getLngLatAlt(String lng, String lat, String alt) {
    	// Return the latitude and longitude as strings
        return lng + "," + lat + "," + 
        		alt;
    }
	
}
