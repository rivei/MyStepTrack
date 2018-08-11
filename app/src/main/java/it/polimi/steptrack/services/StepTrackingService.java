package it.polimi.steptrack.services;

import android.Manifest;
import android.app.ActivityManager;
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
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.List;

import it.polimi.steptrack.AppConstants;
import it.polimi.steptrack.AppUtils;
import it.polimi.steptrack.BuildConfig;
import it.polimi.steptrack.R;
import it.polimi.steptrack.ui.MainActivity;

import static it.polimi.steptrack.AppConstants.SERVICE_RUNNING;
import static it.polimi.steptrack.AppConstants.SERVICE_RUNNING_FOREGROUND;
import static it.polimi.steptrack.AppUtils.PREFS_NAME;

public class StepTrackingService extends Service implements
        SensorEventListener {
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

    // The fence key is how callback code determines which fence fired.
    private final String FENCE_KEY = "fence_key";
    // The intent action which will be fired when your fence is triggered.
    private final String FENCE_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "FENCE_RECEIVER_ACTION";
    // Declare variables for pending intent and fence receiver.
    private PendingIntent myPendingIntent;
    private MyFenceReceiver myFenceReceiver;


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

        //************************* For step count ~*********************//
        // Setup Step Counter
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = null;
        if (sensorManager != null) {
            countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
        if (countSensor != null) {
            Toast.makeText(this, "Started Counting Steps", Toast.LENGTH_LONG).show();
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Device not Compatible!", Toast.LENGTH_LONG).show();
            this.stopSelf();
        }

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

        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        myFenceReceiver = new MyFenceReceiver();
        registerReceiver(myFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
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
        // Setup First Notification TODO-Q?? the first is set when calling start forgound?
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
        //start Monitoring activities and locations
        setupFences();

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
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
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
        if (!mChangingConfiguration) {//TODO: && AppUtils.requestingLocationUpdates(this)) {
            Log.i(TAG, "Starting foreground service");
            startForeground(NOTIFICATION_ID, getNotification(true)); //Anyway works for Oreo
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        if (myFenceReceiver != null) {
            unregisterReceiver(myFenceReceiver);
            myFenceReceiver = null;
        }
        // Unregister the fence:
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .removeFence(FENCE_KEY)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Fence was successfully unregistered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Fence could not be unregistered: " + e);
                    }
                });
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        mSessionStarted = true;
        Log.i(TAG, "Requesting location updates");
        AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
        //TODO-NOTE: here makes the call for onStartCommand.
        //startService(new Intent(getApplicationContext(), StepTrackingService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            mSessionStarted = false;
            AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
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
            AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
            //stopSelf(); //TODO: move this to the whole service
        } catch (SecurityException unlikely) {
            mSessionStarted = true;
            AppUtils.setRequestingLocationUpdates(this, mSessionStarted);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
     private Notification getNotification(boolean isFirstTime) {
         String msg = "Step Counter - Counting";
        if (mNotifContentText != null)  msg = mNotifContentText;
        if (isFirstTime) {
            // PendingIntent to launch activity.
            PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, MainActivity.class), 0);
            mBuilder.setContentIntent(activityPendingIntent)
                    .setContentText(msg)
                    .setContentTitle(AppUtils.getLocationTitle(this))
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setSmallIcon(R.mipmap.ic_tracking)
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
         mBuilder.setContentText(msg);
        mBuilder.setContentTitle(AppUtils.getStepCount(this) + " steps taken");

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
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(AppConstants.UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(AppConstants.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Record Step Count
        AppUtils.setStepCount(this, (int) sensorEvent.values[0]);
        Log.i(TAG, AppUtils.getStepCount(this));
        if (AppUtils.getServiceRunningStatus(this) == SERVICE_RUNNING_FOREGROUND) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void updateNotification(String msg){
        if (msg != null && mBuilder!= null) {
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


    /**
     * Sets up {@link AwarenessFence}'s for the sample app, and registers callbacks for them
     * with a custom {@link BroadcastReceiver}
     */
    private void setupFences() {
        // DetectedActivityFence will fire when it detects the user performing the specified
        // activity.  In this case it's walking.
        AwarenessFence walkingFence = DetectedActivityFence.during(DetectedActivityFence.WALKING);
        AwarenessFence headphoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);

        //Location for Home TODO!!!
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        AwarenessFence geoFence = LocationFence.exiting(45.472236, 9.227279, 25);
        // We can even nest compound fences.  Using both "and" and "or" compound fences, this
        // compound fence will determine when the user has headphones in and is engaging in at least
        // one form of exercise.
        // The below breaks down to "(headphones plugged in) AND (walking OR running OR bicycling)"
        AwarenessFence outHomeFence = AwarenessFence.and(geoFence, AwarenessFence.or(
                walkingFence,DetectedActivityFence.during(DetectedActivityFence.ON_FOOT)));

        // Now that we have an interesting, complex condition, register the fence to receive
        // callbacks.

        // Register the fence to receive callbacks.
        Awareness.getFenceClient(this).updateFences(new FenceUpdateRequest.Builder()
                .addFence(FENCE_KEY, outHomeFence, myPendingIntent)
                .build())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Fence was successfully registered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Fence could not be registered: " + e);
                    }
                });
    }


    // Handle the callback on the Intent.
    public class MyFenceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TextUtils.equals(FENCE_RECEIVER_ACTION, intent.getAction())) {
//                mLogFragment.getLogView()
//                        .println("Received an unsupported action in FenceReceiver: action="
//                                + intent.getAction());
                Toast.makeText(context,"Received an unsupported action in FenceReceiver: action="
                        + intent.getAction(), Toast.LENGTH_LONG).show();
                return;
            }

            FenceState fenceState = FenceState.extract(intent);
            if (TextUtils.equals(fenceState.getFenceKey(), FENCE_KEY)) {
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Log.i(TAG, "Walking out of home.");
                        updateNotification("Walking out of home.");
                        //mNotifContentText = "Walking out of home.";
                        //mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
                        break;
                    case FenceState.FALSE:
                        Log.i(TAG, "Walking back home.");
                        updateNotification( "Walking back home");
                        //mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
                        break;
                    case FenceState.UNKNOWN:
                        Log.i(TAG, "unknown state.");
                        updateNotification( "unknow state");
                        //mNotificationManager.notify(NOTIFICATION_ID, getNotification(false));
                        break;
                }
            }
        }
    }

}
