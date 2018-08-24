package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

@Entity (tableName = "samples",
        foreignKeys = {@ForeignKey(entity = WalkingSession.class,
        parentColumns = "sid",
        childColumns = "session_id",
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE)},
        indices = {@Index(value = {"STimestamp","session_id"},unique = true)}
)
public class SensorSample {
    @PrimaryKey
    public long STimestamp;

    @ColumnInfo(name = "session_id")
    public int SessionID;

    @ColumnInfo(name = "acc_x")
    public float mAccX;

    @ColumnInfo(name = "acc_y")
    public float mAccY;

    @ColumnInfo(name = "acc_z")
    public float mAccZ;

    @ColumnInfo(name = "gyroscope_x")
    public float mGyrX;

    @ColumnInfo(name = "gyroscope_y")
    public float mGyrY;

    @ColumnInfo(name = "gyroscope_z")
    public float mGyrZ;

    @ColumnInfo(name = "magnetic_field_x")
    public float mMagX;

    @ColumnInfo(name = "magnetic_field_y")
    public float mMagY;

    @ColumnInfo(name = "magnetic_field_z")
    public float mMagZ;


}
