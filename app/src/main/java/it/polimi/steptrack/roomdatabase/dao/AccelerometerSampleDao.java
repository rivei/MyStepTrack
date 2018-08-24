package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.AccelerometerSample;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

@Dao
public interface AccelerometerSampleDao {
    @Insert (onConflict = IGNORE)
    void insert(AccelerometerSample accSample);

    @Query("DELETE FROM acc_samples")
    void deleteAll();

    @Query("SELECT * from acc_samples ORDER BY session_id ASC")
    LiveData<List<AccelerometerSample>> getAllSession();


    @Insert(onConflict = IGNORE)
    void insertSamples(AccelerometerSample... accsamples);

}