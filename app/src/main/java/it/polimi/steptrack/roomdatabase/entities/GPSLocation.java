package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "locations",
//        foreignKeys = {@ForeignKey(entity = WalkingSession.class,
//        parentColumns = "sid",
//        childColumns = "session_id",
//        onUpdate = ForeignKey.CASCADE,
//        onDelete = ForeignKey.CASCADE)},
        indices = {@Index(value = {"GTimestamp","session_id"},unique = true)}
)
public class GPSLocation {
    @PrimaryKey
    public long GTimestamp;

    @ColumnInfo
    public int session_id;

    @ColumnInfo
    public double latitude;

    @ColumnInfo
    public double longitude;

}
