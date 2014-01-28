package com.treasure_data.androidsdk.apiclient;

import java.security.InvalidParameterException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class DbTableDescr implements Parcelable {
    //private static final String TAG = DbTblDescr.class.getSimpleName();

    public static final int TABLE_TYPE_LOG = 0;
    public static final int TABLE_TYPE_ITEM = 1;

    protected final String databaseName;
    protected final String tableName;
    protected final int tableType;

    // validators
    private static int validateType(int type)
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

    protected static String validateName(String name)
            throws InvalidParameterException {
        if (name == null)
            throw new InvalidParameterException("Name cannot be 'null'");
        if (name == "")
            throw new InvalidParameterException("Name cannot be an empty string");
        Pattern pattern = Pattern.compile("^[a-z0-9_]+$");
        Matcher matcher = pattern.matcher(name);
        if(!matcher.find())
            throw new InvalidParameterException(
                    "Name can only be composed of lower case letters, numbers, and _");
        return name;
    }

    // constructors
    // baseline constructor, validates db and table name as well as table type
    protected DbTableDescr(String dbName, String tableName, int type)
            throws InvalidParameterException {
        this.databaseName = validateName(dbName);
        this.tableName = validateName(tableName);
        this.tableType = validateType(type);
    };

    // pseudo-default constructor, defaults to log table
    protected DbTableDescr(String dbName, String tableName)
            throws InvalidParameterException {
        this(dbName, tableName, TABLE_TYPE_LOG);
    }

    // getters
    public String getDatabaseName() {
        return databaseName;
    }

    public String getTableName() {
        return tableName;
    }

    public int getTableType() {
        return tableType;
    }

    @Override
    abstract public String toString();

//    // override this method to be used in the hash key comparison inside HashMaps
//    @Override
//    public boolean equals(Object obj) {
//
//
//        if (hashCode() == obj.hashCode())
//            return true;
//        return false;
//    }
//
//    // override this method so that the hash key is created from the "db#table" string
//    @Override
//    public int hashCode() {
//        HashCodeBuilder hash = new HashCodeBuilder();
//        hash.append((databaseName + "#" + tableName).toCharArray());
//        return hash.toHashCode();
//    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((databaseName == null) ? 0 : databaseName.hashCode());
        result = prime * result
                + ((tableName == null) ? 0 : tableName.hashCode());
        result = prime * result + tableType;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DbTableDescr other = (DbTableDescr) obj;
        // databaseName should really never be null by constructor
        if (databaseName == null) {
            if (other.databaseName != null)
                return false;
        } else if (!databaseName.equals(other.databaseName))
            return false;
        // tableName should really never be null by constructor
        if (tableName == null) {
            if (other.tableName != null)
                return false;
        } else if (!tableName.equals(other.tableName))
            return false;
        if (tableType != other.tableType)
            return false;
        return true;
    }

    //
    // implements the Parcelable interface to be able to serialize the object
    //  hence transferring it using an Intent
    //

    protected DbTableDescr(Parcel in) throws InvalidParameterException {
        databaseName = in.readString();
        tableName = in.readString();
        tableType = validateType(in.readInt());
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(databaseName);
        out.writeString(tableName);
        out.writeInt(tableType);
    }
}
