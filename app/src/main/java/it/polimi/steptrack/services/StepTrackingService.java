package it.polimi.steptrack.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
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
import java.util.List;

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
import static it.polimi.steptrack.AppConstants.GPS_ACCEPTABLE_ACCURACY;
import static it.polimi.steptrack.AppConstants.GPS_FAST_UPDATE_INTERVAL;
import static it.polimi.steptrack.AppConstants.GPS_ACCURACY_FOR_SUM;
import static it.polimi.steptrack.AppConstants.GPS_ACCURACY_THRESHOLD;
import static it.polimi.steptrack.AppConstants.NETWORK_UPDATE_INTERVAL;
import static it.polimi.steptrack.AppConstants.OUT_OF_HOME_DISTANCE;
import static it.polimi.steptrack.AppConstants.SECOND2MILLI;
import static it.polimi.steptrack.AppConstants.SECOND2NANO;
import static it.polimi.steptrack.AppConstants.SERVICE_RUNNING_FOREGROUND;
import static it.polimi.steptrack.AppConstants.STEP_SAVE_INTERVAL;
import static it.polimi.steptrack.AppConstants.STEP_SAVE_OFFSET;
//import static it.polimi.steptrack.AppConstants.UPDATE_DISTANCE_IN_METERS;


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


//    private long mTransitionEnterTime = -1L;
//    private long mTransitionExitTime = -1L;
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
    private Location mHomeLocation;
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
    private int mStepsAtStart = 0;
    private int mStepsDetect = 0;
    private int mStepCountOffset = 0;
    private long mStepIncreaseStartTime = 0L;
//    private int mLast20StepOffset = 0;
    private class StepsCounted{
        private int stepsOffset;
        private long timestamp;

        public StepsCounted(int numSteps, long timestamp) {
            this.stepsOffset = numSteps;
            this.timestamp = timestamp;
        }

        public int getStepsOffset() {
            return stepsOffset;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
    ArrayList<StepsCounted> arrSteps = new ArrayList<>();

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
        mHomeLocation =  AppUtils.getPrefPlaceLocation(self);

        // Define a listener that responds to location updates
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the location provider.
                onNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i(TAG,"Location status changed.");
                /* This is called when the GPS status alters */
                switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    Log.v(TAG, "Status Changed: Out of Service");
                    if(provider.equals(LocationManager.GPS_PROVIDER)) {
                        mGPSStableTimes = 0;
                        mGPSLostTimes++;
                    }
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.v(TAG, "Status Changed: Temporarily Unavailable");
                    if(provider.equals(LocationManager.GPS_PROVIDER)) {
                        mGPSLostTimes++;
                        if(mGPSStableTimes > 0){
                            if(mGPSStableTimes > 5) mGPSStableTimes = 5;
                            mGPSStableTimes--;
                        }
                    }
                    break;
                case LocationProvider.AVAILABLE:
                    Log.v(TAG, "Status Changed: Available");
                    if(provider.equals(LocationManager.GPS_PROVIDER)) {
                        mGPSLostTimes = 0;
                    }
                    break;
                }
                autoSessionManagement();
            }

            public void onProviderEnabled(String provider) {
                Log.i(TAG,"Provider Enabled." + provider);
            }

            public void onProviderDisabled(String provider) {
                if(provider.equals(LocationManager.GPS_PROVIDER)) {
                    mGPSLostTimes++;
                    mGPSStableTimes = 0;
                    autoSessionManagement();
                }
                Log.i(TAG,"Provider Disabled." + provider);
            }
        };

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
            mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);//, BATCH_LATENCY_5s);
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
        mTotalStepsCount = AppUtils.getLastStepCount(self);
        mStepCountOffset = AppUtils.getStepCountOffset(self);
        if (AppUtils.getKeyPhoneReboot(self)) { //if the app doesn't auto start
            int hourlyStepsOffset = AppUtils.getRecordSteps(self) - (mStepCountOffset + mTotalStepsCount);
            AppUtils.setKeyRecordSteps(self,hourlyStepsOffset);
            mStepCountOffset = 0 - mTotalStepsCount; //!!!
            AppUtils.setStepCountOffset(self, mStepCountOffset);

            AppUtils.setKeyPhoneReboot(self,false);
        }

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

    public boolean requestLocationUpdates(boolean fastUpdate) {
        boolean isSuccess;
        Log.i(TAG, "Requesting location updates:" + fastUpdate);
        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            isSuccess = false;
        }else {
            if (mLocationManager != null && mLocationManager != null &&
                    AppUtils.requestingLocationUpdates(self)) {
                mLocationManager.removeUpdates(mLocationListener);
                AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
                AppUtils.setRequestingLocationUpdates(this, false);
            }
//            if(fastUpdate) {
                try {
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            NETWORK_UPDATE_INTERVAL, 0, mLocationListener);
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            GPS_FAST_UPDATE_INTERVAL, 0, mLocationListener);//, Looper.getMainLooper());
                    AppUtils.setKeyRequestingLocationUpdatesFast(this, true);
                    AppUtils.setRequestingLocationUpdates(this, true);
                    isSuccess = true;
                }catch (Exception e){
                    isSuccess = false;
                    AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
                    AppUtils.setRequestingLocationUpdates(this, false);
                    Log.e(TAG, "Location could not request updates. " + e);
                    updateNotification("Location could not request updates. " + e);
                }
//            }else {
//                try {
//                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
//                            NETWORK_UPDATE_INTERVAL, 0, mLocationListener);
//                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
//                            GPS_UPDATE_INTERVAL, 0, mLocationListener);
//                    AppUtils.setRequestingLocationUpdates(this, true);
//                    AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
//                    isSuccess = true;
//                }catch (Exception e){
//                    isSuccess = false;
//                    AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
//                    AppUtils.setRequestingLocationUpdates(this, false);
//                    Log.e(TAG, "Location could not request updates. " + e);
//                    updateNotification("Location could not request updates. " + e);
//                }
//            }
        }
        return isSuccess;
    }

    private void startRecording(){
        mWalkingSession = new WalkingSession();
        mWalkingSession.mStartTime = System.currentTimeMillis();
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
                try {
                    outputStream = new FileOutputStream(mSessionFile);
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
                Log.w(TAG, "some sensor not compatible!");
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
    }


    public boolean removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");

        mLocationManager.removeUpdates(mLocationListener);
        AppUtils.setRequestingLocationUpdates(this, false);
        AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
        return true;
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
            if(mWalkingSession.mDuration > 0)
                mWalkingSession.mAverageSpeed = mTotalDistance/mWalkingSession.mDuration;
            else
                mWalkingSession.mAverageSpeed = 0;

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
        mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);//, BATCH_LATENCY_5s);
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
                if(mWalkingSession.mDuration > 0)
                    mWalkingSession.mAverageSpeed = mTotalDistance/mWalkingSession.mDuration;
                else
                    mWalkingSession.mAverageSpeed = 0;

                mStepsDetect = 0;
            }
            mSensorManager.unregisterListener(this); //unregiester motion sensors
            mCurSensorTime = -1L;
            mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);//,BATCH_LATENCY_5s);
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
                    .setShowWhen(false)
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

    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);

        GPSLocation gpsLocation = new GPSLocation();
        gpsLocation.GTimestamp = location.getTime();
        gpsLocation.latitude = location.getLatitude();
        gpsLocation.longitude = location.getLongitude();
        gpsLocation.accuracy = location.getAccuracy();
        gpsLocation.provider = location.getProvider();
        if (location.hasSpeed()){
            gpsLocation.speed = location.getSpeed();
        }else {
            gpsLocation.speed = -1;
        }
        if(location.hasBearing()){
            gpsLocation.bearing = location.getBearing();
        }else {
            gpsLocation.bearing = -1;
        }

        gpsLocation.isWalking = mStepIncreasing;//mIsWalking;
        gpsLocation.session_id = mWalkingSessionId;
        new insertLocationAsyncTask(mDB.locationDao()).execute(gpsLocation);

        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)){
            mCurrentLocation = location;
            mGPSLostTimes = 0;
            if (mCurrentLocation.hasSpeed() &&
                    mCurrentLocation.getSpeed() > 0 &&
                    mCurrentLocation.getAccuracy() < GPS_ACCEPTABLE_ACCURACY){ //!!! GPS_ACCEPTABLE_ACCURACY
                Log.i(TAG, "GPS stable +1");
                mGPSStableTimes++;
            }
            if ((mCurrentLocation.getAccuracy() > GPS_ACCURACY_THRESHOLD) ||
                (mLastLocation!=null && mCurrentLocation.getAccuracy() > 2 * mLastLocation.getAccuracy())){
                if (mGPSStableTimes > 0){
                    if(mGPSStableTimes > 5) mGPSStableTimes = 5;
                    mGPSStableTimes--;
                }
                Log.i(TAG,"GPS stable -1");
            }
            if(!mCurrentLocation.hasBearing() && !mCurrentLocation.hasSpeed()) {
                mGPSLostTimes++;
                mGPSStableTimes=0;
            }

            if (mCurrentLocation.distanceTo(mHomeLocation) -
                    mCurrentLocation.getAccuracy() > OUT_OF_HOME_DISTANCE){
                mOutofHome = true;
            }
            else {
                mOutofHome = false;
            }

            if (mSessionStarted && mWalkingSessionId != -1 ) {
                if(mLastLocation == null){
                    mLastLocation = mCurrentLocation;
                }else{
//                    if (mCurrentLocation.getTime() - mLastLocation.getTime() >= 30 * SECOND2MILLI){
//                        mGPSLostTimes++;
//                        mGPSStableTimes = 0;
//                    }else {//if location accuracy is always lower than 10 m, session never start
                        //TODO: improve algorithm ####
                        if (mCurrentLocation.getAccuracy() <= GPS_ACCURACY_FOR_SUM &&
                                mCurrentLocation.distanceTo(mLastLocation) >= mCurrentLocation.getAccuracy() && //GPS_DISTANCE_THRESHOLD_FOR_SUM)
                                mCurrentLocation.getSpeed() > 0 &&
                                mStepIncreasing) {

                            mTotalDistance += mCurrentLocation.distanceTo(mLastLocation);
                            mLastLocation = mCurrentLocation;
                        }
//                    }
                }
            }

        }else {
            if (mCurrentLocation != null &&
                    (location.getTime() - mCurrentLocation.getTime() >= 30 * SECOND2MILLI)) {
                mGPSLostTimes++;
                mGPSStableTimes = 0;
            }
        }

        autoSessionManagement();//check if should start automatically; !!need to update in both situation
    }

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
            if(mStepCountOffset==Integer.MIN_VALUE){
                mStepCountOffset = systemSteps;
                AppUtils.setStepCountOffset(self,mStepCountOffset);
            }

            //for daily report
//            int lastReportStep = AppUtils.getLastStepCount(self);
//            long lastReportTime = AppUtils.getLastReportTime(self);
//            if (lastReportTime == 0) lastReportTime = mCurSensorTime;

            //for hourly record
            long lastRecordTime = AppUtils.getLastRecordTime(self);
            int hourlyStepsOffset = AppUtils.getRecordSteps(self);
            if (hourlyStepsOffset == 0){
                hourlyStepsOffset = mStepCountOffset;
            }
            if(hourlyStepsOffset > systemSteps){
                hourlyStepsOffset = systemSteps;
            }

            if (!arrSteps.isEmpty()){
                long lastStepsTime = arrSteps.get(arrSteps.size()-1).getTimestamp();
                if (mCurSensorTime - lastStepsTime > 10 * SECOND2NANO){ //10 second without step increasing
                    if (mStepIncreasing){
                        WalkingEvent walkingEvent = new WalkingEvent();
                        walkingEvent.WeTimestamp = System.currentTimeMillis();
                        walkingEvent.mTransition = 0;
                        walkingEvent.mElapsedTime = lastStepsTime;
                        new insertEventAsyncTask(mDB.walkingEventDao()).execute(walkingEvent);
                    }
                    mStepIncreasing = false;
                    arrSteps.clear();
                }else {
                    int stepSum = systemSteps - arrSteps.get(0).getStepsOffset();
                    long stepDur = mCurSensorTime - arrSteps.get(0).getTimestamp();
//                    Log.i(TAG,"timeDiff: " + stepDur);
//                    Log.i(TAG,"stepDiff: " + stepSum);
//                    Log.i(TAG, "array size: " + arrSteps.size());
                    if(stepSum >= 15 && stepDur > 0) { //15 steps as detected walking
                        double ratio = stepDur / stepSum;
                        if ( ratio > 400 && ratio < 900 ) {
                            if (!mStepIncreasing){
                                WalkingEvent walkingEvent = new WalkingEvent();
                                walkingEvent.WeTimestamp = System.currentTimeMillis();
                                walkingEvent.mTransition = 1;
                                walkingEvent.mElapsedTime = arrSteps.get(0).getTimestamp(); //get the timestamp for the first step
                                new insertEventAsyncTask(mDB.walkingEventDao()).execute(walkingEvent);
                            }
                            mStepIncreasing = true;
                        }
                        arrSteps.remove(0); //always remove the first one
                    }
                }
            }else {
                mStepIncreasing = false;
            }
            arrSteps.add(new StepsCounted(systemSteps,mCurSensorTime));

            if (!mSessionStarted) {
                //only record the hourly steps when there is no ongoing walking session;
                if ((mCurSensorTime > lastRecordTime + STEP_SAVE_INTERVAL) ||
                        (systemSteps - hourlyStepsOffset > STEP_SAVE_OFFSET)) {

                    dailyreportAsyncTask dailyreportTask = new dailyreportAsyncTask(mDB);
                    if (AppUtils.isToday(lastRecordTime)) {
                        dailyreportTask.execute(0);
                    } else {
                        dailyreportTask.execute(systemSteps - hourlyStepsOffset);
                    }
                    dailyreportTask.setOnReportFinishedListener(new OnReportFinishedListener() {
                        @Override
                        public void onReportSuccess(Integer stepsOffset) {
                            mStepCountOffset += stepsOffset; //this is not synchronized and followed by the code outside this listener!!!
                            mTotalStepsCount -= stepsOffset;
                            AppUtils.setStepCountOffset(self, mStepCountOffset);
                            AppUtils.setKeyLastStepCount(self, mTotalStepsCount);
                            Log.i(TAG, "Daily report generated");
                        }

                        @Override
                        public void onReportFailed() {
                            Log.i(TAG, "No Daily report");
                        }
                    });

                    HourlySteps hourlySteps = new HourlySteps();
                    hourlySteps.steps = systemSteps - hourlyStepsOffset;
                    hourlySteps.timestamp = mCurSensorTime;
                    new saveStepAsyncTask(mDB.hourlyStepsDao()).execute(hourlySteps);

                    hourlyStepsOffset = systemSteps;
                    AppUtils.setKeyLastRecordTime(self, mCurSensorTime);
                    AppUtils.setKeyRecordSteps(self, hourlyStepsOffset);
                }
            }

            if(systemSteps > mStepCountOffset) {
                mTotalStepsCount = systemSteps - mStepCountOffset;
            }else {
                mStepCountOffset = systemSteps;
                AppUtils.setStepCountOffset(self,mStepCountOffset);
                mTotalStepsCount = 0;
            }

            AppUtils.setKeyLastStepCount(self, mTotalStepsCount);
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
            mHomeLocation =  AppUtils.getPrefPlaceLocation(self);
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
                    updateNotification("Walking detected. Searching for GPS...");
                }
                //TODO: 30 seconds or more to start walking?
                if (mOutofHome &&
                        (mCurrentLocation != null) &&
                        (System.currentTimeMillis() - mCurrentLocation.getTime() < 30 * SECOND2MILLI) &&
                        mGPSLostTimes < 4 && mGPSStableTimes > 2 ){//&&                       //if no GPS signal, it should stop

                    autostart = true;
                }
                if (!arrSteps.isEmpty() &&
                        (curSysTime - arrSteps.get(arrSteps.size()-1).getTimestamp() > 30 * SECOND2MILLI)){ //30 second without step increasing
                    if (mStepIncreasing = true){
                        WalkingEvent walkingEvent = new WalkingEvent();
                        walkingEvent.WeTimestamp = System.currentTimeMillis();
                        walkingEvent.mTransition = 0;
                        walkingEvent.mElapsedTime = arrSteps.get(arrSteps.size()-1).getTimestamp();
                        new insertEventAsyncTask(mDB.walkingEventDao()).execute(walkingEvent);
                    }

                    mStepIncreasing = false;
                    arrSteps.clear();
                    autostart = false;
                }
            }


            if(autostart){
                Log.e(TAG, "auto running session");
                if (!mSessionStarted){
//                    requestLocationUpdates(true);
                    startRecording();
                    mSessionStarted = true;
                }
            }else{
                Log.e(TAG, "auto stop session");
                if (mSessionStarted) {
                    stopRecording("auto");
                    mSessionStarted = false;
                    mGPSLostTimes = 0;
                    mGPSStableTimes = 0;
                    updateNotification("Session record stopped");
                }
                if(mStepIncreasing){
                    if(AppUtils.getKeyRequestingLocationUpdatesFast(self)) {
                        //make sure whenever there is walking session, location is watched
//                        requestLocationUpdates(false);
                        updateNotification("Walking detected. Searching for GPS...");
                    }
                } else {
                    if(AppUtils.requestingLocationUpdates(self)) {
                        removeLocationUpdates();
                        arrSteps.clear();
                        updateNotification("Counting steps");
                    }
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


    public interface OnReportFinishedListener {
        void onReportSuccess(Integer stepsOffset);
        void onReportFailed();
    }

    private static class dailyreportAsyncTask extends AsyncTask<Integer, Void, Integer> {

        private DailySummaryDao mDailySummaryDao;
        private WalkingSessionDao mSessionDao;
        private HourlyStepsDao mHourlyStepDao;

        OnReportFinishedListener onReportFinishedListener;
        public void setOnReportFinishedListener(
                OnReportFinishedListener onReportFinishedListener) {
            this.onReportFinishedListener = onReportFinishedListener;
        }

        dailyreportAsyncTask(AppDatabase appDB){//DailySummaryDao dDao, WalkingSessionDao sDao, HourlyStepsDao hDao) {
            mDailySummaryDao = appDB.dailySummaryDao();
            mSessionDao = appDB.sessionDao();
            mHourlyStepDao = appDB.hourlyStepsDao();
        }

        @Override
        protected Integer doInBackground(Integer... params) {

            int lastHoursteps = params[0];
            long startTime = AppUtils.getYesterdayStart();
            long endTime = AppUtils.getYesterdayEnd();
            DailySummary lastReport = mDailySummaryDao.getLastReport();
            if(lastReport == null ||
                    (lastReport!=null && lastReport.reportTime < startTime)){
                int dailysteps = mHourlyStepDao.getDailyStep(startTime, endTime);
                if (lastHoursteps > 0){
                    dailysteps += lastHoursteps;
                }
                if (dailysteps > 0) {
                    DailySummary report = new DailySummary();
                    report.mDate = DateConverter.toDate(endTime);
                    report.reportTime = endTime;

                    report.numWalkingSessions = mSessionDao.getNumOfSessions(startTime, endTime);
                    report.walkingduration = mSessionDao.getSumDuration(startTime, endTime);
                    report.stepDetect = mSessionDao.getSumStepDetect(startTime, endTime);
                    report.distance = mSessionDao.getSumDistance(startTime, endTime);
                    report.speed = mSessionDao.getAvgSpeed(startTime, endTime);

                    report.steps = dailysteps;

                    mDailySummaryDao.insert(report);

                }
                return dailysteps;
            }else {
                return 0;
            }

//            int yesterdayReport = mDailySummaryDao.getNumSummariesBetween(startTime,endTime);
//            int dailysteps = mHourlyStepDao.getDailyStep(startTime, endTime);
//            if (lastHoursteps > 0){
//                dailysteps += lastHoursteps;
//            }
//            if (yesterdayReport <= 0) {
//            }
//
//            return dailysteps;
        }

        @Override
        protected void onPostExecute(Integer dailysteps) {
            super.onPostExecute(dailysteps);
            if (dailysteps > 0){
                onReportFinishedListener.onReportSuccess(dailysteps);
            }else {
                onReportFinishedListener.onReportFailed();
            }
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

    private static class insertEventAsyncTask extends AsyncTask<WalkingEvent, Void, Void> {

        private WalkingEventDao mAsyncTaskDao;

        insertEventAsyncTask(WalkingEventDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(WalkingEvent... walkingEvents) {
            mAsyncTaskDao.insert(walkingEvents[0]);
            return null;
        }
    }


}
