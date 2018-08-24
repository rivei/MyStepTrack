package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.SensorSample;

@Dao
public interface SensorSampleDao {
    @Insert
    void insert(SensorSample sensorSample);

    @Query("DELETE FROM samples")
    void deleteAll();

    @Query("SELECT * from samples ORDER BY session_id ASC")
    LiveData<List<SensorSample>> getAllSession();


    @Insert
    void insertMulti(SensorSample... samples);

}
