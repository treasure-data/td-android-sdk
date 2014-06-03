package com.treasure_data.androidsdk.apiclient;

import java.security.InvalidParameterException;

import android.os.Parcel;
import android.os.Parcelable;

public class DbItemTableDescr extends DbTableDescr {
    private final String primaryKeyName;
    private final String primaryKeyType;

    // additional validator for primaryKeyType
    static private String validatePrimaryKeyType(String pkType)
            throws InvalidParameterException {
        validateName(pkType);
        if (!pkType.equals("int") && !pkType.equals("string")) {
            throw new InvalidParameterException(
                    "Invalid primary key specified '" + pkType + "'" +
                    " - only 'int' and 'string' supported at this moment.");
        }
        return pkType;
    }

    // constructors
    public DbItemTableDescr(String dbName, String tableName,
                          String primaryKeyName, String primaryKeyType)
            throws InvalidParameterException {
        super(dbName, tableName, DbTableDescr.TABLE_TYPE_ITEM);
        this.primaryKeyName = validateName(primaryKeyName);
        this.primaryKeyType = validatePrimaryKeyType(primaryKeyType);
    }

    // getters
    public String getPrimaryKeyName() {
        return primaryKeyName;
    }

    public String getPrimaryKeyType() {
        return primaryKeyType;
    }

    @Override
    public String toString() {
        return (databaseName + "#" + tableName +
                "(item," + primaryKeyName + ":" + primaryKeyType + ")");
    }

    //
    // implements the Parcelable interface to be able to serialize the object
    //  hence transferring it using an Intent
    //

    protected DbItemTableDescr(Parcel in) {
        super(in);
        primaryKeyName = in.readString();
        primaryKeyType = in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(primaryKeyName);
        out.writeString(primaryKeyType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<DbItemTableDescr> CREATOR
            = new Parcelable.Creator<DbItemTableDescr>() {
        @Override
        public DbItemTableDescr createFromParcel(Parcel in) {
            return new DbItemTableDescr(in);
        }

        @Override
        public DbItemTableDescr[] newArray(int size) {
            return new DbItemTableDescr[size];
        }
    };
}
