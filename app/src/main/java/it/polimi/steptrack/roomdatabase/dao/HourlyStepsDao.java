package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.HourlySteps;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

@Dao
public interface HourlyStepsDao {

    @Insert (onConflict = IGNORE)
    void insert(HourlySteps hourlySteps);

    @Query("DELETE FROM hourly_steps")
    void deleteAll();

    @Query("SELECT * from hourly_steps ORDER BY timestamp ASC")
    LiveData<List<HourlySteps>> getAllSteps();

    @Query("SELECT * from hourly_steps ORDER BY timestamp ASC")
    List<HourlySteps> getAllStepsSynchronous();

    @Query("SELECT SUM(steps) from hourly_steps WHERE timestamp BETWEEN :startTime AND :endTime")
    int getDailyStep(long startTime, long endTime);
}