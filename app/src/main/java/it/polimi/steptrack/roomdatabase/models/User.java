package it.polimi.steptrack.roomdatabase.models;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity (tableName = "users",
        indices = {@Index("uid")})
public class User {

    @NonNull
    @PrimaryKey
    public String uid = "";

    @ColumnInfo (name = "user_name")
    public String mUsername;

}

