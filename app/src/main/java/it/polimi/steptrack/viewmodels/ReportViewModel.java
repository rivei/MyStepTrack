package it.polimi.steptrack.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

import it.polimi.steptrack.roomdatabase.AppDatabase;
import it.polimi.steptrack.roomdatabase.entities.DailySummary;

public class ReportViewModel extends AndroidViewModel {
    // TODO: Implement the ViewModel

    private final LiveData<List<DailySummary>> reports;

    public ReportViewModel(@NonNull Application application) {
        super(application);
        reports = AppDatabase.getInstance(application).dailySummaryDao().getAllSummaries();
    }

    public LiveData<List<DailySummary>> getAllReports(){
        return reports;
    }
}
