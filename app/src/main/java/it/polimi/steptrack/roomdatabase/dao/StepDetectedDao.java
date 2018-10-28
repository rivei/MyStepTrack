package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.StepDetected;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

@Dao
public interface StepDetectedDao {

    @Insert (onConflict = IGNORE)
    void insert(StepDetected stepDetected);

    @Query("DELETE FROM step_detected")
    void deleteAll();

    @Query("SELECT * from step_detected ORDER BY timestamp ASC")
    LiveData<List<StepDetected>> getAllSteps();

    @Query("SELECT * from step_detected ORDER BY timestamp ASC")
    List<StepDetected> getAllStepsSynchronous();

    @Query("SELECT SUM(steps) from step_detected WHERE timestamp BETWEEN :startTime AND :endTime")
    int getSessionStep(long startTime, long endTime);
}