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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
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
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


import java.util.ArrayList;
import java.util.List;

import it.polimi.steptrack.AppConstants;
import it.polimi.steptrack.AppUtils;
import it.polimi.steptrack.R;
import it.polimi.steptrack.roomdatabase.AppDatabase;
import it.polimi.steptrack.roomdatabase.dao.GeoFencingEventDao;
import it.polimi.steptrack.roomdatabase.dao.WalkingEventDao;
import it.polimi.steptrack.roomdatabase.entities.AccelerometerSample;
import it.polimi.steptrack.roomdatabase.entities.GPSLocation;
import it.polimi.steptrack.roomdatabase.entities.GeoFencingEvent;
import it.polimi.steptrack.roomdatabase.entities.GyroscopeSample;
import it.polimi.steptrack.roomdatabase.entities.WalkingEvent;
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;
import it.polimi.steptrack.ui.MainActivity;

import static it.polimi.steptrack.AppConstants.SERVICE_RUNNING_FOREGROUND;
import static it.polimi.steptrack.AppUtils.PREFS_NAME;

public class StepTrackingService extends Service
        implements SensorEventListener{

    private static final String PACKAGE_NAME = "it.polimi.steptrack";

    private static final String TAG = StepTrackingService.class.getSimpleName();

    private boolean mSessionStarted = false; //Activate location update only when session starts

    /**
     * The name of the channel for notifications.
     */
    private static final String CHANNEL_ID = "channel_01";

    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();


    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private static final int NOTIFICATION_ID = 12345678;
    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private String mNotifContentText;
    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderClient}.
     */
    private LocationRequest mLocationRequest;

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
    private Location mLocation;


    /**
     * For activity detection
     */
//    private ActivityRecognitionClient mActivityRecognitionClient; // The entry point for interacting with activity recognition.
//    protected ActivityDetectionReceiver mBroadcastReceiver;
    private PendingIntent mPendingIntent;
    private TransitionsReceiver mTransitionsReceiver;
    private int mIsWalking = -1;


    private LatLng mHomeCoordinate = null;

    private SensorManager mSensorManager;
    private Sensor countSensor = null;
    private Sensor stepDetectSensor = null;
    private Sensor accSensor = null;
    private Sensor gyroSensor = null;
    private Sensor magnSensor = null;

    private long sensorTimeRef1 = -1L;
    private long sensorTimeRef2 = -1L;
    private long sysTimeRef1 = -1L;
    private long sysTimeRef2 = -1L;
    private long startoffset = -1L;
    private long rateoffset = -1L;

    //TODO: for database:
    private AppDatabase mDB;
    //private DataRepository mAccRepo;
    private AccelerometerSample mAccSample;
    private GyroscopeSample mGyroSample;
    private GPSLocation mGPSLocation;
    //private List<AccelerometerSample> mAccSamples;
    private WalkingEvent mWalkingEvent;
    private WalkingSession mWalkingSession;
    private long mWalkingSessionId = -1;
    private int mStepsCount = 0;


    public StepTrackingService() {
    }

    @Override
    public void onCreate() {
        Log.w(TAG, "On Creating");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation(); //This is just a fuse location without high accuracy

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        //***HandlerThread needs to call myHandlerThread.quit() to free the resources and stop the execution of the thread.

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
        mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
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


        //******************** Activity Detection *************************************//
//        mActivityRecognitionClient = new ActivityRecognitionClient(this);
//        mBroadcastReceiver = new ActivityDetectionReceiver();
        Intent intent = new Intent(AppConstants.TRANSITIONS_RECEIVER_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        //PendingIntent.getService(this,)
        mTransitionsReceiver = new TransitionsReceiver();
        registerReceiver(mTransitionsReceiver, new IntentFilter(AppConstants.TRANSITIONS_RECEIVER_ACTION));


        //TODO: db
        //mAccRepo = ((BasicApp)apl
        mDB = AppDatabase.getInstance(this);
    }

    //This is called only when there is Start service
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
//        TODO-NOTE: removed because not need action in the notification
//        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
//                false);
//
//        // We got here because the user decided to remove location updates from the notification.
//        if (startedFromNotification) {
//            removeLocationUpdates();
//            stopSelf();
//        }
//        // Tells the system to not try to recreate the service after it has been killed.
//        return START_NOT_STICKY;
//

        //************************* For step count ~*********************//
/*
        // Setup First Notification TODO-Q?? the first is set when calling start foregound?
        mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
        //updateNotification(true);

        // Setup Shared Preference Change Listener
        SharedPreferences sharedPreferences;
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // Update Notification Bar
                //updateNotification(false);
                mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
*/
        //TODO: start Monitoring activities and locations
        setupActivityTransitions();

//        //TODO: make the notification works when the service resume from phone reboot
        if (!AppUtils.getKeyActivityActive(this)){
            startForeground(NOTIFICATION_ID, getNotification(true));
        }

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

        super.onDestroy();
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        mSessionStarted = true;
        Log.i(TAG, "Requesting location updates");
        //AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
        AppUtils.setKeyStartingWalkingSession(this, mSessionStarted);
        //TODO-NOTE: here makes the call for onStartCommand.
        //startService(new Intent(getApplicationContext(), StepTrackingService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
            //TODO: locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new MyLocationListener());
            //Add sensor listener:
        } catch (SecurityException unlikely) {
            mSessionStarted = false;
            //AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
            AppUtils.setKeyStartingWalkingSession(this, mSessionStarted);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
        mWalkingSession = new WalkingSession();
        mWalkingSession.mStartTime = System.currentTimeMillis();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                mWalkingSessionId = mDB.sessionDao().insert(mWalkingSession);
//                mWalkingSession.sid = mWalkingSessionId;
//            }
//        }).run();
        Thread t = new Thread() {
            public void run() {
                mWalkingSessionId = mDB.sessionDao().insert(mWalkingSession);
                mWalkingSession.sid = mWalkingSessionId;
            }
        };
        t.start();

        if (accSensor != null) {
            //TODO: Accelerometer sensor registration listener
            Toast.makeText(this, "Started Acceleration", Toast.LENGTH_LONG).show();
            mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(this, "Accelerometer not Compatible!", Toast.LENGTH_LONG).show();
            this.stopSelf();
        }

        if (gyroSensor != null) {
            //TODO: Gyroscope sensor registration listener
            Toast.makeText(this, "Started Gyroscope", Toast.LENGTH_LONG).show();
            mSensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(this, "Gyroscope not Compatible!", Toast.LENGTH_LONG).show();
            this.stopSelf();
        }
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            mSessionStarted = false;
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            //AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
            AppUtils.setKeyStartingWalkingSession(this, mSessionStarted);
            //stopSelf(); //TODO: move this to the whole service

        } catch (SecurityException unlikely) {
            mSessionStarted = true;
            //AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
            AppUtils.setKeyStartingWalkingSession(this,mSessionStarted);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
        //TODO: unregister sensor
//        mSessionStarted = false;
//        AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
        if (mWalkingSessionId != -1) {
            mWalkingSession.mEndTime = System.currentTimeMillis();
            mWalkingSession.mDuration = mWalkingSession.mEndTime - mWalkingSession.mStartTime;
            SharedPreferences prefs = this.getSharedPreferences(PREFS_NAME, 0);
            mWalkingSession.mStepCount = prefs.getInt("stepCount", 0);
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    mDB.sessionDao().update(mWalkingSession);
//                }
//            }).run();
            Thread t = new Thread() {
                public void run() {
                    mDB.sessionDao().update(mWalkingSession);
                }
            };
            t.start();
            mWalkingSessionId = -1;
        }
        mSensorManager.unregisterListener(this);
        clearSensorOffset();
        //Never stop step counter sensor listening
        mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification(boolean isFirstTime) {
        String msg = "Step Counter - Counting";
        if (mNotifContentText != null) msg = mNotifContentText;
        if (isFirstTime) {
            // PendingIntent to launch activity.
            PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, MainActivity.class), 0);
            mBuilder.setContentIntent(activityPendingIntent)
                    .setContentText(msg)
                    .setContentTitle(AppUtils.getLocationTitle(this))
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH)

                    //.setTicker(text)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(false)
                    .setOnlyAlertOnce(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilder.setVisibility(Notification.VISIBILITY_SECRET);
            }
            // Set the Channel ID for Android O.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder.setChannelId(CHANNEL_ID); // Channel ID
            }
        }
        // Update Step Count
        mBuilder.setSmallIcon(R.mipmap.ic_tracking); //It's a must
        mBuilder.setContentTitle(mStepsCount + " steps taken");
        if (mSessionStarted) {
            //mBuilder.setContentText(String.format("X:%f Y:%f Z%f", mAccSample.mAccX, mAccSample.mAccY, mAccSample.mAccZ));
        } else {
            mBuilder.setContentText(msg);
        }

        return mBuilder.build();
    }

    private void getLastLocation() {
        Log.w(TAG, "getting last location from onCreate");
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();

                                //Log.w(TAG, "The inital location: " + mLocation);
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);

        mLocation = location;
        if (mSessionStarted && mWalkingSessionId != -1 ) {
            mGPSLocation = new GPSLocation();
            mGPSLocation.GTimestamp = location.getTime();
            mGPSLocation.latitude = location.getLatitude();
            mGPSLocation.longitude = location.getLongitude();
            mGPSLocation.provider = location.getProvider();
            mGPSLocation.accuracy = location.getAccuracy();
            if (location.hasSpeed()){
                mGPSLocation.speed = location.getSpeed();
                //mGPSLocation.speedAccuracy = location.getSpeedAccuracyMetersPerSecond() // > Android v26
            }
            if(location.hasBearing()){
                mGPSLocation.bearing = location.getBearing();
            }
            mGPSLocation.isWalking = mIsWalking;
            mGPSLocation.session_id = mWalkingSessionId;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mDB.locationDao().insert(mGPSLocation);
                    Log.w(TAG, "write GPS");
                }
            }).start();
        }
//        // Notify anyone listening for broadcasts about the new location.
//        Intent intent = new Intent(ACTION_BROADCAST);
//        intent.putExtra(EXTRA_LOCATION, location);
//        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        //TODO-NOTE: not location Update notification content if running as a foreground service.
//        if (serviceIsRunningInForeground(this)) {
//            mNotificationManager.notify(NOTIFICATION_ID, getNotification(true));
//        }
        if (AppUtils.getServiceRunningStatus(this) == SERVICE_RUNNING_FOREGROUND) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification(true));
        }

    }

    /**
     * TODO-NOTE: Sets the location request parameters.update interval and accuracy
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(AppConstants.UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(AppConstants.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // Record Step Count
            mStepsCount = (int)sensorEvent.values[0] - AppUtils.getStepCountOffset(this);
            AppUtils.setKeyLastStepCount(this, mStepsCount);
            Log.i(TAG, "steps: " + mStepsCount);
            if (AppUtils.getServiceRunningStatus(this) == SERVICE_RUNNING_FOREGROUND) {
                mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
            }
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            setupSensorOffset(sensorEvent.timestamp);
            if(startoffset > 0 && rateoffset > 0){
                if (mWalkingSessionId != -1) {
//                    mAccSample = new AccelerometerSample();
//                    mAccSample.SessionID = mWalkingSessionId;
//                    mAccSample.AsTimestamp = (sensorEvent.timestamp - sensorTimeRef1) / rateoffset + sysTimeRef1;//+
//    //                        ((sensorEvent.timestamp - sensorTimeReference)/1000000L);
//    //                mAccSample.AsTimestamp = sensorEvent.timestamp;
//                    mAccSample.mAccX = sensorEvent.values[0];
//                    mAccSample.mAccY = sensorEvent.values[1];
//                    mAccSample.mAccZ = sensorEvent.values[2];
//                    // record acceleration
//                    //mDB.accSampleDao().insert(mAccSample);
//                    if (mSessionStarted) {
//                        //mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
//                    Thread t = new Thread() {
//                        public void run() {
//                            mDB.accSampleDao().insert(mAccSample);
//
//                        }
//                    };
//                    t.start();
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                mDB.accSampleDao().insert(mAccSample);
//                                //Log.w(TAG, "write ACC: " + mAccSample.AsTimestamp);
//                            }
//                        }).start();
//                    }
                }
            }
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            setupSensorOffset(sensorEvent.timestamp);
            if (startoffset > 0 && rateoffset > 0){
//                if(mWalkingSessionId != -1){
//                    mGyroSample = new GyroscopeSample();
//                    mGyroSample.SessionID = mWalkingSessionId;
//                    mGyroSample.GyTimestamp = (sensorEvent.timestamp - sensorTimeRef1)/rateoffset + sysTimeRef1;
//                    mGyroSample.mGyroX = sensorEvent.values[0];
//                    mGyroSample.mGyroY = sensorEvent.values[1];
//                    mGyroSample.mGyroZ = sensorEvent.values[2];
//                    if (mSessionStarted){
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                mDB.gyroSampleDao().insert(mGyroSample);
//                            }
//                        }).start();
//                    }
//                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void updateNotification(String msg) {
        if (msg != null && mBuilder != null) {
            mNotifContentText = msg;
            mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
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
//                        String activity = toActivityString(event.getActivityType());
//                        String transitionType = toTransitionType(event.getTransitionType());
//                        mLogFragment.getLogView()
//                                .println("Transition: "
//                                        + activity + " (" + transitionType + ")" + "   "
//                                        + new SimpleDateFormat("HH:mm:ss", Locale.US)
//                                        .format(new Date()));
                        mWalkingEvent = new WalkingEvent();
                        mWalkingEvent.WeTimestamp = System.currentTimeMillis();
                        mWalkingEvent.mTransition = event.getTransitionType();
                        mWalkingEvent.mElapsedTime = event.getElapsedRealTimeNanos();
                        mIsWalking = mWalkingEvent.mTransition;
                        Toast.makeText(context,toTransitionType(mWalkingEvent.mTransition),
                                Toast.LENGTH_LONG).show();
                        //TODO: write db -> crashed it says it's in main thread.??
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                //TODO:
//                                mDB.walkingEventDao().insert(mWalkingEvent);
//
//                            }
//                        }).run();
                        new insertAsyncTask(mDB.walkingEventDao()).execute(mWalkingEvent);
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


    private void setupSensorOffset(long timestamp){
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
    }
    private void clearSensorOffset(){
        sensorTimeRef1 = -1L;
        sensorTimeRef2 = -1L;
        sysTimeRef1 = -1L;
        sysTimeRef2 = -1L;
        rateoffset = -1L;
        startoffset = -1L;
    }
}
