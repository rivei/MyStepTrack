package it.polimi.steptrack;

import android.app.ActivityManager;
import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static it.polimi.steptrack.AppConstants.MILLI2NANO;
import static it.polimi.steptrack.AppConstants.SERVICE_NOT_RUNNING;
import static it.polimi.steptrack.AppConstants.SERVICE_RUNNING;
import static it.polimi.steptrack.AppConstants.SERVICE_RUNNING_FOREGROUND;
import static it.polimi.steptrack.AppConstants.STEPTRACKINGSERVICE;


public class AppUtils {

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

    private static final String KEY_FIRST_INSTALL_TIME = "first_install_time";
    public static long getFirstInstallTime(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_FIRST_INSTALL_TIME,0);
    }
    public static void setKeyFirstInstallTime(Context context, long firsttime){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(KEY_FIRST_INSTALL_TIME, firsttime)
                .apply();
    }


//    public static final String KEY_ACTIVITY_ACTIVE = "activity_active";
//    public static boolean getKeyActivityActive(Context context){
//        return PreferenceManager.getDefaultSharedPreferences(context)
//                .getBoolean(KEY_ACTIVITY_ACTIVE, false);
//    }
//    public static void setKeyActivityActive(Context context, boolean activityActive){
//        PreferenceManager.getDefaultSharedPreferences(context)
//                .edit()
//                .putBoolean(KEY_ACTIVITY_ACTIVE, activityActive)
//                .apply();
//    }

    private static final String KEY_PHONE_REBOOT = "phone_reboot";
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
    public static void removeKeyPhoneReboot(Context context){
        if(PreferenceManager.getDefaultSharedPreferences(context).contains(KEY_PHONE_REBOOT)) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().remove(KEY_PHONE_REBOOT).apply();
        }
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
                .getInt(KEY_STEP_COUNT_OFFSET, Integer.MIN_VALUE);
    }

    public static void setStepCountOffset(Context context, int stepsOffset) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(KEY_STEP_COUNT_OFFSET,stepsOffset)
                .apply();
    }

    private static final String KEY_LAST_STEP_COUNT = "last_step_count";
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
//    private static final String KEY_LAST_REPORT_TIME = "last_report_time";
//    public static long getLastReportTime(Context context){
//        return PreferenceManager.getDefaultSharedPreferences(context)
//                .getLong(KEY_LAST_REPORT_TIME, 0);
//    }
//    public static void setKeyLastReportTime(Context context, long timestamp){
//        PreferenceManager.getDefaultSharedPreferences(context)
//                .edit()
//                .putLong(KEY_LAST_REPORT_TIME, timestamp)
//                .apply();
//    }

    //For hourly record
    private static final String KEY_LAST_RECORD_TIME = "last_record_time";
    public static long getLastRecordTime(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_RECORD_TIME, 0);
    }
    public static void setKeyLastRecordTime(Context context, long timestamp){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(KEY_LAST_RECORD_TIME, timestamp)
                .apply();
    }


    private static final String KEY_RECORD_STEPS = "record_steps";
    public static int getRecordSteps(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_RECORD_STEPS,0);
    }
    public static void setKeyRecordSteps(Context context, int steps){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(KEY_RECORD_STEPS, steps)
                .apply();
    }

    public static final String KEY_SAMPLING_FREQUENCY = "sampling_frequency";
    public static long getSamplingFrequency(Context context){
        return  PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_SAMPLING_FREQUENCY,0);
    }
    public static void setKeySamplingFrequency(Context context, long freq){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(KEY_SAMPLING_FREQUENCY, freq)
                .apply();
    }

//    public static boolean timeToGenReport(long curTime){
//        Calendar c = Calendar.getInstance();
//        c.setTimeInMillis(curTime);
//
//        //mHour = c.get(Calendar.HOUR);
//        //mMinute = c.get(Calendar.MINUTE);
//        return true;
//    }

    //convert
    public static long elapsedTime2timestamp(long timeNano){
        //this value should always be negative because this function is only call realtime!!!
        long delta = (timeNano - SystemClock.elapsedRealtimeNanos())/MILLI2NANO;
        return System.currentTimeMillis() + (timeNano - SystemClock.elapsedRealtimeNanos())/MILLI2NANO;
    }

    /**
     * For location
     */
    private static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates";

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

    private static final String KEY_REQUESTING_LOCATION_UPDATES_FAST = "requesting_locaction_updates_fast";
    public static boolean getKeyRequestingLocationUpdatesFast(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES_FAST, false);
    }

    public static void setKeyRequestingLocationUpdatesFast(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES_FAST, requestingLocationUpdates)
                .apply();
    }

//    /**
//     * Returns the {@code location} object as a human readable string.
//     * @param location  The {@link Location}.
//     */
//    public static String getLocationText(Location location) {
//        return location == null ? "Unknown location" :
//                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
//    }

    public static String getLocationTitle(Context context) {
        return context.getString(R.string.location_updated,
                DateFormat.getDateTimeInstance().format(new Date()));
    }


//    public float getUpdatedDistance(Location oldLocation, Location newLocation){
//        float distance;
//        /**
//         *There is 68% chance that user is within 100m from this location.
//         *So neglect location updates with poor accuracy
//         */
///*        if(curLocation.getAccuracy()>ACCURACY_THRESHOLD){
//            return 0;
//        }
//        if(oldLocation.getLatitude() == 0 && oldLocation.getLongitude() == 0){
//            oldLocation.setLatitude(curLocation.getLatitude());
//            oldLocation.setLongitude(curLocation.getLongitude());
//            newLocation.setLatitude(curLocation.getLatitude());
//            newLocation.setLongitude(curLocation.getLongitude());
//            return 0;
//        }else{
//            oldLocation.setLatitude(newLocation.getLatitude());
//            oldLocation.setLongitude(newLocation.getLongitude());
//            newLocation.setLatitude(curLocation.getLatitude());
//            newLocation.setLongitude(curLocation.getLongitude());
//        }*/
//
//        // Calculate distance between last two geolocations
//        distance = newLocation.distanceTo(oldLocation);
//        return distance;
//    }

    /**
     * For Geofencing
     */
    public static final String KEY_PLACE_LAT = "place_lat";
    public static final String KEY_PLACE_LON = "place_lon";

    //Save chosen place
    public static void setPrefPlaceCoordinate(Context context, double lat, double lon){
        //SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME,0);//PreferenceManager.getDefaultSharedPreferences(context);
        if (lat == 0 && lon == 0) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().remove(KEY_PLACE_LAT).apply();
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().remove(KEY_PLACE_LON).apply();
        } else {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(KEY_PLACE_LAT,Double.doubleToRawLongBits(lat))
                    .apply();

            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(KEY_PLACE_LON,Double.doubleToRawLongBits(lon))
                    .apply();
            Toast.makeText(context, "Place pref set", Toast.LENGTH_LONG).show();
        }
    }

    public static Location getPrefPlaceLocation(Context context){
        //SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME,0);//PreferenceManager.getDefaultSharedPreferences(context);
        Double lat = Double.longBitsToDouble(PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_PLACE_LAT, 0));
        Double lon = Double.longBitsToDouble(PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_PLACE_LON, 0));

        Location location = new Location("null");
        if( lat!=0 && lon !=0 ){
            location.setProvider("home");
            location.setLatitude(lat);
            location.setLongitude(lon);
        }
        return location;
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


    /**
     * @return milliseconds since 1.1.1970 for tomorrow 0:00:01 local timezone
     */
    public static long getYesterdayStart(){
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTimeInMillis();
    }

    public static long getYesterdayEnd(){
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);

        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);

        return c.getTimeInMillis();
    }

    public static boolean isToday(long timestamp){
        Calendar c = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        today.setTimeInMillis(System.currentTimeMillis());
        return c.get(Calendar.DATE) == (today.get(Calendar.DATE));
    }
    //    public static long getDateStart(long timestamp){
//        if(timestamp > 0) {
//            Calendar c = Calendar.getInstance();
//            c.setTimeInMillis(timestamp);
//            c.set(Calendar.HOUR_OF_DAY, 0);
//            c.set(Calendar.MINUTE, 0);
//            c.set(Calendar.SECOND, 0);
//            c.set(Calendar.MILLISECOND, 0);
//
//            return c.getTimeInMillis();
//
////            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
////            String dateInString = c.get(Calendar.DAY_OF_MONTH) + "/" +
////                    c.get(Calendar.MONTH) + "/" +
////                    c.get(Calendar.YEAR);
////            try {
////                return sdf.parse(dateInString);
////            } catch (ParseException e) {
////                e.printStackTrace();
////                return null;
////            }
//        }else {
//            return timestamp;
//        }
//    }
//
//    /**
//     * For activity detection
//     */
//    static String detectedActivitiesToJson(ArrayList<DetectedActivity> detectedActivitiesList) {
//        Type type = new TypeToken<ArrayList<DetectedActivity>>() {}.getType();
//        return new Gson().toJson(detectedActivitiesList, type);
//    }
//
//    static ArrayList<DetectedActivity> detectedActivitiesFromJson(String jsonArray) {
//        Type listType = new TypeToken<ArrayList<DetectedActivity>>(){}.getType();
//        ArrayList<DetectedActivity> detectedActivities = new Gson().fromJson(jsonArray, listType);
//        if (detectedActivities == null) {
//            detectedActivities = new ArrayList<>();
//        }
//        return detectedActivities;
//    }

//    public static List<String> Acc2String(AppDatabase db) {
//        List<String> strings = null;
//        for (AccelerometerSample sample : db.accSampleDao().getAllSamplesSynchronous()) {
//            strings.add(sample.toString());
//        }
//        return strings;
//    }

    public static void writeFile(String filename, String fileheader, List<String> dataList){
        FileOutputStream outputStream;
        String pathToExternalStorage = Environment.getExternalStorageDirectory().toString();
        File stepTrackDir;
        File f;
        try {
            stepTrackDir = new File(pathToExternalStorage, "/StepTrack");
            if (!stepTrackDir.exists()) {
                stepTrackDir.mkdirs();
                //mainActivity.logger.i(getActivity(), TAG, "Export Dir created: " + created);
            }
            File exportDir = new File(stepTrackDir, "/export");
            if (!exportDir.exists()){
                exportDir.mkdirs();
            }
            f = new File(exportDir, filename);
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

    /**
     * For manual mode
     */
    public static final String KEY_MANUAL_MODE = "manual_mode";
    public static boolean getKeyMandualMode(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_MANUAL_MODE, false);
    }

    public static void setKeyManualMode(Context context, boolean manualModeOn){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_MANUAL_MODE, manualModeOn)
                .apply();
    }
}
