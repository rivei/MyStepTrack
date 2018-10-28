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

    @Query("SELECT * from sessions ORDER BY start_time DESC")
    LiveData<List<WalkingSession>> getAllSessions();

    @Query("SELECT * from sessions ORDER BY start_time ASC")
    List<WalkingSession> getAllSessionsSynchronous();

    @Query("SELECT * from sessions WHERE sid == :SessionId")
    LiveData<WalkingSession> getSession(int SessionId);
//
//    //get all session id from one day
//    @Query("SELECT sid FROM sessions WHERE start_time BETWEEN :startTime AND :endTime")
//    List<Long> getSessionsID(long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM sessions WHERE start_time BETWEEN :startTime AND :endTime")
    int getNumOfSessions(long startTime, long endTime);

    //total duration for one day
    @Query("SELECT SUM(duration) FROM sessions WHERE start_time BETWEEN :startTime AND :endTime")
    long getSumDuration(long startTime, long endTime);

    //total step count for one day
    @Query("SELECT SUM(step_detect) FROM sessions WHERE start_time BETWEEN :startTime AND :endTime")
    long getSumStepDetect(long startTime, long endTime);

    @Query("SELECT SUM(distance) FROM sessions WHERE start_time BETWEEN :startTime AND :endTime")
    float getSumDistance(long startTime, long endTime);

    @Query("SELECT AVG(average_speed) FROM sessions WHERE start_time BETWEEN :startTime AND :endTime")
    float getAvgSpeed(long startTime, long endTime);
//    @Query("SELECT SUM(stepCount) as total, AVG(stepCount) as average FROM userFitnessDailyRecords
// where forDay BETWEEN :startDay AND :endDay ORDER BY forDay ASC")
//    SumAveragePojo getUserFitnessSumAndAverageForLastThirtyDays(Date startDay, Date endDay);
}
