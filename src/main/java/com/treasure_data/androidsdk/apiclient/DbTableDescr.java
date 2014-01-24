package com.treasure_data.androidsdk.apiclient;

import java.security.InvalidParameterException;

import org.apache.commons.lang.builder.HashCodeBuilder;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class DbTableDescr implements Parcelable {
    private static final String TAG = DbTableDescr.class.getSimpleName();

    public static final int TABLE_TYPE_LOG = 0;
    public static final int TABLE_TYPE_ITEM = 1;
    public static int validateType(int type)
            throws InvalidParameterException {
        switch (type) {
        case (TABLE_TYPE_ITEM):
        case (TABLE_TYPE_LOG):
            return type;
        default:
            throw new InvalidParameterException(
                    "Invalid table type specified - " +
                    "only 'log' and 'item' are supported at this moment.");
        }
    }

    protected String mDbName;
    protected String mTableName;
    protected int mTableType;

    // pseudo-default constructor, defaults to log table
    protected DbTableDescr(String dbName, String tableName) {
        mDbName = dbName;
        mTableName = tableName;
        mTableType = TABLE_TYPE_LOG;
    }

    protected DbTableDescr(String dbName, String tableName, int type)
            throws InvalidParameterException {
        mDbName = dbName;
        mTableName = tableName;
        mTableType = validateType(type);
    };

    public String getDatabaseName() {
        return mDbName;
    }

    public String getTableName() {
        return mTableName;
    }

    public int getTableType() {
        return mTableType;
    }

    abstract public String toString();

    // override this method to be used in the hash key comparison inside HashMaps
    public boolean equals(Object obj) {
        if (hashCode() == obj.hashCode())
            return true;
        return false;
    }

    // override this method so that the hash key is created from the "db#table" string
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder();
        hash.append((mDbName + "#" + mTableName).toCharArray());
        return hash.toHashCode();
    }

    //
    // implements the Parcelable interface to be able to serialize the object
    //  hence transferring it using an Intent
    //

    protected DbTableDescr(Parcel in) throws InvalidParameterException {
        mDbName = in.readString();
        mTableName = in.readString();
        mTableType = validateType(in.readInt());
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mDbName);
        out.writeString(mTableName);
        out.writeInt(mTableType);
    }
}


