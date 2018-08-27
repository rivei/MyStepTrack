package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity (tableName = "walking_events")
public class WalkingEvent {

    @PrimaryKey
    public long WeTimestamp;

    @ColumnInfo (name = "transition")
    public int mTransition;

    @ColumnInfo (name = "elapsed_time")
    public long mElapsedTime;

    @Override
    public String toString(){
        return WeTimestamp + ", " +
                mTransition + ", " +
                mElapsedTime;
    }

}
