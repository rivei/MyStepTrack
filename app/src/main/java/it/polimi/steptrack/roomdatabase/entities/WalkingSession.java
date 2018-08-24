package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;


@Entity (tableName = "sessions",
        foreignKeys = {@ForeignKey(entity = User.class,
                parentColumns = "uid",
                childColumns = "user_id",
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.CASCADE)},
        indices = {@Index(value = {"sid", "user_id"}, unique = true)}
)
public class WalkingSession {

    @PrimaryKey(autoGenerate = true)
    public int sid;

    @ColumnInfo(name = "user_id")
    public int userId;

    @ColumnInfo(name = "start_time")
    public long mStartTime;

    @ColumnInfo(name = "end_time")
    public long mEndTime;

    @ColumnInfo(name = "step_count")
    public long mStepCount;

    @ColumnInfo(name = "step_detect")
    public long mStepDetect;

    @ColumnInfo(name = "distance")
    public long mDistance;

    @ColumnInfo(name = "average_speed")
    public double mAverageSpeed;

    @ColumnInfo(name = "duration")
    public long mDuration;

    @ColumnInfo(name = "tag")
    public String mTag;


}
