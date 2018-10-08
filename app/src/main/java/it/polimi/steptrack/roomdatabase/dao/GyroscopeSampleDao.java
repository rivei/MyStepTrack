package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.GyroscopeSample;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

@Dao
public interface GyroscopeSampleDao {
    @Insert (onConflict = IGNORE)
    void insert(GyroscopeSample gyroSample);

    @Insert (onConflict = IGNORE)
    void insert(List<GyroscopeSample> accSamples);

    @Query("DELETE FROM gyro_samples")
    void deleteAll();

    @Query("SELECT * from gyro_samples ORDER BY session_id ASC")
    LiveData<List<GyroscopeSample>> getAllSamples();

    @Query("SELECT * from gyro_samples ORDER BY session_id ASC")
    List<GyroscopeSample> getAllSamplesSynchronous();


    @Insert(onConflict = IGNORE)
    void insertSamples(GyroscopeSample... accsamples);
}