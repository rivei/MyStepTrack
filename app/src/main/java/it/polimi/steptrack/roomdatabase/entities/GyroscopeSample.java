package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;


@Entity(tableName = "gyro_samples",
        foreignKeys = {@ForeignKey(entity = WalkingSession.class,
                parentColumns = "sid",
                childColumns = "session_id",
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.CASCADE)},
        indices = {@Index(value = {"GyTimestamp","session_id"},unique = true)}
)
public class GyroscopeSample {
    @PrimaryKey
    public long GyTimestamp;

    @ColumnInfo(name = "session_id")
    public long SessionID;

    @ColumnInfo(name = "gyro_x")
    public float mGyroX;

    @ColumnInfo(name = "gyro_y")
    public float mGyroY;

    @ColumnInfo(name = "gyro_z")
    public float mGyroZ;

    @Override
    public String toString(){
        return SessionID + ", " +
                GyTimestamp + "," +
                mGyroX + ", " +
                mGyroY + ", " +
                mGyroZ;
    }
}

