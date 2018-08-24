package it.polimi.steptrack;

import android.app.Application;
import android.os.AsyncTask;

import it.polimi.steptrack.roomdatabase.AppDatabase;
import it.polimi.steptrack.roomdatabase.dao.AccelerometerSampleDao;
import it.polimi.steptrack.roomdatabase.entities.AccelerometerSample;

public class AccRepository {
    private AccelerometerSampleDao mAccSampleDao;


//    // TODO Note that in order to unit test the Repository, you have to remove the Application
//    // dependency. This adds complexity and much more code, and this sample is not about testing.
//    // See the BasicSample in the android-architecture-components repository at
//    // https://github.com/googlesamples
//
//    public AccRepository(Application application) {
//        AppDatabase db = AppDatabase.getInstance(application);
//        mAccSampleDao = db.accSampleDao();
//
//    }
//
//    // You must call this on a non-UI thread or your app will crash.
//    // Like this, Room ensures that you're not doing any long running operations on the main
//    // thread, blocking the UI.
//    public void insert (AccelerometerSample accSample) {
//        new insertAsyncTask(mAccSampleDao).execute(accSample);
//    }
//
//    private static class insertAsyncTask extends AsyncTask<AccelerometerSample, Void, Void> {
//
//        private AccelerometerSampleDao mAsyncTaskDao;
//
//        insertAsyncTask(AccelerometerSampleDao dao) {
//            mAsyncTaskDao = dao;
//        }
//
//        @Override
//        protected Void doInBackground(final AccelerometerSample... params) {
//            mAsyncTaskDao.insert(params[0]);
//            return null;
//        }
//    }
}
