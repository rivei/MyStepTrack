package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.DailySummary;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

@Dao
public interface DailySummaryDao {
    @Insert(onConflict = IGNORE)
    void insert(DailySummary dailySummary);

    @Update
    void update(DailySummary dailySummary);

    @Query("DELETE FROM daily_summaries")
    void deleteAll();

    @Query("SELECT * from daily_summaries ORDER BY did DESC")
    LiveData<List<DailySummary>> getAllSummaries();

    @Query("SELECT * from daily_summaries ORDER BY did ASC")
    List<DailySummary> getAllSummariesSynchronous();
//
//    @Query("SELECT * from daily_summaries WHERE did = (SELECT MAX(did) FROM daily_summaries)")
//    MutableLiveData<DailySummary> getLastReportTime();

}
