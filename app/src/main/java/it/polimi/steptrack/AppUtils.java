package it.polimi.steptrack;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import it.polimi.steptrack.roomdatabase.AppDatabase;
import it.polimi.steptrack.roomdatabase.entities.AccelerometerSample;
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;

import static it.polimi.steptrack.AppConstants.SERVICE_NOT_RUNNING;
import static it.polimi.steptrack.AppConstants.SERVICE_RUNNING;
import static it.polimi.steptrack.AppConstants.SERVICE_RUNNING_FOREGROUND;
import static it.polimi.steptrack.AppConstants.STEPTRACKINGSERVICE;
import static java.lang.Math.round;

/**
 * A class for App sharedpreference management
 */
public class AppUtils {
    // Identify Shared Preference Store
    public final static String PREFS_NAME = "steptrack_prefs";

    /**
     * Returns if the service is running,running in foregound or not running
     * @param context The {@link Context}.
     * @return the satatus of the running service
     */
    public static int getServiceRunningStatus(Context context){
        int status = SERVICE_NOT_RUNNING;
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        if(manager != null){
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                    Integer.MAX_VALUE)){
                if(STEPTRACKINGSERVICE.equals(service.service.getClassName())){
                    status = SERVICE_RUNNING;
                    if (service.foreground) status = SERVICE_RUNNING_FOREGROUND;
                    break;
                }
            }
        }
        return status;
    }

    public static final String KEY_ACTIVITY_ACTIVE = "activity_active";
    public static boolean getKeyActivityActive(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ACTIVITY_ACTIVE, false);
    }
    public static void setKeyActivityActive(Context context, boolean activityActive){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_ACTIVITY_ACTIVE, activityActive)
                .apply();
    }

    public static final String KEY_PHONE_REBOOT = "phone_reboot";
    public static boolean getKeyPhoneReboot(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_PHONE_REBOOT, false);
    }
    public static void setKeyPhoneReboot(Context context, boolean phoneReboot){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_PHONE_REBOOT,phoneReboot)
                .apply();
    }


    public static final String KEY_STARTING_WALKING_SESSION = "starting_walking_session";
    public static boolean startingWalkingSession(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_STARTING_WALKING_SESSION, false);
    }

    public static void setKeyStartingWalkingSession(Context context, boolean startingWalkingSession){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_STARTING_WALKING_SESSION, startingWalkingSession)
                .apply();
    }

    /**
     * For continous step counting
     */
    public static final String KEY_STEP_COUNT_OFFSET = "step_offset";
    public static int getStepCountOffset(Context context) {
        //step counts to be subtracted
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_STEP_COUNT_OFFSET, 0);
    }

    public static void setStepCountOffset(Context context, int stepsOffset) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(KEY_STEP_COUNT_OFFSET,stepsOffset)
                .apply();
    }

    public static final String KEY_LAST_STEP_COUNT = "last_step_count";
    public static int getLastStepCount(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_LAST_STEP_COUNT,0);
    }
    public static void setKeyLastStepCount(Context context, int lastStepCount){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(KEY_LAST_STEP_COUNT, lastStepCount)
                .apply();
    }




    /**
     * For location
     */
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

//    /*** For step counts ********/
//
//    // Should the Step Counting Service be running?
//    public static boolean shouldServiceRun(Context context) {
//        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
//        return prefs.getBoolean("serviceRunning", false);
//    }
//
//    // Should the Step Counting Service be running?
//    public static void setServiceRun(Context context, boolean running) {
//        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
//        SharedPreferences.Editor prefsEditor = prefs.edit();
//        prefsEditor.putBoolean("serviceRunning", running);
//        prefsEditor.apply();
//    }


    /**
     * For Geofencing
     */
    public static final String KEY_PLACE_LAT = "place_lat";
    public static final String KEY_PLACE_LON = "place_lon";

    //Save chosen place
    public static void setPrefPlace(Context context, Place place){
        //SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME,0);//PreferenceManager.getDefaultSharedPreferences(context);
        if (place == null) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().remove(KEY_PLACE_LAT).apply();
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().remove(KEY_PLACE_LON).apply();
        } else {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(KEY_PLACE_LAT,Double.doubleToRawLongBits(place.getLatLng().latitude))
                    .apply();

            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(KEY_PLACE_LON,Double.doubleToRawLongBits(place.getLatLng().longitude))
                    .apply();
            Toast.makeText(context, "Place pref set", Toast.LENGTH_LONG).show();
        }
    }

    public static LatLng getPrefPlaceLatLng(Context context){
        //SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME,0);//PreferenceManager.getDefaultSharedPreferences(context);
        Double lat = Double.longBitsToDouble(PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_PLACE_LAT, 0));
        Double lon = Double.longBitsToDouble(PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_PLACE_LON, 0));

        LatLng latLng = null;
        if( lat!=0 && lon !=0 ){
            latLng = new LatLng(lat,lon);
        }
        return latLng;
    }

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


    /**
     * For activity detection
     */
    static String detectedActivitiesToJson(ArrayList<DetectedActivity> detectedActivitiesList) {
        Type type = new TypeToken<ArrayList<DetectedActivity>>() {}.getType();
        return new Gson().toJson(detectedActivitiesList, type);
    }

    static ArrayList<DetectedActivity> detectedActivitiesFromJson(String jsonArray) {
        Type listType = new TypeToken<ArrayList<DetectedActivity>>(){}.getType();
        ArrayList<DetectedActivity> detectedActivities = new Gson().fromJson(jsonArray, listType);
        if (detectedActivities == null) {
            detectedActivities = new ArrayList<>();
        }
        return detectedActivities;
    }

    public static List<String> Acc2String(AppDatabase db) {
        List<String> strings = null;
        for (AccelerometerSample sample : db.accSampleDao().getAllSamplesSynchronous()) {
            strings.add(sample.toString());
        }
        return strings;
    }

    public static void writeFile(String filename, String fileheader, List<String> dataList){
        FileOutputStream outputStream;
        File f;
        try {
            f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);
            outputStream = new FileOutputStream(f);

            // Print header
            outputStream.write(fileheader.getBytes());
            // Print Raw Data
            for (String s : dataList) {
                outputStream.write(
                        (s != null) ? s.getBytes() : "NULL".getBytes()
                );
                outputStream.write("\n".getBytes());
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
