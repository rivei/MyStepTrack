package it.polimi.steptrack;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class AppUtils {
    // Identify Shared Preference Store
    public final static String PREFS_NAME = "steptrack_prefs";

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

    /*** For step counts ********/

    // Should the Step Counting Service be running?
    public static boolean shouldServiceRun(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getBoolean("serviceRunning", false);
    }

    // Should the Step Counting Service be running?
    public static void setServiceRun(Context context, boolean running) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean("serviceRunning", running);
        prefsEditor.apply();
    }

    // How many steps have I walked?
    public static String getStepCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return String.format("%,d", (prefs.getInt("stepCount", 0) - prefs.getInt("stepCountSubtract", 0)));
    }

    // Set how many steps I have walked.
    public static void setStepCount(Context context, Integer steps) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putInt("stepCount", steps);
        prefsEditor.apply();
    }

//    // Set Subtract Step Count (Reset)
//    public static void resetStepCount(Context context) {
//        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
//        SharedPreferences.Editor prefsEditor = prefs.edit();
//        prefsEditor.putInt("stepCountSubtract", prefs.getInt("stepCount", 0));
//        prefsEditor.apply();
//    }
//
//    // Reset the Subtract Step Count (On Boot)
//    public static void clearStepCount(Context context) {
//        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
//        SharedPreferences.Editor prefsEditor = prefs.edit();
//        prefsEditor.putInt("stepCountSubtract", 0);
//        prefsEditor.putInt("stepCount", 0);
//        prefsEditor.apply();
//    }

    /** TODO organized in a better way
     * @return milliseconds since 1.1.1970 for tomorrow 0:00:01 local timezone
     */
    public static long getTomorrow() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 1);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DATE, 1); //oneDay after
        return c.getTimeInMillis();
    }

    public static long getToday(){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 1);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
