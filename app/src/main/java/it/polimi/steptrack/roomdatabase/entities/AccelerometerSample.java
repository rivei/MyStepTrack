package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;


@Entity(tableName = "acc_samples",
//        foreignKeys = {@ForeignKey(entity = WalkingSession.class,
//                parentColumns = "sid",
//                childColumns = "session_id",
//                onUpdate = ForeignKey.CASCADE,
//                onDelete = ForeignKey.CASCADE)},
        indices = {@Index(value = {"AsTimestamp","session_id"},unique = true)}
)
public class AccelerometerSample {
    @PrimaryKey
    public long AsTimestamp;

    @ColumnInfo(name = "session_id")
    public int SessionID;

    @ColumnInfo(name = "acc_x")
    public float mAccX;

    @ColumnInfo(name = "acc_y")
    public float mAccY;

    @ColumnInfo(name = "acc_z")
    public float mAccZ;

//    public AccelerometerSample(int sessionID) {
//        SessionID = sessionID;
//    }
}

