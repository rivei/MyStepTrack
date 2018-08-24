package it.polimi.steptrack.roomdatabase.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity (tableName = "activities")
public class RecognizedActivity {

    @PrimaryKey
    public long RTimestamp;

    @ColumnInfo (name = "conf_foot")
    public int mConfOnFoot;
    @ColumnInfo (name = "conf_walking")
    public int mConfWalking;
    @ColumnInfo (name = "conf_Running")
    public int mConfRunning;

    @ColumnInfo (name = "conf_other")
    public int mConfOther;

    @ColumnInfo (name = "prob_activity")
    public int mProbActivity;

    @ColumnInfo (name = "conf_prob")
    public int mConfProb;
}
