package it.polimi.steptrack.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import java.util.List;

import it.polimi.steptrack.BasicApp;
import it.polimi.steptrack.DataRepository;
import it.polimi.steptrack.roomdatabase.AppDatabase;
import android.databinding.ObservableField;

import it.polimi.steptrack.roomdatabase.dao.WalkingSessionDao;
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;

//TODO: from BasicSample
public class WalkingSessionViewModel extends AndroidViewModel {
//    private final MediatorLiveData<List<WalkingSession>> mWalkingSessions;

//    public ObservableField<WalkingSession> session = new ObservableField<>();
    //private WalkingSessionDao sessionDao;
    private final LiveData<List<WalkingSession>> mWalkingSessions;

    public WalkingSessionViewModel(@NonNull Application application) {
        super(application);
//        mWalkingSessions = new MediatorLiveData<>();
//        // set by default null, until we get data from the database.
//        mWalkingSessions.setValue(null);
//
//        LiveData<List<WalkingSession>> walkingSessions = AppDatabase.getInstance(getApplication())
//                .sessionDao().getAllSessions();
//
//        // observe the changes of the products from the database and forward them
//        mWalkingSessions.addSource(walkingSessions, mWalkingSessions::setValue);

        //sessionDao = AppDatabase.getInstance(getApplication()).sessionDao();
        mWalkingSessions = AppDatabase.getInstance(application).sessionDao().getAllSessions();
    }

    /**
     * Expose the LiveData Comments query so the UI can observe it.
     */
    public LiveData<List<WalkingSession>> getAllSessions() {
        return mWalkingSessions;
//        return sessionDao.getAllSessions();
    }

}
