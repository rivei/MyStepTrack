package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.GPSLocation;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

@Dao
public interface GPSLocationDao
{
    @Insert(onConflict = IGNORE)
    void insert(GPSLocation gpsLocation);

    @Query("DELETE FROM locations")
    void deleteAll();

    @Query("SELECT * from locations ORDER BY session_id ASC")
    LiveData<List<GPSLocation>> getAllLocation();

    @Query("SELECT * from locations ORDER BY session_id ASC")
    List<GPSLocation> getAllLocationSynchronous();

    @Query("SELECT * from locations WHERE session_id = :sid ORDER BY GTimestamp ASC")
    List<GPSLocation> getSessionLocations(long sid);

    @Query("SELECT AVG(speed) FROM locations WHERE session_id = :sid AND speed > 0 ORDER BY GTimestamp ASC")
    float getSessionSpeed(long sid);
}
