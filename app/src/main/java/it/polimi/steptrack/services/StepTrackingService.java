package it.polimi.steptrack.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
//import android.os.Handler;
//import android.os.HandlerThread;
import android.os.IBinder;
//import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

//import com.google.android.gms.location.ActivityRecognition;
//import com.google.android.gms.location.ActivityTransition;
//import com.google.android.gms.location.ActivityTransitionEvent;
//import com.google.android.gms.location.ActivityTransitionRequest;
//import com.google.android.gms.location.ActivityTransitionResult;
//import com.google.android.gms.location.DetectedActivity;
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.android.gms.tasks.Task;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.polimi.steptrack.AppConstants;
import it.polimi.steptrack.AppUtils;
import it.polimi.steptrack.R;
import it.polimi.steptrack.roomdatabase.AppDatabase;
import it.polimi.steptrack.roomdatabase.DateConverter;
import it.polimi.steptrack.roomdatabase.dao.DailySummaryDao;
import it.polimi.steptrack.roomdatabase.dao.GPSLocationDao;
import it.polimi.steptrack.roomdatabase.dao.HourlyStepsDao;
import it.polimi.steptrack.roomdatabase.dao.StepDetectedDao;
import it.polimi.steptrack.roomdatabase.dao.WalkingEventDao;
import it.polimi.steptrack.roomdatabase.dao.WalkingSessionDao;
import it.polimi.steptrack.roomdatabase.entities.DailySummary;
import it.polimi.steptrack.roomdatabase.entities.GPSLocation;
import it.polimi.steptrack.roomdatabase.entities.HourlySteps;
import it.polimi.steptrack.roomdatabase.entities.StepDetected;
import it.polimi.steptrack.roomdatabase.entities.WalkingEvent;
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;
import it.polimi.steptrack.ui.MainActivity;

import static it.polimi.steptrack.AppConstants.BATCH_LATENCY_5s;
import static it.polimi.steptrack.AppConstants.FAST_UPDATE_INTERVAL_IN_MILLISECONDS;
import static it.polimi.steptrack.AppConstants.GPS_ACCEPTABLE_ACCURACY;
import static it.polimi.steptrack.AppConstants.GPS_ACCURACY_FOR_SUM;
import static it.polimi.steptrack.AppConstants.GPS_ACCURACY_THRESHOLD;
import static it.polimi.steptrack.AppConstants.MINUTE2MILLI;
import static it.polimi.steptrack.AppConstants.SECOND2MILLI;
import static it.polimi.steptrack.AppConstants.SERVICE_RUNNING_FOREGROUND;
import static it.polimi.steptrack.AppConstants.STEP_SAVE_INTERVAL;
import static it.polimi.steptrack.AppConstants.STEP_SAVE_OFFSET;
import static it.polimi.steptrack.AppConstants.UPDATE_DISTANCE_IN_METERS;
import static it.polimi.steptrack.AppConstants.UPDATE_INTERVAL_IN_MILLISECONDS;

public class StepTrackingService extends Service
        implements SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String PACKAGE_NAME = "it.polimi.steptrack";
    private static final String TAG = StepTrackingService.class.getSimpleName();
    private StepTrackingService self = this;

    /**
     * State variables
     */
    private boolean mSessionStarted = false; //Activate location update and sensors listener only when session starts
    private boolean mIsWalking = false; //Start walking = 0; stop walking = 1;
    private boolean mStepIncreasing = false;
    //private boolean mIsReported = false;

    private long mTransitionEnterTime = -1L;
    private long mTransitionExitTime = -1L;
    private boolean mOutofHome = false; //TODO: radius greater than threshold (30 meters + accuracy).
    private boolean mManualMode = false; //when manual mode is true, walking session doesn't depends on other things;
    private int mGPSLostTimes = 0;
    private int mGPSStableTimes = 0;

    /**
     * For App notification
     */
    private static final String CHANNEL_ID = "channel_01"; //notification channel name

//    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
//
//    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
//    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
//            ".started_from_notification";

    private final IBinder mBinder = new LocalBinder(); //for binding Main activity

    private static final int NOTIFICATION_ID = 12345678; //The identifier for the notification displayed for the foreground service.
    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private String mNotificationContentText;


    // Acquire a reference to the system Location Manager
    LocationManager mLocationManager;
    LocationListener mLocationListener;

//    /**
//     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderClient}.
//     */
//    private LocationRequest mLocationFastRequest;
//    private LocationRequest mLocationSlowRequest;
//
//    /**
//     * Provides access to the Fused Location Provider API.
//     */
//    private FusedLocationProviderClient mFusedLocationClient;
//
//    /**
//     * Callback for changes in location.
//     */
//    private LocationCallback mLocationCallback;

//    private Handler mServiceHandler;

    /**
     * The current location.
     */
    private Location mCurrentLocation;
    private Location mLastLocation;
//    private LatLng mHomeCoordinate = null;
    private float mTotalDistance = 0f;

//    /**
//     * For activity detection
//     */
//    private PendingIntent mPendingIntent;
//    private TransitionsReceiver mTransitionsReceiver;

    private SensorManager mSensorManager;
    private Sensor countSensor = null;
    private Sensor stepDetectSensor = null;
    private Sensor accSensor = null;
    private Sensor gyroSensor = null;
    private Sensor magnSensor = null;

    private float[] accelerometerMatrix = new float[3];
    private float[] gyroscopeMatrix = new float[3];
    private float[] magneticMatrix = new float[3];

    private long sensorRecordInterval = 0;
    private long mCurSensorTime = -1L;
    private long mLastSensorUpdate = -1L;

    private int mTotalStepsCount = 0;
    private int mHourlyStepsOffset = 0;
    private int mStepsAtStart = 0;
    private int mStepsDetect = 0;
    private int mStepCountOffset = 0;
    private long mLast20StepTime = 0L;
    private int mLast20StepOffset = 0;

    /**
     *  for database
     */
    private AppDatabase mDB;
    //    private GPSLocation mGPSLocation;
    private WalkingSession mWalkingSession;
    private long mWalkingSessionId = -1;

    private File mStepTrackDir = null;
    private File mSessionFile = null;
    private final String sessionHeader = "timestamp,acc_X,acc_Y,acc_Z,gyro_X,gyro_Y,gyro_Z," +
            "magn_X,magn_Y,magn_Z,latitude,longitude,accuracy,speed \n";


    public StepTrackingService() { //Empty constructor
    }

    @Override
    public void onCreate() {
        Log.w(TAG, "On Creating");
        if (AppUtils.getFirstInstallTime(self) == 0) {
            AppUtils.setKeyFirstInstallTime(self, System.currentTimeMillis());
        }
        mDB = AppDatabase.getInstance(this);

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                onNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                if(!provider.equals(LocationManager.GPS_PROVIDER)) return;
                /* This is called when the GPS status alters */
                switch (status) {
                    case LocationProvider.OUT_OF_SERVICE:
                        Log.v(TAG, "Status Changed: Out of Service");
                        Toast.makeText(self, "Status Changed: Out of Service",
                                Toast.LENGTH_SHORT).show();
                        mGPSStableTimes = 0;
                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        Log.v(TAG, "Status Changed: Temporarily Unavailable");
                        Toast.makeText(self, "Status Changed: Temporarily Unavailable",
                                Toast.LENGTH_SHORT).show();
                        if(mGPSStableTimes>0) mGPSStableTimes--;
                        break;
                    case LocationProvider.AVAILABLE:
                        Log.v(TAG, "Status Changed: Available");
                        Toast.makeText(self, "Status Changed: Available",
                                Toast.LENGTH_SHORT).show();
                        mGPSLostTimes = 0;
                        break;
                }
                autoSessionManagement();
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
                mGPSStableTimes = 0;
                autoSessionManagement();
            }
        };


//        /**
//         * setup fused location client and call back
//         */
//        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//
//        mLocationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                super.onLocationResult(locationResult);
//                onNewLocation(locationResult.getLastLocation());
//            }
//        };
//
//        initLocationRequest(); //for location request when walking is detected.


//        HandlerThread handlerThread = new HandlerThread(TAG);
//        handlerThread.start();
//        mServiceHandler = new Handler(handlerThread.getLooper());
        //***HandlerThread needs to call myHandlerThread.quit() to free the resources and stop the execution of the thread.

        mManualMode = AppUtils.getKeyMandualMode(self);

        /**
         * Set up sensors
         */
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            countSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            stepDetectSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            magnSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        if (countSensor != null) {
            Toast.makeText(this, "Started Counting Steps", Toast.LENGTH_LONG).show();
            mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI, BATCH_LATENCY_5s);
        } else {
            Toast.makeText(this, "Step count sensor missing. Device not Compatible!", Toast.LENGTH_LONG).show();
            this.stopSelf();
        }
        long freq = AppUtils.getSamplingFrequency(self);
        if (freq == 0) {
            sensorRecordInterval = SECOND2MILLI / 50; //default 50 hz
        } else {
            sensorRecordInterval = SECOND2MILLI / freq;
        }

        //************************* For step count *********************//
        // Setup Step Counter
        mStepCountOffset = AppUtils.getStepCountOffset(self); //this will only change either at system start or after daily report
        mTotalStepsCount = AppUtils.getLastStepCount(this) - mStepCountOffset;

        // Get Notification Manager
        mNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }

//        /**
//         * Activity Detection
//         */
//        Intent intent = new Intent(AppConstants.TRANSITIONS_RECEIVER_ACTION);
//        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
//        mTransitionsReceiver = new TransitionsReceiver();
//        registerReceiver(mTransitionsReceiver, new IntentFilter(AppConstants.TRANSITIONS_RECEIVER_ACTION));

        /**
         * Setup File path for tracking records
         */
        String pathToExternalStorage = Environment.getExternalStorageDirectory().toString();
        mStepTrackDir = new File(pathToExternalStorage, "/StepTrack");
        if (!mStepTrackDir.exists()) {
            Boolean created = mStepTrackDir.mkdirs();
            //mainActivity.logger.i(getActivity(), TAG, "Export Dir created: " + created);
        }
        File exportDir = new File(mStepTrackDir, "/export");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
    }

    //This is called only when there is Start service
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Foreground service started");

        //************************* For step count ~*********************//
//        //start Monitoring activities and locations
//        setupActivityTransitions();

        PreferenceManager.getDefaultSharedPreferences(self)
                .registerOnSharedPreferenceChangeListener(this);

        //start foreground in all situations
        startForeground(NOTIFICATION_ID, getNotification(true));
        // Restart the service if its killed
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
        //stopForeground(true); //Let notification always on;
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        //stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    //Here is the place where the service shows in the notification;
    // It is activated when the App UI goes to the background
    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Last client unbound from service");

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
//        if (!mChangingConfiguration) {//TODO: && AppUtils.requestingLocationUpdates(this)) {
//            Log.i(TAG, "Starting foreground service");
//            startForeground(NOTIFICATION_ID, getNotification(true)); //Anyway works for Oreo
//        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
//        // Unregister the transitions
//        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mPendingIntent)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Log.i(TAG, "Transitions successfully unregistered.");
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.e(TAG, "Transitions could not be unregistered: " + e);
//                    }
//                });
//        if (mTransitionsReceiver != null) {
//            unregisterReceiver(mTransitionsReceiver);
//            mTransitionsReceiver = null;
//        }

//        mServiceHandler.removeCallbacksAndMessages(null);
        mLocationManager.removeUpdates(mLocationListener);

        PreferenceManager.getDefaultSharedPreferences(self)
                .unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    /**
     * Makes a request for location updates. Note that the
     * {@link SecurityException} is merely log.
     */

    public boolean requestLocationUpdates(boolean fastUpdate) {
        boolean isSuccess;
        Log.i(TAG, "Requesting location updates:" + fastUpdate);
//        AppUtils.setRequestingLocationUpdates(this, true);
//        try {
//            //TODO: locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new MyLocationListener());
//            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
//            if(fastUpdate) {
//                mFusedLocationClient.requestLocationUpdates(mLocationFastRequest,
//                        mLocationCallback, Looper.myLooper());
//                AppUtils.setKeyRequestingLocationUpdatesFast(this, true);
//            }else {
//                mFusedLocationClient.requestLocationUpdates(mLocationSlowRequest,
//                        mLocationCallback, Looper.myLooper());
//                AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
//                updateNotification("searching for GPS...");
//            }
//            isSuccess = true;
//        } catch (SecurityException unlikely) {
//            AppUtils.setRequestingLocationUpdates(this, false);
//            AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
//            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
//            isSuccess = false;
//            updateNotification("Lost location permission. Could not request updates. " + unlikely);
//        }

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            isSuccess = false;
        }else {
            if(fastUpdate) {
                mLocationManager.removeUpdates(mLocationListener);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        UPDATE_INTERVAL_IN_MILLISECONDS/4, UPDATE_DISTANCE_IN_METERS, mLocationListener);
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        FAST_UPDATE_INTERVAL_IN_MILLISECONDS, UPDATE_DISTANCE_IN_METERS, mLocationListener);
            }else {
                mLocationManager.removeUpdates(mLocationListener);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        UPDATE_INTERVAL_IN_MILLISECONDS, UPDATE_DISTANCE_IN_METERS, mLocationListener);
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        UPDATE_INTERVAL_IN_MILLISECONDS, UPDATE_DISTANCE_IN_METERS, mLocationListener);
                updateNotification("searching for GPS...");
            }

            isSuccess = true;
        }
        return isSuccess;
    }

    private void startRecording(){
        mWalkingSession = new WalkingSession();
        mWalkingSession.mStartTime = System.currentTimeMillis();
//        mWalkingSession.mStartNano = SystemClock.elapsedRealtimeNanos();
        mStepsAtStart = mTotalStepsCount;
        mTotalDistance = 0f;

        Thread t = new Thread() {
            public void run() {
                mWalkingSessionId = mDB.sessionDao().insert(mWalkingSession);
                mWalkingSession.sid = mWalkingSessionId;
                /**
                 * create walking session file
                 */
                if (!mStepTrackDir.exists()) {
                    if(!mStepTrackDir.mkdirs()){
                        Log.e(TAG,"error creating folder");
                    }
                }
                mSessionFile = new File(mStepTrackDir, mWalkingSessionId+".csv");
                if(!mSessionFile.exists()) {
                    try {
                        mSessionFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "error in file creation");
                    }
                }
                FileOutputStream outputStream;
//                String sessionInfo = "StartTimestamp," + mWalkingSession.mStartTime +
//                        ",StartInNano," + mWalkingSession.mStartNano + "\n"; //elapsed time problem solved;
                try {
                    outputStream = new FileOutputStream(mSessionFile);
//                    outputStream.write(sessionInfo.getBytes());
                    outputStream.write(sessionHeader.getBytes()); //write header
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e(TAG,"create session file failed.");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "write session header failed");
                }
            }
        };
        t.start();
        mCurSensorTime = 0L;
        int numofSensors = registerSensorListener();
        if (numofSensors > 0) {
            if (numofSensors < 4){
                Toast.makeText(this, "some sensor not compatible!", Toast.LENGTH_LONG).show();
            }
            Log.i(TAG, "Start recording");
            Toast.makeText(this, "Starts session", Toast.LENGTH_SHORT).show();
            if (AppUtils.getServiceRunningStatus(this) == SERVICE_RUNNING_FOREGROUND){
                updateNotification("Recording session");
            }
        } else {
            Toast.makeText(this, "No sensor available!", Toast.LENGTH_LONG).show();
        }
    }

    public void manualStartNewSession() {
        Log.i(TAG, "Requesting new walking session");
        if(requestLocationUpdates(true)){
            mSessionStarted = true;
            startRecording();
        }else {
            mSessionStarted = false;
        }
        AppUtils.setKeyStartingWalkingSession(self,mSessionStarted);

    }

    private int registerSensorListener(){
        //boolean isSuccess = false;
        int numofSensors = 0;
        if(accSensor != null){
            mSensorManager.registerListener(self, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
            numofSensors++;
        }
        if (gyroSensor != null){
            mSensorManager.registerListener(self, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
            numofSensors++;
        }
        if(magnSensor != null){
            mSensorManager.registerListener(self, magnSensor, SensorManager.SENSOR_DELAY_FASTEST);
            numofSensors++;
        }
        if(stepDetectSensor != null){
            mSensorManager.registerListener(self, stepDetectSensor, SensorManager.SENSOR_DELAY_NORMAL,0);
            numofSensors++;
        }
        return numofSensors;
//        if(numofSensors >= 4){
//            isSuccess = true;
//        }
//        return isSuccess;
    }

    /**
     * Removes location updates. Note that the
     * {@link SecurityException} is merely log .
     */
    public boolean removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
//        boolean isSuccess;
//        boolean isFast = AppUtils.getKeyRequestingLocationUpdatesFast(self);
//        try {
//            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
//            AppUtils.setRequestingLocationUpdates(this, false);
//            AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
//            isSuccess = true;
//            updateNotification("Counting steps");
//        } catch (SecurityException unlikely) {
//            AppUtils.setRequestingLocationUpdates(this, true);
//            AppUtils.setKeyRequestingLocationUpdatesFast(this,isFast);
//            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
//            isSuccess = false;
//            updateNotification("Lost location permission. Could not remove updates. " + unlikely);
//        }
//        return isSuccess;

        // Remove the listener you previously added
        mLocationManager.removeUpdates(mLocationListener);
        AppUtils.setRequestingLocationUpdates(this, false);
        AppUtils.setKeyRequestingLocationUpdatesFast(this, false);

        return true;
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > MINUTE2MILLI *2;
        boolean isSignificantlyOlder = timeDelta < -MINUTE2MILLI *2;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    private void stopRecording(String inputTag){
        if (mWalkingSessionId != -1) {
//            mWalkingSession.mEndNano = SystemClock.elapsedRealtimeNanos();
            mWalkingSession.mEndTime = System.currentTimeMillis();
            mWalkingSession.mDuration = (mWalkingSession.mEndTime - mWalkingSession.mStartTime)/1000; //convert to seconds
            //SharedPreferences prefs = this.getSharedPreferences(PREFS_NAME, 0);
            mWalkingSession.mStepCount = mTotalStepsCount - mStepsAtStart;
            mWalkingSession.mStepDetect = mStepsDetect;
            mWalkingSession.mDistance = mTotalDistance;
            mWalkingSession.mAverageSpeed = mTotalDistance/mWalkingSession.mDuration;
            mStepsDetect = 0;
            mWalkingSession.mTag = inputTag;

            new updateSessionAsyncTask(mDB.sessionDao(),mDB.locationDao())
                    .execute(mWalkingSession);

            mWalkingSessionId = -1;
            mLastLocation = null;
            mTotalDistance = 0f;
        }
        mSensorManager.unregisterListener(this); //unregiester motion sensors
        mCurSensorTime = -1L;
        //Never stop step counter sensor listening
        mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI, BATCH_LATENCY_5s);
        mSessionFile = null;
        updateNotification( "Session not recording");
        Log.e(TAG, "Stop recording.");
    }

    public void manualStopSession() {
        Log.i(TAG, "Manually stopping sensor session.");
        if(removeLocationUpdates()){
            if (mWalkingSessionId != -1) {
//                mWalkingSession.mEndNano = SystemClock.elapsedRealtimeNanos();
                mWalkingSession.mEndTime = System.currentTimeMillis();
                mWalkingSession.mDuration = (mWalkingSession.mEndTime - mWalkingSession.mStartTime)/1000;
                mWalkingSession.mStepCount = mTotalStepsCount - mStepsAtStart;
                mWalkingSession.mStepDetect = mStepsDetect;
                mWalkingSession.mDistance = mTotalDistance;
                mWalkingSession.mAverageSpeed = mTotalDistance/mWalkingSession.mDuration;

                mStepsDetect = 0;
            }
            mSensorManager.unregisterListener(this); //unregiester motion sensors
            mCurSensorTime = -1L;
            mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
            mSessionStarted = false;
            mSessionFile = null;
            mLastLocation = null;
            mTotalDistance = 0f;
            updateNotification( "Session not recording");
        }else {
            mSessionStarted = true;
        }
//        clearSensorOffset();
        AppUtils.setKeyStartingWalkingSession(self,mSessionStarted);
    }

    public void updateSessionTag(String inputTag) {
        if (mWalkingSessionId != -1) {
            mWalkingSession.mTag = inputTag;

            new updateSessionAsyncTask(mDB.sessionDao(),mDB.locationDao())
                    .execute(mWalkingSession);

            mWalkingSessionId = -1;
        }
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification(boolean isFirstTime) {
        String msg = "Counting Steps";
        if (mNotificationContentText != null) msg = mNotificationContentText;
        if (isFirstTime) {
            // PendingIntent to launch activity.
            PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, MainActivity.class), 0);
            mNotificationBuilder.setContentIntent(activityPendingIntent)
                    .setContentText(msg)
                    .setContentTitle(AppUtils.getLocationTitle(this))
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH) //TODO: check if lower priority works
                    //.setTicker(text)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(false)
                    .setOnlyAlertOnce(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mNotificationBuilder.setVisibility(Notification.VISIBILITY_SECRET);
            }
            // Set the Channel ID for Android O.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationBuilder.setChannelId(CHANNEL_ID); // Channel ID
            }
        }
        // Update Step Count
        mNotificationBuilder.setSmallIcon(R.mipmap.ic_tracking); //It's a must
        mNotificationBuilder.setContentTitle(mTotalStepsCount + " steps taken");
        mNotificationBuilder.setContentText(msg);

        return mNotificationBuilder.build();
    }

//    private void getLastLocation() {
//        //Log.w(TAG, "getting last location from onCreate");
//        try {
//            mFusedLocationClient.getLastLocation()
//                    .addOnCompleteListener(task -> {
//                        if (task.isSuccessful() && task.getResult() != null) {
//                            mCurrentLocation = task.getResult();
//
//                            //Log.w(TAG, "The inital location: " + mCurrentLocation);
//                        } else {
//                            Log.w(TAG, "Failed to get location.");
//                        }
//                    });
//        } catch (SecurityException unlikely) {
//            Log.e(TAG, "Lost location permission." + unlikely);
//        }
//    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);

        mCurrentLocation = location;
        GPSLocation gpsLocation = new GPSLocation();
        gpsLocation.GTimestamp = mCurrentLocation.getTime();
        gpsLocation.latitude = mCurrentLocation.getLatitude();
        gpsLocation.longitude = mCurrentLocation.getLongitude();
        gpsLocation.accuracy = mCurrentLocation.getAccuracy();
        gpsLocation.provider = mCurrentLocation.getProvider();
        if (mCurrentLocation.hasSpeed()){
            gpsLocation.speed = mCurrentLocation.getSpeed();
        }else {
            gpsLocation.speed = -1;
        }
        if(mCurrentLocation.hasBearing()){
            gpsLocation.bearing = mCurrentLocation.getBearing();
        }else {
            gpsLocation.bearing = -1;
        }

        gpsLocation.isWalking = mIsWalking;
        gpsLocation.session_id = mWalkingSessionId;
        new insertLocationAsyncTask(mDB.locationDao()).execute(gpsLocation);

        if(mCurrentLocation.getProvider().equals(LocationManager.GPS_PROVIDER)){
            mGPSLostTimes = 0;
            if (mCurrentLocation.getAccuracy() < GPS_ACCEPTABLE_ACCURACY){
                mGPSStableTimes++;
            }else {
                if (mGPSStableTimes > 0) mGPSStableTimes--;
            }
        }else {
            mGPSLostTimes++;
        }

//        if (mCurrentLocation.hasSpeed() && mCurrentLocation.hasBearing()) {
//            float speed = mCurrentLocation.getSpeed();
//            if(speed <= 0) {
//                mGPSLostTimes++;
//            }
//            else {
//                mGPSLostTimes = 0;
//            }
//
//            if (mCurrentLocation.getAccuracy() < GPS_ACCEPTABLE_ACCURACY) {
//                mGPSStableTimes++;
//            }else {
//                if(mGPSStableTimes > 0) mGPSStableTimes--;
//            }
//
//        }else {
//            mGPSLostTimes++;
//        }
//        if(mCurrentLocation.getAccuracy() > GPS_ACCURACY_THRESHOLD) {
//            mGPSLostTimes++;
//        }

        autoSessionManagement();//check if should start automatically;

        if (mSessionStarted && mWalkingSessionId != -1 ) {
            if(mLastLocation == null){
                mLastLocation = mCurrentLocation;
            }else{
                //TODO: improve algorithm ####
                if (mCurrentLocation.getAccuracy() <= GPS_ACCURACY_FOR_SUM &&
                        mCurrentLocation.distanceTo(mLastLocation) >= mCurrentLocation.getAccuracy()/2 && //GPS_DISTANCE_THRESHOLD_FOR_SUM)
                        mCurrentLocation.getSpeed() > 0){

                    mTotalDistance += mCurrentLocation.distanceTo(mLastLocation);
                    mLastLocation = mCurrentLocation;
                }
            }
        }
    }

//    /**
//     * Sets the location request parameters.update interval and accuracy
//     */
//    private void initLocationRequest() {
//        mLocationSlowRequest = new LocationRequest(); //for indoor;
//        mLocationSlowRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
//                .setFastestInterval(FAST_UPDATE_INTERVAL_IN_MILLISECONDS)
//                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        mLocationFastRequest = new LocationRequest(); //for outdoor and recording
//        mLocationFastRequest.setInterval(FAST_UPDATE_INTERVAL_IN_MILLISECONDS)
//                .setFastestInterval(1) // update as fast as possible
//                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        mCurSensorTime = AppUtils.elapsedTime2timestamp(sensorEvent.timestamp);

        if (mWalkingSessionId != -1 &&
                (mSessionFile != null) &&
                mSessionFile.exists()) {
            FileOutputStream outputStream;
            String dataLine = "";
            //long systime = SystemClock.elapsedRealtimeNanos();//System.currentTimeMillis();
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_STEP_DETECTOR:
                    mStepsDetect += 1;//+= sensorEvent.values.length;
//                    new Thread(() -> {
                        StepDetected stepDetected = new StepDetected();
                        stepDetected.steps = mStepsDetect;
                        stepDetected.timestamp = mCurSensorTime;
                        stepDetected.session_id = mWalkingSessionId;
//                        /**
//                         * NOTE: in Android 8 this is considered blocking the main thread and
//                         * writing DB in main thread is not allow.
//                         */
//                        mDB.stepDetectedDao().insert(stepDetected);
//                    }).run();
                    new insertStepAsyncTask(mDB.stepDetectedDao()).execute(stepDetected);
                    dataLine = mCurSensorTime + "," + mStepsDetect +"\n";
                    try {
                        outputStream = new FileOutputStream(mSessionFile, true);
                        outputStream.write(dataLine.getBytes());
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "write step data failed");
                    }
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    accelerometerMatrix = sensorEvent.values;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    gyroscopeMatrix = sensorEvent.values;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magneticMatrix = sensorEvent.values;
                    break;
            }

            //This is a way to have sampling frequency in file recording;
            if ((mCurSensorTime - mLastSensorUpdate) >= sensorRecordInterval) {
                mLastSensorUpdate = mCurSensorTime;
                if(mCurrentLocation != null && (mCurrentLocation.getTime()/SECOND2MILLI) == (mCurSensorTime /SECOND2MILLI)){//only log when there is location
                    dataLine = mCurSensorTime + "," +
                            accelerometerMatrix[0] + "," + accelerometerMatrix[1] + "," + accelerometerMatrix[2] + "," +
                            gyroscopeMatrix[0] + ", " + gyroscopeMatrix[1] + ", " + gyroscopeMatrix[2] + ", " +
                            magneticMatrix[0] + "," + magneticMatrix[1] + "," + magneticMatrix[2] + "," +
                            mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude() + "," +
                            mCurrentLocation.getAccuracy() + "," + mCurrentLocation.getSpeed() + "\n";
                }else {
                    dataLine = mCurSensorTime + "," +
                            accelerometerMatrix[0] + "," + accelerometerMatrix[1] + "," + accelerometerMatrix[2] + "," +
                            gyroscopeMatrix[0] + ", " + gyroscopeMatrix[1] + ", " + gyroscopeMatrix[2] + ", " +
                            magneticMatrix[0] + "," + magneticMatrix[1] + "," + magneticMatrix[2] + "," + ",,,\n";
                }
                try {
                    //Log.w(TAG,"Thread starts");
                    outputStream = new FileOutputStream(mSessionFile, true);
                    outputStream.write(dataLine.getBytes());
                    //Log.i(TAG, "write sensor data " + mCurSensorTime);
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "write sensor data failed");
                }
            }
        }

        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER){
            int systemSteps = (int)sensorEvent.values[0];
            int lastReportStep = AppUtils.getLastStepCount(self);
            long lastReportTime = AppUtils.getLastReportTime(self);
            mHourlyStepsOffset = AppUtils.getReportSteps(self);
            if(mLast20StepOffset == 0){
                mLast20StepOffset = systemSteps;
                mLast20StepTime = mCurSensorTime;
            }else {
                int stepDiff = systemSteps - mLast20StepOffset;
                long timeDiff = mCurSensorTime - mLast20StepTime;
                double ratio = 0;
                if(stepDiff != 0) ratio = timeDiff/stepDiff;
                Log.i(TAG,"timeDiff: " + timeDiff);
                Log.i(TAG,"stepDiff: " + stepDiff);
                Log.e(TAG, "ratio: " + ratio);

                if ((stepDiff >= 20 || timeDiff >= 20 * SECOND2MILLI)){
                    if(ratio < 750 && ratio > 450) {
                        mStepIncreasing = true;
                    }
                    else {
                        mStepIncreasing = false;
                    }
                    mLast20StepOffset = systemSteps;
                    mLast20StepTime = mCurSensorTime;
                }
            }

            if(mStepCountOffset==Integer.MIN_VALUE){
                mStepCountOffset = systemSteps;
                AppUtils.setStepCountOffset(self,mStepCountOffset);
            }
            mTotalStepsCount = systemSteps - mStepCountOffset;

            if (AppUtils.isBeforeToday(lastReportTime) && AppUtils.isToday(mCurSensorTime)) {
                //generate report at the first step in the new day detected
                DailySummary dailySummary = new DailySummary();
                dailySummary.steps = lastReportStep;
                dailySummary.mDate = DateConverter.toDate(AppUtils.getDateStart(lastReportTime));
                new dailyreportAsyncTask(mDB.dailySummaryDao(), mDB.sessionDao())
                        .execute(dailySummary);
                mStepCountOffset += lastReportStep;
                mTotalStepsCount = systemSteps - mStepCountOffset;
                AppUtils.setStepCountOffset(self, mStepCountOffset);
                AppUtils.setKeyLastStepCount(self, mTotalStepsCount);

                mHourlyStepsOffset = systemSteps; //maybe redundant because below logic includes this
                AppUtils.setKeyReportSteps(self, mHourlyStepsOffset);
                AppUtils.setKeyLastReportTime(self, mCurSensorTime);
                Log.i(TAG, "new day has come.");
            }

            if(!mSessionStarted) {
                //only record the hourly steps when there is no ongoing walking session;
                if ((mCurSensorTime > lastReportTime + STEP_SAVE_INTERVAL) ||
                        (systemSteps - mHourlyStepsOffset > STEP_SAVE_OFFSET)) {

                    HourlySteps hourlySteps = new HourlySteps();
                    hourlySteps.steps = systemSteps - mHourlyStepsOffset;
                    hourlySteps.timestamp = mCurSensorTime;
                    new saveStepAsyncTask(mDB.hourlyStepsDao()).execute(hourlySteps);

                    mHourlyStepsOffset = systemSteps;
                    AppUtils.setKeyLastReportTime(self, mCurSensorTime);
                    AppUtils.setKeyReportSteps(self, mHourlyStepsOffset);
                }
            }

            AppUtils.setKeyLastStepCount(this, mTotalStepsCount);

            Log.i(TAG, "Total steps count: " + mTotalStepsCount);
            //TODO: can change below to updateNotification
            if (AppUtils.getServiceRunningStatus(this) == SERVICE_RUNNING_FOREGROUND) {
                mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
            }
            //status will be checked whenever there are steps increasing, so even when the session stopped
            // at indoor walking and the elder start to go out again walking, new session can be activated.
            autoSessionManagement();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void updateNotification(String msg) {
        if (msg != null && mNotificationBuilder != null) {
            mNotificationContentText = msg;
            mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(AppUtils.KEY_PLACE_LAT) || key.equals(AppUtils.KEY_PLACE_LON)){
            //home location changed
//            mHomeCoordinate =  AppUtils.getPrefPlaceLatLng(self);
            Log.i(TAG, "home location changed.");
        }
        if(key.equals(AppUtils.KEY_MANUAL_MODE)){
            mManualMode = AppUtils.getKeyMandualMode(self);
            Log.i(TAG, "Manual mode switched.");
        }
        if(key.equals(AppUtils.KEY_SAMPLING_FREQUENCY)){
            long freq = AppUtils.getSamplingFrequency(self);
            if(freq == 0){
                sensorRecordInterval = SECOND2MILLI/50; //default 50 hz
            }else {
                sensorRecordInterval = SECOND2MILLI/freq;
            }
        }
        if (key.equals(AppUtils.KEY_STEP_COUNT_OFFSET)){
            mTotalStepsCount = AppUtils.getStepCountOffset(self);
        }
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public StepTrackingService getService() {
            return StepTrackingService.this;
        }
    }


//    private static String toTransitionType(int transitionType) {
//        switch (transitionType) {
//            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
//                return "Start Walking";
//            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
//                return "Stop Walking";
//            default:
//                return "UNKNOWN";
//        }
//    }

//    /**
//     * Sets up {@link ActivityTransitionRequest}'s for the sample app, and registers callbacks for them
//     * with a custom {@link BroadcastReceiver}
//     */
//    private void setupActivityTransitions() {
//        List<ActivityTransition> transitions = new ArrayList<>();
//        transitions.add(
//                new ActivityTransition.Builder()
//                        .setActivityType(DetectedActivity.WALKING)
//                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
//                        .build());
//        transitions.add(
//                new ActivityTransition.Builder()
//                        .setActivityType(DetectedActivity.WALKING)
//                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
//                        .build());
//        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);
//
//        // Register for Transitions Updates.
//        Task<Void> task =
//                ActivityRecognition.getClient(this)
//                        .requestActivityTransitionUpdates(request, mPendingIntent);
//        task.addOnSuccessListener(
//                new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void result) {
//                        Log.i(TAG, "Transitions Api was successfully registered.");
//                    }
//                });
//        task.addOnFailureListener(
//                new OnFailureListener() {
//                    @Override
//                    public void onFailure(Exception e) {
//                        Log.e(TAG, "Transitions Api could not be registered: " + e);
//                    }
//                });
//    }
//
//    /**
//     * A basic BroadcastReceiver to handle intents from from the Transitions API.
//     */
//    public class TransitionsReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (!TextUtils.equals(AppConstants.TRANSITIONS_RECEIVER_ACTION, intent.getAction())) {
////                mLogFragment.getLogView()
////                        .println("Received an unsupported action in TransitionsReceiver: action="
////                                + intent.getAction());
//                Toast.makeText(context, "Received an unsupported action in TransitionsReceiver: action="
//                        + intent.getAction(), Toast.LENGTH_LONG).show();
//                return;
//            }
//            if (ActivityTransitionResult.hasResult(intent)) {
//                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
//                if (result != null) {
//                    for (ActivityTransitionEvent event : result.getTransitionEvents()) {
//                        //private List<AccelerometerSample> mAccSamples;
//                        WalkingEvent mWalkingEvent = new WalkingEvent();
//                        mWalkingEvent.WeTimestamp = System.currentTimeMillis();
//                        mWalkingEvent.mTransition = event.getTransitionType();
//                        mWalkingEvent.mElapsedTime =
//                                AppUtils.elapsedTime2timestamp(event.getElapsedRealTimeNanos());
//                        //mIsWalking = mWalkingEvent.mTransition;
////                        Toast.makeText(context,toTransitionType(mWalkingEvent.mTransition),
////                                Toast.LENGTH_LONG).show();
//                        if(mWalkingEvent.mTransition == 0){
//                            mIsWalking = true;
//                            mTransitionEnterTime = mWalkingEvent.mElapsedTime;
//                        }
//                        if (mWalkingEvent.mTransition == 1){
//                            mIsWalking = false;
//                            mTransitionExitTime = mWalkingEvent.mElapsedTime;
//                        }
//
//                        new insertEventAsyncTask(mDB.walkingEventDao()).execute(mWalkingEvent);
//
//                        autoSessionManagement(); //check if need auto starting;
//                    }
//                }
//            }
//        }
//    }
//
//    private static class insertEventAsyncTask extends AsyncTask<WalkingEvent, Void, Void> {
//
//        private WalkingEventDao mAsyncTaskDao;
//
//        insertEventAsyncTask(WalkingEventDao dao) {
//            mAsyncTaskDao = dao;
//        }
//
//        @Override
//        protected Void doInBackground(WalkingEvent... walkingEvents) {
//            mAsyncTaskDao.insert(walkingEvents[0]);
//            return null;
//        }
//    }

    private static class insertStepAsyncTask extends AsyncTask<StepDetected, Void, Void> {

        private StepDetectedDao mAsyncTaskDao;

        insertStepAsyncTask(StepDetectedDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(StepDetected... stepDetected) {
            mAsyncTaskDao.insert(stepDetected[0]);
            return null;
        }
    }

    private static class insertLocationAsyncTask extends AsyncTask<GPSLocation, Void, Void> {

        private GPSLocationDao mAsyncTaskDao;

        insertLocationAsyncTask(GPSLocationDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(GPSLocation... location) {
            mAsyncTaskDao.insert(location[0]);
            return null;
        }
    }
    private void autoSessionManagement(){
        if(!mManualMode) {
            boolean autostart = false;
            long curSysTime = System.currentTimeMillis();
            if(mStepIncreasing){ //mIsWalking
                Log.e(TAG, "Is walking");
                Log.i(TAG,"GPS Lost time:" + mGPSLostTimes);
                Log.i(TAG,"GPS stable time:" + mGPSStableTimes);

                if(!AppUtils.requestingLocationUpdates(self)) {
//                    getLastLocation();
                    requestLocationUpdates(false); //slow update
                }
                //TODO: 30 seconds or more to start walking?
                if ((mCurrentLocation != null) &&
                        mCurrentLocation.getProvider().equals(LocationManager.GPS_PROVIDER) &&
                        mGPSLostTimes < 5 && mGPSStableTimes > 2 ){//&&                       //if no GPS signal, it should stop
//                        (mCurrentLocation.getTime() >= mTransitionEnterTime) &&
//                        (mTransitionEnterTime - mTransitionExitTime) > MINUTE2MILLI &&     //real start walking since last stopped
//                        ((curSysTime - mTransitionEnterTime) > (MINUTE2MILLI / 2))) {
                    autostart = true;
                }
                if(curSysTime - mLast20StepTime > 30 * SECOND2MILLI){
                    mStepIncreasing = false;
                    mLast20StepOffset = 0;
                    mLast20StepTime = 0L;
                    autostart = false;
                }
            }
//            else{
//                Log.e(TAG, "Is not walking");
//                if(mTransitionEnterTime > 0 && mTransitionExitTime > 0 &&
//                        ((mTransitionExitTime - mTransitionEnterTime) < MINUTE2MILLI/3 ||
//                        (curSysTime - mTransitionExitTime) < MINUTE2MILLI/3)  &&
//                        mGPSLostTimes < 5 && mGPSStableTimes > 1 ){
//                    autostart = true; //do not stop session if it is not a real stop (20s e.g. traffic light)
//                }
//                if(curSysTime - mLast20StepTime > 30 * SECOND2MILLI){
//                    mStepIncreasing = false;
//                    mLast20StepOffset = 0;
//                    mLast20StepTime = 0L;
//                    autostart = false;
//                }
//            }

            if(autostart){
                Log.e(TAG, "auto running session");
                if (!mSessionStarted){
                    requestLocationUpdates(true);
                    startRecording();
                    mSessionStarted = true;
                }
            }else{
                Log.e(TAG, "auto stop session");
                if (mSessionStarted) {
                    stopRecording("auto");
                    mGPSStableTimes = 0;
                    mSessionStarted = false;
                }
                if(mStepIncreasing){
                    if(AppUtils.getKeyRequestingLocationUpdatesFast(self))
                        //make sure whenever there is walking session, location is watched
                        requestLocationUpdates(false);
                } else {
                    if(AppUtils.requestingLocationUpdates(self)) {
                        removeLocationUpdates();
                        mLast20StepOffset = 0;
                        mLast20StepTime = 0L;                    }
                }

                Log.i(TAG,"Is step increasing:"+ mStepIncreasing);
            }
//        }else{ //When manual mode is on, stop all ongoing sessions??
//            if (mSessionStarted){
//                manualStopSession();
//                updateSessionTag("aborted");
//            }
        }
    }

    private static class dailyreportAsyncTask extends AsyncTask<DailySummary, Void, Void> {

        private DailySummaryDao mAsyncTaskDao;
        private WalkingSessionDao mSessionDao;

        dailyreportAsyncTask(DailySummaryDao dDao, WalkingSessionDao sDao) {
            mAsyncTaskDao = dDao;
            mSessionDao = sDao;
        }

        @Override
        protected Void doInBackground(DailySummary... dailySummaries) {
            DailySummary report = dailySummaries[0];
            Date reportdate = report.mDate;
            long startTime = DateConverter.toTimestamp(reportdate);
            long endTime = DateConverter.toTimestamp(reportdate) + 24*60*60*1000 - 1;

            report.numWalkingSessions = mSessionDao.getNumOfSessions(startTime, endTime);
            report.walkingduration = mSessionDao.getSumDuration(startTime, endTime);
            report.stepDetect = mSessionDao.getSumStepDetect(startTime,endTime);
            report.distance = mSessionDao.getSumDistance(startTime,endTime);
            report.speed = mSessionDao.getAvgSpeed(startTime, endTime);

            mAsyncTaskDao.insert(report);
            return null;
        }
    }

    private static class updateSessionAsyncTask extends AsyncTask<WalkingSession, Void, Void> {

        private WalkingSessionDao mSessionDao;
        private GPSLocationDao mLocationdao;

        updateSessionAsyncTask(WalkingSessionDao sDao, GPSLocationDao lDao) {
            mSessionDao = sDao;
            mLocationdao = lDao;
        }

        @Override
        protected Void doInBackground(WalkingSession... sessions) {
            WalkingSession thisSession = sessions[0];

            //thisSession.mDistance = totalDistance(thisSession.sid);
            //thisSession.mAverageSpeed = avgSpeed(thisSession.sid);

            //mAsyncTaskDao.insert(walkingEvents[0]);
            mSessionDao.update(thisSession); //already calculated
            return null;
        }

        private float totalDistance(long sid){
            //TODO: #######
            float totalD = 0f;
            Location lastLocation = new Location("dummy");
            Location newLocation = new Location("dummy");

            List<GPSLocation> locations = mLocationdao.getSessionLocations(sid);
            for(GPSLocation loc: locations){
                newLocation.setProvider("dummy");
                newLocation.setLatitude(loc.latitude);
                newLocation.setLongitude(loc.longitude);
                newLocation.setAccuracy(loc.accuracy);
                newLocation.setSpeed(loc.speed);
                //newLocation.setElapsedRealtimeNanos(loc.GTimestamp);
                newLocation.setTime(loc.GTimestamp);
                if(lastLocation.getProvider().equals("set")){
                    totalD += newLocation.distanceTo(lastLocation);
                }

                //if(loc.accuracy < GPS_ACCEPTABLE_ACCURACY && loc.speed > 0){
                    lastLocation = newLocation;
                    lastLocation.setProvider("set");
                //}
            }

            return totalD;
        }

        private float avgSpeed(long sid){
            //TODO: #####
            float averageS = 0f;

            averageS = mLocationdao.getSessionSpeed(sid);
            return averageS;
        }
    }

    private static class saveStepAsyncTask extends AsyncTask<HourlySteps, Void, Void> {

        private HourlyStepsDao mAsyncTaskDao;

        saveStepAsyncTask(HourlyStepsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(HourlySteps... steps) {
            mAsyncTaskDao.insert(steps[0]);
            return null;
        }
    }


}
