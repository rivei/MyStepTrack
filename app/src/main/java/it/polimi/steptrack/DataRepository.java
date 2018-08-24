package it.polimi.steptrack;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.os.AsyncTask;

import java.util.List;

import it.polimi.steptrack.roomdatabase.AppDatabase;
import it.polimi.steptrack.roomdatabase.dao.AccelerometerSampleDao;
import it.polimi.steptrack.roomdatabase.entities.AccelerometerSample;
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;

/**
 * Repository handling the work with products and comments. TODO: change comments (from Basic Sample)
 */
public class DataRepository {

    private static DataRepository sInstance;

    private final AppDatabase mDatabase;
    private MediatorLiveData<List<WalkingSession>> mConductedSessions;

    private AccelerometerSampleDao mAccSampleDao;


    private DataRepository(final AppDatabase database) {
        mDatabase = database;
        //        AppDatabase db = AppDatabase.getInstance(application);
        mAccSampleDao = database.accSampleDao();
/* //TODO: set for view model
        mConductedSessions = new MediatorLiveData<>();
        mConductedSessions.addSource(mDatabase.sessionDao().getAllSessions(),
                sessions -> {
                    if(mDatabase.getDatabaseCreated().getValue() != null){
                        mConductedSessions.postValue(sessions);
                    }
                }) ;*/
    }

    public static DataRepository getInstance(final AppDatabase database) {
        if (sInstance == null) {
            synchronized (DataRepository.class) {
                if (sInstance == null) {
                    sInstance = new DataRepository(database);
                }
            }
        }
        return sInstance;
    }

    /**
     * Get the list of products from the database and get notified when the data changes.
     */
//    public LiveData<List<ProductEntity>> getProducts() {
//        return mObservableProducts;
//    }
//
//    public LiveData<ProductEntity> loadProduct(final int productId) {
//        return mDatabase.productDao().loadProduct(productId);
//    }
//
//    public LiveData<List<CommentEntity>> loadComments(final int productId) {
//        return mDatabase.commentDao().loadComments(productId);
//    }

    public LiveData<List<WalkingSession>> loadAllSessions(){
        return mConductedSessions;
    }

    public LiveData<WalkingSession> getSession (final int sessionId) {
        return mDatabase.sessionDao().getSession(sessionId);
    }


    // You must call this on a non-UI thread or your app will crash.
    // Like this, Room ensures that you're not doing any long running operations on the main
    // thread, blocking the UI.
    public void insert (AccelerometerSample accSample) {
        new insertAsyncTask(mAccSampleDao).execute(accSample);
    }

    private static class insertAsyncTask extends AsyncTask<AccelerometerSample, Void, Void> {

        private AccelerometerSampleDao mAsyncTaskDao;

        insertAsyncTask(AccelerometerSampleDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final AccelerometerSample... params) {
            mAsyncTaskDao.insert(params[0]);
            return null;
        }
    }
}

