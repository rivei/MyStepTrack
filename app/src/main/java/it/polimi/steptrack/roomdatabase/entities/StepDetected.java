package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity  (tableName = "step_detected")
public class StepDetected {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo
    public long session_id;

    @ColumnInfo
    public long timestamp;

    @ColumnInfo
    public int steps;

    @Override
    public String toString(){
        return session_id + ", " +
               timestamp + ", " +
               steps;
    }

}
