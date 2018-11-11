package it.polimi.steptrack;


public final class AppConstants {
    public static final String PACKAGE_NAME = "it.polimi.steptrack";

    public static final long MILLI2NANO = 1000000;
    public static final long SECOND2NANO = 1000 * MILLI2NANO;
    public static final long MINUTE2NANO = 60 * SECOND2NANO;
    public static final long SECOND2MILLI = 1000;
    public static final long MINUTE2MILLI = 60 * SECOND2MILLI;

    /**
     * For StepTrackingService
     */
    public final static String STEPTRACKINGSERVICE = PACKAGE_NAME + ".services.StepTrackingService";
    public final static int SERVICE_NOT_RUNNING = 0;
    public final static int SERVICE_RUNNING = 1;
    public final static int SERVICE_RUNNING_FOREGROUND = 2;
    public static final long STEP_SAVE_INTERVAL = 1000 * 60 * 60; //save every hour
    public static final int STEP_SAVE_OFFSET = 100; //save ongoing step counting to db every 50 steps;
    //continuous 50 steps defines a walking starts


    /**
     * For Locations
     */
    public static final float OUT_OF_HOME_DISTANCE = 30f;//100f; //100m
    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long NETWORK_UPDATE_INTERVAL = 15000; //15s
    public static final long GPS_UPDATE_INTERVAL = 10000; //10s
    //The fastest rate for active location updates. Updates will never be more frequent than this value.
    public static final long GPS_FAST_UPDATE_INTERVAL = 2000;//choosing 2s will get 3s or more
    public static final float UPDATE_DISTANCE_IN_METERS = 3f;

    public static final float GPS_ACCEPTABLE_ACCURACY = 25f;  //TODO: define GPS accuracy radius
    public static final float GPS_ACCURACY_THRESHOLD = 50f;
    public static final float GPS_ACCURACY_FOR_SUM = 10f;     //only points within this accuracy will be counted
    public static final float GPS_DISTANCE_THRESHOLD_FOR_SUM = GPS_ACCURACY_FOR_SUM/2;         //only sum when the distance between 2 points are bigger than this


    /**
     * For stepcount sensors
      */
    public final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;
    public final static int BATCH_LATENCY = 5000000; //in microseconds
    public static final long SCREEN_OFF_RECEIVER_DELAY = 500; //in milliseconds

    /**
     * For Geofencing
     */
    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";
    public static final float GEOFENCE_RADIUS_IN_METERS = 25; //25 meter


    /**
     * For Activity recognition
     */
    public final static String TRANSITIONS_RECEIVER_ACTION =
            PACKAGE_NAME + ".TRANSITIONS_RECEIVER_ACTION";

//    public static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST_ACTION";
//
//    public static final String ACTIVITY_EXTRA = PACKAGE_NAME + ".ACTIVITY_EXTRA";
//
//    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES";
//
//    public static final String ACTIVITY_UPDATES_REQUESTED_KEY = PACKAGE_NAME +
//            ".ACTIVITY_UPDATES_REQUESTED";
//
//    public static final String DETECTED_ACTIVITIES = PACKAGE_NAME + ".DETECTED_ACTIVITIES";
//
//    /**
//     * The desired time between activity detections. Larger values result in fewer activity
//     * detections while improving battery life. A value of 0 results in activity detections at the
//     * fastest possible rate. Getting frequent updates negatively impact battery life and a real
//     * app may prefer to request less frequent updates.
//     */
//    public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 5 * 1000;;



}
