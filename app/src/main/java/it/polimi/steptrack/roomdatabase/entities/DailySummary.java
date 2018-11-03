package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "daily_summaries",
//        foreignKeys = {@ForeignKey(entity = User.class,
//                parentColumns = "uid",
//                childColumns = "user_id",
//                onUpdate = ForeignKey.CASCADE,
//                onDelete = ForeignKey.CASCADE)},
        indices = {@Index(value = {"did", "user_id"}, unique = true)}
)
public class DailySummary {
    @PrimaryKey(autoGenerate = true)
    public long did;

    @ColumnInfo(name = "user_id")
    public int userId;

    @ColumnInfo(name = "report_date")
    public Date mDate;

    @ColumnInfo
    public int numWalkingSessions;

    @ColumnInfo
    public long walkingduration;

    @ColumnInfo
    public float distance;

    @ColumnInfo
    public int steps;

    @ColumnInfo
    public float speed;

    @ColumnInfo
    public long stepDetect;

    @ColumnInfo
    public long reportTime;

//    public DailySummary(){}
//
//    public DailySummary(int userId, Date mDate, int numWalkingSessions, long walkingduration,
//                        float distance, int steps, float speed, long stepDetect, long reportTime) {
//        this.userId = userId;
//        this.mDate = mDate;
//        this.numWalkingSessions = numWalkingSessions;
//        this.walkingduration = walkingduration;
//        this.distance = distance;
//        this.steps = steps;
//        this.speed = speed;
//        this.stepDetect = stepDetect;
//        this.reportTime = reportTime;
//    }

    @Override
    public String toString(){
        return userId + "," +
                did + "," +
                mDate + "," +
                numWalkingSessions + "," +
                walkingduration + "," +
                distance + "," +
                speed + "," +
                steps + "," +
                stepDetect + "," +
                reportTime;
    }
}
