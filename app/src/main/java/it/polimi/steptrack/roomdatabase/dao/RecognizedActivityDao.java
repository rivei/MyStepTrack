package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.RecognizedActivity;

@Dao
public interface RecognizedActivityDao {

    @Insert
    void insert(RecognizedActivity activities);

    @Query("DELETE FROM activities")
    void deleteAll();

    @Query("SELECT * from activities ORDER BY RTimestamp ASC")
    LiveData<List<RecognizedActivity>> getAllActivities();
}