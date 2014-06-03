package com.treasure_data.androidsdk.apiclient;

import android.os.Parcel;
import android.os.Parcelable;

public class DbLogTableDescr extends DbTableDescr {
    private final String timeColumnName;

    public DbLogTableDescr(String dbName, String tableName,
                              String timeColumName) {
        super(dbName, tableName, DbTableDescr.TABLE_TYPE_LOG);
        this.timeColumnName = timeColumName;
    }

    // assigns timeColumnName to default 'time'
    public DbLogTableDescr(String dbName, String tableName) {
        this(dbName, tableName, "time");
    }

    // getters
    public String getTimeColumnName() {
        return timeColumnName;
    }

    @Override
    public String toString() {
        return (databaseName + "#" + tableName + "(log," + timeColumnName + ")");
    }

    //
    // implements the Parcelable interface to be able to serialize the object
    //  hence transferring it using an Intent
    //

    protected DbLogTableDescr(Parcel in) {
        super(in);
        timeColumnName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(timeColumnName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<DbLogTableDescr> CREATOR
            = new Parcelable.Creator<DbLogTableDescr>() {
        @Override
        public DbLogTableDescr createFromParcel(Parcel in) {
            return new DbLogTableDescr(in);
        }

        @Override
        public DbLogTableDescr[] newArray(int size) {
            return new DbLogTableDescr[size];
        }
    };
}