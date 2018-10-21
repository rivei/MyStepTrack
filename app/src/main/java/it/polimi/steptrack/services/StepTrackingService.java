package it.polimi.steptrack.services;

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
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.polimi.steptrack.AppConstants;
import it.polimi.steptrack.AppUtils;
import it.polimi.steptrack.R;
import it.polimi.steptrack.roomdatabase.AppDatabase;
import it.polimi.steptrack.roomdatabase.dao.WalkingEventDao;
import it.polimi.steptrack.roomdatabase.entities.GPSLocation;
import it.polimi.steptrack.roomdatabase.entities.WalkingEvent;
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;
import it.polimi.steptrack.ui.MainActivity;

import static it.polimi.steptrack.AppConstants.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS;
import static it.polimi.steptrack.AppConstants.MILLI2NANO;
import static it.polimi.steptrack.AppConstants.MINUTE2NANO;
import static it.polimi.steptrack.AppConstants.SECOND2NANO;
import static it.polimi.steptrack.AppConstants.SERVICE_RUNNING_FOREGROUND;
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

    private long mTransitionEnterTime = -1L;
    private long mTransitionExitTime = -1L;
    private boolean mOutofHome = false; //radius greater than threshold.
    private boolean mManualMode = false; //when manual mode is true, walking session doesn't depends on other things;
    private int mGPSLostTimes = 0;
    private int mGPSStableTimes = 0;

    /**
     * For App notification
     */
    private static final String CHANNEL_ID = "channel_01"; //notification channel name

    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();

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


    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderClient}.
     */
    private LocationRequest mLocationFastRequest;
    private LocationRequest mLocationSlowRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;

    private Handler mServiceHandler;

    /**
     * The current location.
     */
    private Location mCurrentLocation;
    private LatLng mHomeCoordinate = null;


    /**
     * For activity detection
     */
    private PendingIntent mPendingIntent;
    private TransitionsReceiver mTransitionsReceiver;



    private SensorManager mSensorManager;
    private Sensor countSensor = null;
    private Sensor stepDetectSensor = null;
    private Sensor accSensor = null;
    private Sensor gyroSensor = null;
    private Sensor magnSensor = null;

    float[] accelerometerMatrix = new float[3];
    float[] gyroscopeMatrix = new float[3];
    float[] magneticMatrix = new float[3];

    private long sensorTimeRef1 = -1L;
    private long sensorTimeRef2 = -1L;
    private long sysTimeRef1 = -1L;
    private long sysTimeRef2 = -1L;
    private long startoffset = -1L;
    private long rateoffset = -1L;
    private long curTime = -1L;
    private long lastUpdate = -1L;

    //TODO: for database:
    private AppDatabase mDB;
    //private DataRepository mAccRepo;
    //private AccelerometerSample mAccSample;
    //private GyroscopeSample mGyroSample;
    private GPSLocation mGPSLocation;
    private WalkingSession mWalkingSession;
    private long mWalkingSessionId = -1;
    private int mStepsCount = 0;
    private int stepsCountS = 0;
    private int mStepsDetect = 0;

    private File mStepTrackDir = null;
    private File mSessionFile = null;
    private final String sessionHeader = "timestampNANO,acc_X,acc_Y,acc_Z,gyro_X,gyro_Y,gyro_Z," +
                                        "magn_X,magn_Y,magn_Z,latitude,longitude,accuracy,speed \n";


    public StepTrackingService() { //Empty constructor
    }

    @Override
    public void onCreate() {
        Log.w(TAG, "On Creating");
        /**
         * setup fused location client and call back
         */
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        initLocationRequest(); //for location request when walking is detected.
        //getLastLocation(); //This is just a fuse location without high accuracy

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
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
            mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Step count sensor missing. Device not Compatible!", Toast.LENGTH_LONG).show();
            this.stopSelf();
        }

        //************************* For step count *********************//
        // Setup Step Counter
        mStepsCount = AppUtils.getLastStepCount(this) - AppUtils.getStepCountOffset(this);

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
         * Activity Detection
          */
        Intent intent = new Intent(AppConstants.TRANSITIONS_RECEIVER_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        mTransitionsReceiver = new TransitionsReceiver();
        registerReceiver(mTransitionsReceiver, new IntentFilter(AppConstants.TRANSITIONS_RECEIVER_ACTION));

        //TODO: db
        mDB = AppDatabase.getInstance(this);

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
        if (!exportDir.exists()){
            exportDir.mkdirs();
        }
    }

    //This is called only when there is Start service
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Foreground service started");

        //************************* For step count ~*********************//
        //TODO: start Monitoring activities and locations
        setupActivityTransitions();

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


        // Unregister the transitions: (TODO: in Activity.onPause)
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mPendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Transitions successfully unregistered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Transitions could not be unregistered: " + e);
                    }
                });
        //TODO: Activity.onStop
        if (mTransitionsReceiver != null) {
            unregisterReceiver(mTransitionsReceiver);
            mTransitionsReceiver = null;
        }

        mServiceHandler.removeCallbacksAndMessages(null);

        PreferenceManager.getDefaultSharedPreferences(self)
                .unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */

    public boolean requestLocationUpdates(boolean fastUpdate) {
        boolean isSuccess;
        Log.i(TAG, "Requesting location updates:" + fastUpdate);
        AppUtils.setRequestingLocationUpdates(this, true);
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            if(fastUpdate) {
                mFusedLocationClient.requestLocationUpdates(mLocationFastRequest,
                        mLocationCallback, Looper.myLooper());
                AppUtils.setKeyRequestingLocationUpdatesFast(this, true);
            }else {
                mFusedLocationClient.requestLocationUpdates(mLocationSlowRequest,
                        mLocationCallback, Looper.myLooper());
                AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
            }

            isSuccess = true;
        } catch (SecurityException unlikely) {
            AppUtils.setRequestingLocationUpdates(this, false);
            AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
            isSuccess = false;
        }
        return isSuccess;
    }

    private void startRecording(){
        mWalkingSession = new WalkingSession();
        mWalkingSession.mStartTime = System.currentTimeMillis();
        mWalkingSession.mStartNano = SystemClock.elapsedRealtimeNanos();
        stepsCountS = mStepsCount;

        Thread t = new Thread() {
            public void run() {
                mWalkingSessionId = mDB.sessionDao().insert(mWalkingSession);
                mWalkingSession.sid = mWalkingSessionId;
                /**
                 * create walking session file
                 */
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
                String sessionInfo = "StartTimestamp," + mWalkingSession.mStartTime +
                        ",StartInNano," + mWalkingSession.mStartNano + "\n";
                try {
                    outputStream = new FileOutputStream(mSessionFile);
                    outputStream.write(sessionInfo.getBytes());
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
        if (registerSensorListener()) {
            Log.e(TAG, "Start recording");
            Toast.makeText(this, "Starts session", Toast.LENGTH_LONG).show();
            if (AppUtils.getServiceRunningStatus(this) == SERVICE_RUNNING_FOREGROUND){
                updateNotification("Recording session");
            }
        } else {
            Toast.makeText(this, "some sensor not Compatible!", Toast.LENGTH_LONG).show();
            this.stopSelf();
        }
    }

    public void requestNewWalkingSession() {
        Log.i(TAG, "Requesting new walking session");
        AppUtils.setKeyStartingWalkingSession(this, mSessionStarted);
        if(requestLocationUpdates(true)){
            mSessionStarted = true;
            startRecording();
/*
            mSessionStarted = true;
            mWalkingSession = new WalkingSession();
            mWalkingSession.mStartTime = System.currentTimeMillis();
            mWalkingSession.mStartNano = SystemClock.elapsedRealtimeNanos();
            stepsCountS = mStepsCount;

            Thread t = new Thread() {
                public void run() {
                    mWalkingSessionId = mDB.sessionDao().insert(mWalkingSession);
                    mWalkingSession.sid = mWalkingSessionId;
                    */
/**
                     * create walking session file
                     *//*

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
                    String sessionInfo = "StartTimestamp," + mWalkingSession.mStartTime +
                            ",StartInNano," + mWalkingSession.mStartNano;
                    try {
                        outputStream = new FileOutputStream(mSessionFile);
                        outputStream.write(sessionInfo.getBytes());
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
*/

/*            if (registerSensorListener()) {
                Toast.makeText(this, "Starts session ", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "some sensor not Compatible!", Toast.LENGTH_LONG).show();
                this.stopSelf();
            }*/
        }else {
            mSessionStarted = false;
        }
        AppUtils.setKeyStartingWalkingSession(self,mSessionStarted);
/*        try {
            mFusedLocationClient.requestLocationUpdates(mLocationFastRequest,
                    mLocationCallback, Looper.myLooper());
            //TODO: locationManager.requestNewWalkingSession(LocationManager.GPS_PROVIDER, 5000, 10, new MyLocationListener());
            //Add sensor listener:
        } catch (SecurityException unlikely) {
            mSessionStarted = false;
            //AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
            AppUtils.setKeyStartingWalkingSession(this, mSessionStarted);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }*/
    }

    private boolean registerSensorListener(){
        boolean isSuccess = false;
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
        if(numofSensors >= 4){
            isSuccess = true;
        }
        return isSuccess;
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public boolean removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        boolean isSuccess;
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            AppUtils.setRequestingLocationUpdates(this, false);
            AppUtils.setKeyRequestingLocationUpdatesFast(this, false);
            isSuccess = true;
        } catch (SecurityException unlikely) {
            AppUtils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
            isSuccess = false;
        }
        return isSuccess;
    }


    private void stopRecording(String inputTag){
        mSessionFile = null;
        if (mWalkingSessionId != -1) {
            mWalkingSession.mEndNano = SystemClock.elapsedRealtimeNanos();
            mWalkingSession.mEndTime = System.currentTimeMillis();
            mWalkingSession.mDuration = mWalkingSession.mEndTime - mWalkingSession.mStartTime;
            //SharedPreferences prefs = this.getSharedPreferences(PREFS_NAME, 0);
            mWalkingSession.mStepCount = mStepsCount - stepsCountS;
            mWalkingSession.mStepDetect = mStepsDetect;
            mStepsDetect = 0;
            mWalkingSession.mTag = inputTag;

            Thread t = new Thread() {
                public void run() {
                    mDB.sessionDao().update(mWalkingSession);
                }
            };
            t.start();
            mWalkingSessionId = -1;
        }
        mSensorManager.unregisterListener(this); //unregiester motion sensors
        //Never stop step counter sensor listening
        mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        mNotificationContentText = "Session not recording";
        Log.e(TAG, "Stop recording.");
    }

    public void manualEndWalkingSession(String inputTag) {
        Log.i(TAG, "Manually stopping sensor session.");
        clearSensorOffset();
        if(removeLocationUpdates()){
            mSessionStarted = false;
            stopRecording(inputTag);
/*
            if (mWalkingSessionId != -1) {
                mWalkingSession.mEndNano = SystemClock.elapsedRealtimeNanos();
                mWalkingSession.mEndTime = System.currentTimeMillis();
                mWalkingSession.mDuration = mWalkingSession.mEndTime - mWalkingSession.mStartTime;
                //SharedPreferences prefs = this.getSharedPreferences(PREFS_NAME, 0);
                mWalkingSession.mStepCount = mStepsCount - stepsCountS;
                mWalkingSession.mStepDetect = mStepsDetect;
                mStepsDetect = 0;
                mWalkingSession.mTag = inputTag;

                Thread t = new Thread() {
                    public void run() {
                        mDB.sessionDao().update(mWalkingSession);
                    }
                };
                t.start();
                mWalkingSessionId = -1;
            }
            mSensorManager.unregisterListener(this); //unregiester motion sensors
            //Never stop step counter sensor listening
            mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
*/
        }else {
            mSessionStarted = true;
        }
        AppUtils.setKeyStartingWalkingSession(self,mSessionStarted);
/*
        try {
            mSessionStarted = false;
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            //AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
            AppUtils.setKeyStartingWalkingSession(this, mSessionStarted);
            //stopSelf(); //TODO: move this to the whole service
            mSessionFile = null; //TODO: **** check if this works

        } catch (SecurityException unlikely) {
            mSessionStarted = true;
            //AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
            AppUtils.setKeyStartingWalkingSession(this,mSessionStarted);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
*/
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification(boolean isFirstTime) {
        String msg = "Step Counter - Counting";
        if (mNotificationContentText != null) msg = mNotificationContentText;
        if (isFirstTime) {
            // PendingIntent to launch activity.
            PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, MainActivity.class), 0);
            mNotificationBuilder.setContentIntent(activityPendingIntent)
                    .setContentText(msg)
                    .setContentTitle(AppUtils.getLocationTitle(this))
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH)

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
        mNotificationBuilder.setContentTitle(mStepsCount + " steps taken");
        if (mSessionStarted) {
            //mNotificationBuilder.setContentText(String.format("X:%f Y:%f Z%f", mAccSample.mAccX, mAccSample.mAccY, mAccSample.mAccZ));
        } else {
            mNotificationBuilder.setContentText(msg);
        }

        return mNotificationBuilder.build();
    }

    private void getLastLocation() {
        Log.w(TAG, "getting last location from onCreate");
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mCurrentLocation = task.getResult();

                            //Log.w(TAG, "The inital location: " + mCurrentLocation);
                        } else {
                            Log.w(TAG, "Failed to get location.");
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);

        mCurrentLocation = location;
        if (location.hasSpeed()) {
            float speed = location.getSpeed();
            if(speed == 0) {
                mGPSLostTimes++;
                //if(mGPSStableTimes > 0) mGPSStableTimes--; //do not terminate immediately, no need because lost signal can control?
            }
            else {
                mGPSLostTimes = 0;
            }
            if (location.getAccuracy() < 30) //TODO: define GPS accuracy radius
                mGPSStableTimes++;

        }else {
            mGPSLostTimes++;
            //if(mGPSStableTimes > 0) mGPSStableTimes--; //do not terminate immediately
        }
        if(location.getAccuracy() > 50) //TODO: define GPS accuracy radius
            mGPSLostTimes++;

        autoSessionManagement();//check if should start automatically;

        if (mSessionStarted && mWalkingSessionId != -1 ) {
            mGPSLocation = new GPSLocation();
            mGPSLocation.GTimestamp = location.getElapsedRealtimeNanos();//location.getTime();
            mGPSLocation.latitude = location.getLatitude();
            mGPSLocation.longitude = location.getLongitude();
            mGPSLocation.provider = location.getProvider();
            mGPSLocation.accuracy = location.getAccuracy();
            if (location.hasSpeed()){
                mGPSLocation.speed = location.getSpeed();
            }else {
                mGPSLocation.speed = -1;
            }
            if(location.hasBearing()){
                mGPSLocation.bearing = location.getBearing();
            }else {
                mGPSLocation.bearing = -1;
            }

            Log.i(TAG,"GPS Lost time:" + mGPSLostTimes);
            Log.i(TAG,"GPS stable time:" + mGPSStableTimes);

            mGPSLocation.isWalking = mIsWalking;
            mGPSLocation.session_id = mWalkingSessionId;
            new Thread(() -> {
                mDB.locationDao().insert(mGPSLocation);
                Log.w(TAG, "write GPS");
            }).start();
        }
//        // Notify anyone listening for broadcasts about the new location.
//        Intent intent = new Intent(ACTION_BROADCAST);
//        intent.putExtra(EXTRA_LOCATION, location);
//        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        //TODO-NOTE: not location Update notification content if running as a foreground service.
//        if (AppUtils.getServiceRunningStatus(this) == SERVICE_RUNNING_FOREGROUND) {
//            mNotificationManager.notify(NOTIFICATION_ID, getNotification(true));
//        }

    }

    /**
     * TODO-NOTE: Sets the location request parameters.update interval and accuracy
     */
    private void initLocationRequest() {
        mLocationSlowRequest = new LocationRequest();
        mLocationSlowRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS * 6) //TODO: for fast testing;
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //indoor in 1 minute (60s)

        mLocationFastRequest = new LocationRequest();
        mLocationFastRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
                .setFastestInterval(1) //TODO: update as fast as possible
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (mWalkingSessionId != -1 && mSessionFile.exists()) {
            FileOutputStream outputStream;
            String dataLine = "";
            //long systime = SystemClock.elapsedRealtimeNanos();//System.currentTimeMillis();
            curTime = sensorEvent.timestamp;
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_STEP_DETECTOR:
                    mStepsDetect += 1;//sensorEvent.values[0];//+= sensorEvent.values.length;
                    dataLine = curTime + "," + mStepsDetect +"\n";
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

            //TODO: set sensor update frequency in ms
            ////This can't be set below 10ms due to Android/hardware limitations. Use 9 to get more accurate 10ms intervals
            if ((curTime - lastUpdate) >= 10 * MILLI2NANO) {
                lastUpdate = curTime;
                if(mCurrentLocation != null && (mCurrentLocation.getElapsedRealtimeNanos()/SECOND2NANO) == (curTime/SECOND2NANO)){//only log when there is location
                    dataLine = curTime + "," +
                            accelerometerMatrix[0] + "," + accelerometerMatrix[1] + "," + accelerometerMatrix[2] + "," +
                            gyroscopeMatrix[0] + ", " + gyroscopeMatrix[1] + ", " + gyroscopeMatrix[2] + ", " +
                            magneticMatrix[0] + "," + magneticMatrix[1] + "," + magneticMatrix[2] + "," +
                            mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude() + "," +
                            mCurrentLocation.getAccuracy() + "," + mCurrentLocation.getSpeed() + "\n";
                }else {
                    dataLine = curTime + "," +
                            accelerometerMatrix[0] + "," + accelerometerMatrix[1] + "," + accelerometerMatrix[2] + "," +
                            gyroscopeMatrix[0] + ", " + gyroscopeMatrix[1] + ", " + gyroscopeMatrix[2] + ", " +
                            magneticMatrix[0] + "," + magneticMatrix[1] + "," + magneticMatrix[2] + "," + ",,,\n";
                }
                try {
                    //Log.w(TAG,"Thread starts");
                    outputStream = new FileOutputStream(mSessionFile, true);
                    outputStream.write(dataLine.getBytes());
                    //Log.i(TAG, "write sensor data " + curTime);
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "write sensor data failed");
                }
            }
        }

        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER){
            // Record Step Count
            mStepsCount = (int)sensorEvent.values[0] - AppUtils.getStepCountOffset(this);
            AppUtils.setKeyLastStepCount(this, mStepsCount);
            Log.i(TAG, "steps: " + mStepsCount);
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
        if(key.equals(AppUtils.KEY_REQUESTING_LOCATION_UPDATES)){

        }
        if(key.equals(AppUtils.KEY_PLACE_LAT) || key.equals(AppUtils.KEY_PLACE_LON)){
            //home location changed
            mHomeCoordinate =  AppUtils.getPrefPlaceLatLng(self);
            Log.i(TAG, "home location changed.");
        }
        if(key.equals(AppUtils.KEY_MANUAL_MODE)){
            mManualMode = AppUtils.getKeyMandualMode(self);
            Log.i(TAG, "Manual mode switched.");
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


    private static String toTransitionType(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "Start Walking";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "Stop Walking";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Sets up {@link ActivityTransitionRequest}'s for the sample app, and registers callbacks for them
     * with a custom {@link BroadcastReceiver}
     */
    private void setupActivityTransitions() {
        List<ActivityTransition> transitions = new ArrayList<>();
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());
        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        // Register for Transitions Updates.
        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, mPendingIntent);
        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i(TAG, "Transitions Api was successfully registered.");
                    }
                });
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Transitions Api could not be registered: " + e);
                    }
                });
    }

    /**
     * A basic BroadcastReceiver to handle intents from from the Transitions API.
     */
    public class TransitionsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TextUtils.equals(AppConstants.TRANSITIONS_RECEIVER_ACTION, intent.getAction())) {
//                mLogFragment.getLogView()
//                        .println("Received an unsupported action in TransitionsReceiver: action="
//                                + intent.getAction());
                Toast.makeText(context, "Received an unsupported action in TransitionsReceiver: action="
                        + intent.getAction(), Toast.LENGTH_LONG).show();
                return;
            }
            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                if (result != null) {
                    for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                        //private List<AccelerometerSample> mAccSamples;
                        WalkingEvent mWalkingEvent = new WalkingEvent();
                        mWalkingEvent.WeTimestamp = SystemClock.elapsedRealtimeNanos();//System.currentTimeMillis();
                        mWalkingEvent.mTransition = event.getTransitionType();
                        mWalkingEvent.mElapsedTime = event.getElapsedRealTimeNanos();
                        //mIsWalking = mWalkingEvent.mTransition;
//                        Toast.makeText(context,toTransitionType(mWalkingEvent.mTransition),
//                                Toast.LENGTH_LONG).show();
                        if(mWalkingEvent.mTransition == 0){
                            mIsWalking = true;
                            mTransitionEnterTime = mWalkingEvent.mElapsedTime;//System.currentTimeMillis();
                        }
                        if (mWalkingEvent.mTransition == 1){
                            mIsWalking = false;
                            mTransitionExitTime = mWalkingEvent.mElapsedTime;//System.currentTimeMillis();
                        }

                        new insertAsyncTask(mDB.walkingEventDao()).execute(mWalkingEvent);

                        autoSessionManagement(); //check if need auto starting;
                    }
                }
            }
        }
    }

    private static class insertAsyncTask extends AsyncTask<WalkingEvent, Void, Void> {

        private WalkingEventDao mAsyncTaskDao;

        insertAsyncTask(WalkingEventDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(WalkingEvent... walkingEvents) {
            mAsyncTaskDao.insert(walkingEvents[0]);
            return null;
        }
    }


    private long getCurrentTimestamp(long timestamp){
        long current = -1L;
        if(sensorTimeRef1 == -1L && sysTimeRef1 == -1L){
            sensorTimeRef1 = timestamp;
            sysTimeRef1 = System.currentTimeMillis();
        }
        if(sysTimeRef2 == -1L && System.currentTimeMillis() - sysTimeRef1 > 0){
            sysTimeRef2 = System.currentTimeMillis();
            sensorTimeRef2 = timestamp;
            rateoffset = (sensorTimeRef2 - sensorTimeRef1)/(sysTimeRef2 - sysTimeRef1);
            rateoffset = rateoffset > 1000 ? 1000000L : 1;
            startoffset = sysTimeRef1 - sensorTimeRef1/rateoffset;
//            Log.e(TAG, "rate offset:" + rateoffset);
//            Log.e(TAG, "system time delta: " + (sysTimeRef2 - sysTimeRef1));
//            Log.e(TAG, "sensor time delta:" + (sensorTimeRef2 - sensorTimeRef1));
        }

        if(startoffset > 0 && rateoffset > 0){
            current = (timestamp - sensorTimeRef1)/rateoffset;// + sysTimeRef1;
        }

        return current;
    }
    private void clearSensorOffset(){
        sensorTimeRef1 = -1L;
        sensorTimeRef2 = -1L;
        sysTimeRef1 = -1L;
        sysTimeRef2 = -1L;
        rateoffset = -1L;
        startoffset = -1L;
        curTime = -1L;
        lastUpdate = -1L;
    }

    private void autoSessionManagement(){
        if(!mManualMode) {
            boolean autostart = false;
            if(mIsWalking){
                Log.e(TAG, "Is walking");
                Log.i(TAG,"GPS Lost time:" + mGPSLostTimes);
                Log.i(TAG,"GPS stable time:" + mGPSStableTimes);
                if(!AppUtils.requestingLocationUpdates(self)) {
                    getLastLocation();
                    requestLocationUpdates(false); //slow update
                }
                //TODO: 30 seconds or more to start walking?
                if ((mCurrentLocation != null) &&
                        (mCurrentLocation.getElapsedRealtimeNanos() >= mTransitionEnterTime) &&
                        mGPSLostTimes < 4 && mGPSStableTimes > 2 &&                       //if no GPS signal, it should stop
                        (mTransitionEnterTime - mTransitionExitTime) > MINUTE2NANO &&     //real start walking since last stopped
                        ((SystemClock.elapsedRealtimeNanos() - mTransitionEnterTime) > (MINUTE2NANO / 2))) {
                    autostart = true;
                }
            }
            else{
                Log.e(TAG, "Is not walking");
                if(mTransitionEnterTime > 0 && mTransitionExitTime > 0 &&
                        ((mTransitionExitTime - mTransitionEnterTime) < MINUTE2NANO ||
                        (SystemClock.elapsedRealtimeNanos() - mTransitionExitTime) < MINUTE2NANO/2)  &&
                        mGPSLostTimes < 4 && mGPSStableTimes > 2 ){
                    autostart = true; //do not stop session if it is not a real stop
                }
            }

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
                if(mIsWalking){
                    if(AppUtils.requestingLocationUpdatesFast(self))
                        //make sure whenever there is walking session, location is watched
                        requestLocationUpdates(false);
                } else {
                    if(AppUtils.requestingLocationUpdates(self)) {
                        removeLocationUpdates();
                    }
                }
            }
        }
    }
}
