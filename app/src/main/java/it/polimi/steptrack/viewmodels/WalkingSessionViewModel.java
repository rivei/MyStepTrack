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
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;

//TODO: from BasicSample
public class WalkingSessionViewModel extends AndroidViewModel {
    private final MediatorLiveData<List<WalkingSession>> mWalkingSessions;

//    public ObservableField<WalkingSession> session = new ObservableField<>();

    public WalkingSessionViewModel(@NonNull Application application) {
        super(application);
        mWalkingSessions = new MediatorLiveData<>();
        // set by default null, until we get data from the database.
        mWalkingSessions.setValue(null);

        LiveData<List<WalkingSession>> walkingSessions = ((BasicApp) application).getRepository()
                .loadAllSessions();//.getProducts();

        // observe the changes of the products from the database and forward them
        mWalkingSessions.addSource(walkingSessions, mWalkingSessions::setValue);

    }

    /**
     * Expose the LiveData Comments query so the UI can observe it.
     */
    public LiveData<List<WalkingSession>> getAllSessions() {
        return mWalkingSessions;
    }

//    public void setSession(WalkingSession session) {
//        this.product.set(session);
//    }

    /**
     * A creator is used to inject the product ID into the ViewModel
     * <p>
     * This creator is to showcase how to inject dependencies into ViewModels. It's not
     * actually necessary in this case, as the product ID can be passed in a public method.
     */
    public static class Factory extends ViewModelProvider.NewInstanceFactory {

        @NonNull
        private final Application mApplication;

        private final int mSessionId;

        private final DataRepository mRepository;

        public Factory(@NonNull Application application, int sessionId) {
            mApplication = application;
            mSessionId = sessionId;
            mRepository = ((BasicApp) application).getRepository();
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            //noinspection unchecked
            return (T) new WalkingSessionViewModel(mApplication);
        }
    }

}
