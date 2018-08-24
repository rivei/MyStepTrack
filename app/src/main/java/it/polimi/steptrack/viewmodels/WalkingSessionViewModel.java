package it.polimi.steptrack.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
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
    private final LiveData<WalkingSession> mConductedSession;
    public ObservableField<WalkingSession> product = new ObservableField<>();
    private final int mSessionId;

    public WalkingSessionViewModel(@NonNull Application application, DataRepository repository,
                            final int sessionId) {
        super(application);
        mSessionId = sessionId;

        mConductedSession = repository.getSession(mSessionId);
//        mObservableComments = repository.loadComments(mProductId);
//        mObservableProduct = repository.loadProduct(mProductId);
    }

    /**
     * Expose the LiveData Comments query so the UI can observe it.
     */
    public LiveData<WalkingSession> getObservableProduct() {
        return mConductedSession;
    }

    public void setSession(WalkingSession session) {
        this.product.set(session);
    }

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

        public Factory(@NonNull Application application, int productId) {
            mApplication = application;
            mSessionId = productId;
            mRepository = ((BasicApp) application).getRepository();
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            //noinspection unchecked
            return (T) new WalkingSessionViewModel(mApplication, mRepository, mSessionId);
        }
    }

}
