package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.WalkingEvent;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

@Dao
public interface WalkingEventDao {

    @Insert (onConflict = IGNORE)
    void insert(WalkingEvent walkingEvent);

    @Query("DELETE FROM walking_events")
    void deleteAll();

    @Query("SELECT * from walking_events ORDER BY WeTimestamp ASC")
    LiveData<List<WalkingEvent>> getAllActivities();

    @Query("SELECT * from walking_events ORDER BY WeTimestamp ASC")
    List<WalkingEvent> getAllActivitiesSynchronous();
}