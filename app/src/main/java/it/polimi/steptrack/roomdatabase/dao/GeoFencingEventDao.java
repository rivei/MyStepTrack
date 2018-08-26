package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.GeoFencingEvent;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

@Dao
public interface GeoFencingEventDao {

    @Insert(onConflict = IGNORE)
    void insert(GeoFencingEvent geoFencingEvent);

    @Query("DELETE FROM geofencing_events")
    void deleteAll();

    @Query("SELECT * from geofencing_events ORDER BY GeTimestamp ASC")
    LiveData<List<GeoFencingEvent>> getAllActivities();
}