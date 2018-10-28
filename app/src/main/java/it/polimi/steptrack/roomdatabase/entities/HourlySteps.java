package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity  (tableName = "hourly_steps")
public class HourlySteps {
    @PrimaryKey(autoGenerate = true)
    public long hid;

    @ColumnInfo
    public long timestamp;

    @ColumnInfo
    public int steps;

    @Override
    public String toString(){
        return hid + ", " +
                timestamp + ", " +
                steps;
    }


}
