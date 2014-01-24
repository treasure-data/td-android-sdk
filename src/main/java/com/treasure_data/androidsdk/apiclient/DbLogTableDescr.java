package com.treasure_data.androidsdk.apiclient;

import java.security.InvalidParameterException;

import android.os.Parcel;
import android.os.Parcelable;

public class DbLogTableDescr extends DbTableDescr {
    private String mTimeColumnName;

    public DbLogTableDescr(String dbName, String tableName,
                              String timeColumName) {
        super(dbName, tableName, DbTableDescr.TABLE_TYPE_LOG);
        mTimeColumnName = timeColumName;
    }

    // assigns mTimeColumnName to default 'time'
    public DbLogTableDescr(String dbName, String tableName) {
        super(dbName, tableName, DbTableDescr.TABLE_TYPE_LOG);
        mTimeColumnName = "time";
    }

    // assigns mTimeColumnName to default 'time'. Reads database and table
    //  names from the String array. Checks if there at least 2 elements in
    //  the array before proceeding.
    public DbLogTableDescr(String[] dbTable)
            throws InvalidParameterException {
        super(dbTable[0], dbTable[1], DbTableDescr.TABLE_TYPE_LOG);
        mTimeColumnName = "time";
    }

    public String toString() {
        return (mDbName + "#" + mTableName + "(log," + mTimeColumnName + ")");
    }

    //
    // implements the Parcelable interface to be able to serialize the object
    //  hence transferring it using an Intent
    //

    protected DbLogTableDescr(Parcel in) {
        super(in);
        mTimeColumnName = in.readString();
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mTimeColumnName);
    }

    // not really useful AFAIK
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<DbLogTableDescr> CREATOR
            = new Parcelable.Creator<DbLogTableDescr>() {
        public DbLogTableDescr createFromParcel(Parcel in) {
            return new DbLogTableDescr(in);
        }

        public DbLogTableDescr[] newArray(int size) {
            return new DbLogTableDescr[size];
        }
    };
}