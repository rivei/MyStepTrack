package it.polimi.steptrack.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import it.polimi.steptrack.roomdatabase.AppDatabase;

public class DataExportIntentService extends IntentService {
    private static final String TAG = DataExportIntentService.class.getSimpleName();
    private static final String DATA_NULL   = "Data NULL";


    private AppDatabase mDB;
    //private DataRepository mAccRepo;
    private List<String> mAccSamples;
    private List<String> mGyroSamples;
    private List<String> mGPSLocations;
    private List<String> mWalkingEvents;
    private List<String> mWalkingSessions;
    private List<String> mGeofencingEvent;

    public DataExportIntentService() {
        super("DataExportIntentService");
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.w(TAG, "Start Service");
        Toast.makeText(this,"Start Service", Toast.LENGTH_LONG).show();
        if(!isExternalStorageWritable()){
            Log.w(TAG, "External memory not available");
            return;
        }

        mDB = AppDatabase.getInstance(this);
//        mWalkingSessions = mDB.sessionDao().getAllSessions();
//        mAccSamples = mDB.accSampleDao().getAllSamplesString();
//        mGPSLocations = mDB.locationDao().getAllLocation();
//        mWalkingEvents = mDB.walkingEventDao().getAllActivities();
//        mGeofencingEvent = mDB.geoFencingEventDao().getAllFences();
        Log.w(TAG, "Prepare to read DB");
        //List<String> rawDataList = AppUtils.Acc2String(mDB);

        String filename = "alldata.txt";//"Insoles - " + sessionDate;
        File f;

        FileOutputStream outputStream;
        final String header = "user_id, session_id, start_time, end_time，step_count，step_detect" +
                "，distance，average_speed，duration，tag \n";

//        try {
//            f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);
//            outputStream = new FileOutputStream(f);
//
//            // Print header
//            outputStream.write(header.getBytes());
//            // Print Raw Data
//            for(String s : rawDataList){
//                outputStream.write(
//                        (s != null) ? s.getBytes() : DATA_NULL.getBytes()
//                );
//                outputStream.write("\n".getBytes());
//            }
//
//            outputStream.close();
//            Log.e(TAG, "Output Stream Closed");
//
//            Toast.makeText(this,"Export finished", Toast.LENGTH_LONG).show();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        Log.i(TAG, "End service");

    }

}
