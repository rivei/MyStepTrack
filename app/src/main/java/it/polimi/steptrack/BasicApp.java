package it.polimi.steptrack;

import android.app.Application;

import it.polimi.steptrack.roomdatabase.AppDatabase;

public class BasicApp extends Application { //TODO: from BasicSample

    private AppExecutors mAppExecutors;

    @Override
    public void onCreate() {
        super.onCreate();
        mAppExecutors = new AppExecutors();
    }

    public AppDatabase getDatabase() {
        return AppDatabase.getInstance(this, mAppExecutors);
    }

    public DataRepository getRepository() {
        return DataRepository.getInstance(getDatabase());
    }
}
