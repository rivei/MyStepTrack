package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity  (tableName = "geofencing_events")
public class GeoFencingEvent {
    @PrimaryKey
    public long GeTimestamp;

    @ColumnInfo(name = "transition")
    public int mTransition;

    @Override
    public String toString(){
        return GeTimestamp + ", " + mTransition;
    }

}
