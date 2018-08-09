package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import it.polimi.steptrack.roomdatabase.models.WalkingSession;

@Dao
public interface WalkingSessionDao {
    @Insert
    void insert(WalkingSession wksession);

    @Query("DELETE FROM sessions")
    void deleteAll();

    @Query("SELECT * from sessions ORDER BY user_id ASC")
    LiveData<List<WalkingSession>> getAllSession();

}
