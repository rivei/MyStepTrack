package it.polimi.steptrack;

import com.google.android.gms.location.DetectedActivity;

import androidx.versionedparcelable.ParcelUtils;

public final class AppConstants {
    public static final String PACKAGE_NAME = "it.polimi.steptrack";

    public static final long MILLI2NANO = 1000000;
    public static final long SECOND2NANO = 1000 * MILLI2NANO;
    public static final long MINUTE2NANO = 60 * SECOND2NANO;

    /**
     * For StepTrackingService
     */
    public final static String STEPTRACKINGSERVICE = PACKAGE_NAME + ".services.StepTrackingService";
    public final static int SERVICE_NOT_RUNNING = 0;
    public final static int SERVICE_RUNNING = 1;
    public final static int SERVICE_RUNNING_FOREGROUND = 2;


    /**
     * For Locations
     */
    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000; //10s
    //The fastest rate for active location updates. Updates will never be more frequent than this value.
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2; //5s

    /**
     * For stepcount sensors
      */
    public final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;


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
