package it.polimi.steptrack.roomdatabase.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import it.polimi.steptrack.roomdatabase.entities.WalkingSession;

@Dao
public interface WalkingSessionDao {
    @Insert
    long insert(WalkingSession wksession);

    @Update
    void update(WalkingSession wksession);

    @Query("DELETE FROM sessions")
    void deleteAll();

    @Query("SELECT * from sessions ORDER BY user_id ASC")
    LiveData<List<WalkingSession>> getAllSessions();

    @Query("SELECT * from sessions WHERE sid == :SessionId")
    LiveData<WalkingSession> getSession(int SessionId);
}
