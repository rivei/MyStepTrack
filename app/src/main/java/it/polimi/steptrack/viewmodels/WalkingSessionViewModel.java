package it.polimi.steptrack.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

import it.polimi.steptrack.roomdatabase.AppRoomDatabase;
import it.polimi.steptrack.roomdatabase.models.WalkingSession;

public class WalkingSessionViewModel extends AndroidViewModel {
    private final LiveData<List<WalkingSession>> mWalkingSessions;

    private AppRoomDatabase mDb;

    public WalkingSessionViewModel(Application application) {
        super(application);
        //createDb();

        // TODO: Assign books to the 'findBooksBorrowedByName' query.
        mWalkingSessions = null;
    }

    public void createDb() {
        mDb = AppRoomDatabase.getInstance(this.getApplication());

        // Populate it with initial data
        //DatabaseInitializer.populateAsync(mDb);
    }

    public LiveData<List<WalkingSession>> getAllSessions() { return mWalkingSessions; }

//    public void insert(WalkingSession session) { mRepository.insert(session); }
}
