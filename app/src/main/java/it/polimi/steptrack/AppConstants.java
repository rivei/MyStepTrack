package it.polimi.steptrack;

public final class AppConstants {

    /**
     * For Locations
     */
    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    //The fastest rate for active location updates. Updates will never be more frequent than this value.
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * For stepcount sensors
      */
    public final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;


    /**
     * For StepTrackingService
     */
    public final static String STEPTRACKINGSERVICE = "it.polimi.steptrack.services.StepTrackingService";
    public final static int SERVICE_RUNNING = 1;
    public final static int SERVICE_RUNNING_FOREGROUND = 2;
}
