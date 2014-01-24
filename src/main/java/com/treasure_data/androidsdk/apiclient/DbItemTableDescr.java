package com.treasure_data.androidsdk.apiclient;

import java.security.InvalidParameterException;

import android.os.Parcel;
import android.os.Parcelable;

public class DbItemTableDescr extends DbTableDescr {
    private String mPrimaryKeyName;
    private String mPrimaryKeyType;

    public DbItemTableDescr(String dbName, String tableName,
                               String primaryKeyName, String primaryKeyType)
        throws InvalidParameterException {

        super(dbName, tableName, DbTableDescr.TABLE_TYPE_ITEM);
        mPrimaryKeyName = primaryKeyName;
        if (!primaryKeyType.equals("int") && !primaryKeyType.equals("string")) {
            throw new InvalidParameterException(
                    "Invalid primary key specified - " +
                    "only 'int' and 'string' supported at this moment.");
        }
        mPrimaryKeyType = primaryKeyType;
    }

    public String getPrimaryKeyName() {
        return mPrimaryKeyName;
    }

    public String getPrimaryKeyType() {
        return mPrimaryKeyType;
    }

    public String toString() {
        return (mDbName + "#" + mTableName +
                "(item," + mPrimaryKeyName + ":" + mPrimaryKeyType + ")");
    }

    //
    // implements the Parcelable interface to be able to serialize the object
    //  hence transferring it using an Intent
    //

    protected DbItemTableDescr(Parcel in) {
        super(in);
        mPrimaryKeyName = in.readString();
        mPrimaryKeyType = in.readString();
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mPrimaryKeyName);
        out.writeString(mPrimaryKeyType);
    }

    // not really useful AFAIK
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<DbItemTableDescr> CREATOR
            = new Parcelable.Creator<DbItemTableDescr>() {
        public DbItemTableDescr createFromParcel(Parcel in) {
            return new DbItemTableDescr(in);
        }

        public DbItemTableDescr[] newArray(int size) {
            return new DbItemTableDescr[size];
        }
    };
}