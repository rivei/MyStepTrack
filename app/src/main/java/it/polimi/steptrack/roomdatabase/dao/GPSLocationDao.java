package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.models.GPSLocation;

@Dao
public interface GPSLocationDao
{
    @Insert
    void insert(GPSLocation gpsLocation);

    @Query("DELETE FROM locations")
    void deleteAll();

    @Query("SELECT * from locations ORDER BY session_id ASC")
    LiveData<List<GPSLocation>> getAllLocation();
}
