package it.polimi.steptrack;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;

public class AppUtils {

    public static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates";

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    public static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    public static void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply();
    }

    /**
     * Returns the {@code location} object as a human readable string.
     * @param location  The {@link Location}.
     */
    public static String getLocationText(Location location) {
        return location == null ? "Unknown location" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    public static String getLocationTitle(Context context) {
        return context.getString(R.string.location_updated,
                DateFormat.getDateTimeInstance().format(new Date()));
    }


    public float getUpdatedDistance(Location oldLocation, Location newLocation){
        float distance;
        /** TODO
         *There is 68% chance that user is within 100m from this location.
         *So neglect location updates with poor accuracy
         */
/*        if(curLocation.getAccuracy()>ACCURACY_THRESHOLD){
            return 0;
        }
        if(oldLocation.getLatitude() == 0 && oldLocation.getLongitude() == 0){
            oldLocation.setLatitude(curLocation.getLatitude());
            oldLocation.setLongitude(curLocation.getLongitude());
            newLocation.setLatitude(curLocation.getLatitude());
            newLocation.setLongitude(curLocation.getLongitude());
            return 0;
        }else{
            oldLocation.setLatitude(newLocation.getLatitude());
            oldLocation.setLongitude(newLocation.getLongitude());
            newLocation.setLatitude(curLocation.getLatitude());
            newLocation.setLongitude(curLocation.getLongitude());
        }*/

        // Calculate distance between last two geolocations
        distance = newLocation.distanceTo(oldLocation);
        return distance;
    }

}
