package it.polimi.steptrack.ui;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import it.polimi.steptrack.AppUtils;
import it.polimi.steptrack.BuildConfig;
import it.polimi.steptrack.R;
import it.polimi.steptrack.receivers.ServiceRestartReceiver;
import it.polimi.steptrack.roomdatabase.AppDatabase;
import it.polimi.steptrack.roomdatabase.entities.AccelerometerSample;
import it.polimi.steptrack.roomdatabase.entities.GPSLocation;
import it.polimi.steptrack.roomdatabase.entities.GeoFencingEvent;
import it.polimi.steptrack.roomdatabase.entities.GyroscopeSample;
import it.polimi.steptrack.roomdatabase.entities.WalkingEvent;
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;
import it.polimi.steptrack.services.DataExportIntentService;
import it.polimi.steptrack.services.StepTrackingService;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    SharedPreferences.OnSharedPreferenceChangeListener,
                    StatusFragment.OnStatusInteractionListener,
                    SettingFragment.OnSettingFragmentInteractionListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    final MainActivity self = this;

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34; //TODO should be 12345??
    private static final String[] permissionsArray = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    FloatingActionButton fab1;
    FloatingActionButton fab2;

    private static final int PLACE_PICKER_REQUEST = 1;

    // A reference to the service used to get location updates.
    private StepTrackingService mService = null;
    //private StepTrackingService2 mService2 = null;
    // Tracks the bound state of the service.
    private boolean mBound = false;
    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepTrackingService.LocalBinder binder = (StepTrackingService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //set-up bootreceiver
        final ComponentName onBootReceiver = new ComponentName(getApplication().getPackageName(), ServiceRestartReceiver.class.getName());
        if(getPackageManager().getComponentEnabledSetting(onBootReceiver) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            getPackageManager().setComponentEnabledSetting(onBootReceiver,PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Check that the user hasn't revoked permissions by going to Settings.
        if(!checkPermissions()){
            requestPermissions();
        }

//        WelcomeFragment fragment = new WelcomeFragment();
//        getSupportFragmentManager().beginTransaction()
//                .add(R.id.fragment_container, fragment, WelcomeFragment.TAG).commit();

        //TODO: Check if address is set, if not popup place picking dialog
        //StatusFragment fragment = StatusFragment.newInstance(45.482134, 9.224853);
//        getSupportFragmentManager().beginTransaction()
//                .add(R.id.fragment_container, fragment, StatusFragment.TAG).commit();

        //TODO: *** Start service for counting steps
        // Check if the service is running
        if (AppUtils.getServiceRunningStatus(self) <= 0) {

//            //TODO: Mark Service as Started
//            AppUtils.setServiceRun(this, false);

            // Start Step Counting service
            Intent serviceIntent = new Intent(this, StepTrackingService.class);
            startService(serviceIntent); //Activate onStartCommand
        }

    }

    @Override
    public void onBackPressed() { //auto-generated
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //auto-generated
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { //auto-generated
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment fragment = null;

        if (id == R.id.nav_camera) {
            // update the main content by replacing fragments
            LatLng latLng = AppUtils.getPrefPlaceLatLng(self);
            if (latLng == null){
                Toast.makeText(self,"No home address recorded. Please select Home location", Toast.LENGTH_LONG).show();
                PickHomeAddress();
                return false;
            }
            fragment = StatusFragment.newInstance(latLng.latitude, latLng.longitude);

            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();

        } else if (id == R.id.nav_gallery) {
            //Pick the location for home address
            fragment = SettingFragment.newInstance("1", "2");
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();

        } else if (id == R.id.nav_slideshow) {
            //TODO: Export all data
            Log.w(TAG, "Going to export data");
//            Intent createFileIntent = new Intent(this, DataExportIntentService.class);
//            startService(createFileIntent);
//            Log.w(TAG,"FINISHED??");
            ExportData();
        } else if (id == R.id.nav_manage) {
//            WalkingSessionFragment walkingSessionFragment = new WalkingSessionFragment();
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, walkingSessionFragment, WalkingSessionFragment.TAG)
//                    .commit();

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

//        try{
//            fragment = (Fragment) fragmentClass.newInstance();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // Insert the fragment by replacing any existing fragment
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);


        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, StepTrackingService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);//TODO NOTE: activates the onCreate of service and
        //TODO NOTE: the foreground mode only works when this is put in onStart

        LatLng latLng = AppUtils.getPrefPlaceLatLng(self);
        if (latLng == null){
            Toast.makeText(self,"No home address recorded. Please select Home location", Toast.LENGTH_LONG).show();
            PickHomeAddress();
            //return false;
//            StatusFragment fragment;
//            fragment = StatusFragment.newInstance(-1.0, -1.0);
//            FragmentManager fragmentManager = getSupportFragmentManager();
//            fragmentManager.beginTransaction()
//                    .add(R.id.fragment_container, fragment, StatusFragment.TAG)
//                    .commit();
        }
        else {
            StatusFragment fragment;
            fragment = StatusFragment.newInstance(latLng.latitude, latLng.longitude);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, fragment, StatusFragment.TAG)
                    .commit();
        }
    }

    private void PickHomeAddress(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.permission_rationale), Toast.LENGTH_LONG).show();
            return;
        }
        try {
            // Start a new Activity for the Place Picker API, this will trigger {@code #onActivityResult}
            // when a place is selected or with the user cancels.
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            //Intent i = builder.build(this);
            startActivityForResult(builder.build(MainActivity.this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, String.format("GooglePlayServices Not Available [%s]", e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, String.format("PlacePicker Exception: %s", e.getMessage()));
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
//        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
//                new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));

    }

    @Override
    protected void onPause() {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
//        if (myFenceReceiver != null) {
//            unregisterReceiver(myFenceReceiver);
//            myFenceReceiver = null;
//        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

        AppUtils.setKeyActivityActive( this,false);
        super.onStop();
    }

    /***
     * Called when the Place Picker Activity returns back with a selected place (or after canceling)
     *
     * @param requestCode The request code passed when calling startActivityForResult
     * @param resultCode  The result code specified by the second activity
     * @param data        The Intent that carries the result data.
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            if (place == null) {
                Log.i(TAG, "No place selected");
                return;
            }
            AppUtils.setPrefPlace(self,place);
            LatLng latLng = place.getLatLng();
            StatusFragment fragment;
            fragment = StatusFragment.newInstance(latLng.latitude, latLng.longitude);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, fragment, StatusFragment.TAG)
                    .commit();

////            // Extract the place information from the API
//            //Temperately use sharedpreference
//            AppUtils.setPrefPlace(self,place);
//            WelcomeFragment fragment = (WelcomeFragment)getSupportFragmentManager().
//                    findFragmentByTag(WelcomeFragment.TAG);
//                    //findFragmentById(R.id.fragment_container);
//            if(fragment != null){
//                fragment.setHomeCoordinate(place.getLatLng());
////                TextView textView = fragment.getView().findViewById(R.id.tvSteps);
////                textView.setText("Home is: " + place.getLatLng().toString());
//            }
//            Toast.makeText(self,getString(R.string.home_address_added), Toast.LENGTH_SHORT).show();
////            // Insert a new place into DB TODO
////            ContentValues contentValues = new ContentValues();
////            contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, placeID);
////            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, contentValues);
////
////            // Get live data information
////            refreshPlacesData();
        }
        else{
            Toast.makeText(self,"No home address recorded. Please select Home location", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        // Update the buttons state depending on whether location updates are being requested.
        if (s.equals(AppUtils.KEY_REQUESTING_LOCATION_UPDATES)) {
//            setButtonsState(sharedPreferences.getBoolean(AppUtils.KEY_REQUESTING_LOCATION_UPDATES,
//                    false));
        }
        //if (s.equals(AppUtils.KEY_PLACE_LAT))
    }



    /**
     * Returns the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        boolean hasAllPermissions = true;
        for (String s : permissionsArray) {
            if (ActivityCompat.checkSelfPermission(this, s)
                    != PackageManager.PERMISSION_GRANTED) {
                hasAllPermissions = false;
                break;
            }
        }
        return  hasAllPermissions;
    }

    private void requestPermissions() {
        //TODO: make the permission list better
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.drawer_layout),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    permissionsArray, REQUEST_PERMISSIONS_REQUEST_CODE);
/*                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);*/
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    permissionsArray, REQUEST_PERMISSIONS_REQUEST_CODE);
//            ActivityCompat.requestPermissions(MainActivity.this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted. TODO make this only for location update
                //mService.requestLocationUpdates();
            } else {
                // Permission denied.
                setButtonsState(false);
                Snackbar.make(
                        findViewById(R.id.drawer_layout),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    private void setButtonsState(boolean requestingLocationUpdates) {
        if (requestingLocationUpdates) {
            fab1.hide();//.setEnabled(false);
            fab2.show();//.setEnabled(true);
        } else {
            fab1.show();//.setEnabled(true);
            fab2.hide();//.setEnabled(false);
        }
    }

    private void ExportData(){
        if(!isExternalStorageWritable()){
            Log.w(TAG, "External memory not available");
            return;
        }

        Log.w(TAG, "Prepare to read DB");
        Thread t = new Thread() {
            public void run() {
                AppDatabase mDB = AppDatabase.getInstance(self);
                List<String> rawDataList = new ArrayList<String>();
                for (AccelerometerSample sample : mDB.accSampleDao().getAllSamplesSynchronous()) {
                    rawDataList.add(sample.toString());
                }
                String filename = "accSamples.csv";//"Insoles - " + sessionDate;
                final String header = "session_id, timestamp, acc_x, acc_y, acc_z \n";
                AppUtils.writeFile(filename, header, rawDataList);
            }
        };
        t.start();
        t = new Thread() {
            public void run() {
                AppDatabase mDB = AppDatabase.getInstance(self);
                List<String> rawDataList = new ArrayList<String>();
                for (GeoFencingEvent event : mDB.geoFencingEventDao().getAllFencesSynchronous()) {
                    rawDataList.add(event.toString());
                }
                String filename = "Geofences.csv";
                final String header = "timestamp, transition \n";
                AppUtils.writeFile(filename, header, rawDataList);
            }
        };
        t.start();
        t = new Thread() {
            public void run() {
                AppDatabase mDB = AppDatabase.getInstance(self);
                List<String> rawDataList = new ArrayList<String>();
                for (GPSLocation location : mDB.locationDao().getAllLocationSynchronous()) {
                    rawDataList.add(location.toString());
                }
                String filename = "Locations.csv";
                final String header = "session_id, GTimestamp,latitude, longitude, provider, accuracy \n";
                AppUtils.writeFile(filename, header, rawDataList);
            }
        };
        t.start();
        t = new Thread() {
            public void run() {
                AppDatabase mDB = AppDatabase.getInstance(self);
                List<String> rawDataList = new ArrayList<String>();
                for (WalkingEvent event : mDB.walkingEventDao().getAllActivitiesSynchronous()) {
                    rawDataList.add(event.toString());
                }
                String filename = "iswalking.csv";
                final String header = "timestamp,transition, elapse_time \n";
                AppUtils.writeFile(filename, header, rawDataList);
            }
        };
        t.start();
        t = new Thread() {
            public void run() {
                AppDatabase mDB = AppDatabase.getInstance(self);
                List<String> rawDataList = new ArrayList<String>();
                for (WalkingSession session : mDB.sessionDao().getAllSessionsSynchronous()) {
                    rawDataList.add(session.toString());
                }
                String filename = "walkingsession.csv";
                final String header = "user_Id, session_id, StartTime, EndTime, StepCount, StepDetect, Distance, AverageSpeed, Duration, Tag \n";
                AppUtils.writeFile(filename, header, rawDataList);
            }
        };
        t.start();
        t = new Thread() {
            public void run() {
                AppDatabase mDB = AppDatabase.getInstance(self);
                List<String> rawDataList = new ArrayList<String>();
                for (GyroscopeSample gyroSample : mDB.gyroSampleDao().getAllSamplesSynchronous()) {
                    rawDataList.add(gyroSample.toString());
                }
                String filename = "gyroSamples.csv";//"Insoles - " + sessionDate;
                final String header = "session_id, timestamp, gyro_x, gyro_y, gyro_z \n";
                AppUtils.writeFile(filename, header, rawDataList);
            }
        };
        t.start();
    }

    @Override
    public void StatusInteraction(int interactionType) {
        switch (interactionType){
            case StatusFragment.ON_START_CLICKED:
                if (mService != null){
                    if (! AppUtils.startingWalkingSession(self)){
                        mService.requestLocationUpdates();
                    }else {
                        mService.removeLocationUpdates();
                    }
                }
                //Toast.makeText(self, "Clicked started", Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void onSettingFragmentInteraction(int interactionType) {
        switch (interactionType){
            case SettingFragment.ON_PLACE_CLICKED:
                    //Pick the location for home address
                PickHomeAddress();
//                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
//                        != PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(this, getString(R.string.permission_rationale), Toast.LENGTH_LONG).show();
//                    return;
//                }
//                try {
//                    // Start a new Activity for the Place Picker API, this will trigger {@code #onActivityResult}
//                    // when a place is selected or with the user cancels.
//                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
//                    //Intent i = builder.build(this);
//                    startActivityForResult(builder.build(MainActivity.this), PLACE_PICKER_REQUEST);
//                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
//                    Log.e(TAG, String.format("GooglePlayServices Not Available [%s]", e.getMessage()));
//                } catch (Exception e) {
//                    Log.e(TAG, String.format("PlacePicker Exception: %s", e.getMessage()));
//                }
                break;
            case SettingFragment.ON_EXPORT_CLICKED:
                ExportData();
                break;
        }
    }
}
