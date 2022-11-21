package com.example.diplomka;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DataModel extends SQLiteOpenHelper {
    protected static final String DB_NAME = "dbStreetData";
    protected static final int DB_VERSION = 1;

    public static final String ATR_ID = "id";

    public static final String TBL_NAME_POINTS = "tbDataPoints";
    public static final String ATR_SESSION = "session";
    public static final String ATR_TS = "dt";
    public static final String ATR_LAT = "lat";
    public static final String ATR_LON = "lon";
    public static final String ATR_NOISE = "noise";

    public static final String TBL_NAME_STREETS = "tbStreetData";
    public static final String ATR_FROM = "point_from";
    public static final String ATR_TO = "point_to";
    public static final String ATR_SIDEWALK = "sidewalk";
    public static final String ATR_SIDEWALK_WIDTH = "sidewalk_width";
    public static final String ATR_GREEN = "green";
    public static final String ATR_COMFORT = "comfort";

    public DataModel(Context ctx){
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query;

        query = "CREATE TABLE " + TBL_NAME_POINTS + " (" +
                ATR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ATR_SESSION + " INTEGER, " +
                ATR_TS + " INTEGER, " +
                ATR_LAT + " REAL, " +
                ATR_LON + " REAL, " +
                ATR_NOISE + " REAL)";
        db.execSQL(query);

        query = "CREATE TABLE " + TBL_NAME_STREETS + " (" +
                ATR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ATR_FROM + " INTEGER, " +
                ATR_TO + " INTEGER, " +
                ATR_SIDEWALK + " INTEGER, " +
                ATR_SIDEWALK_WIDTH + " INTEGER, " +
                ATR_GREEN + " INTEGER, " +
                ATR_COMFORT + " INTEGER)";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query;
        query = "DROP TABLE IF EXISTS " + TBL_NAME_POINTS;
        db.execSQL(query);
        query = "DROP TABLE IF EXISTS " + TBL_NAME_STREETS;
        db.execSQL(query);
        onCreate(db);
    }

    //---------------DATA POINTS--------------------------------------

    public void addDataPoints(long ts, int session, double lat, double lon, double noise) {
        ContentValues values = new ContentValues();
        values.put(ATR_TS, ts);
        values.put(ATR_SESSION, session);
        values.put(ATR_LAT, lat);
        values.put(ATR_LON, lon);
        values.put(ATR_NOISE, noise);

        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TBL_NAME_POINTS,null, values);
    }

    public long getDataPointsCount() {
        SQLiteDatabase db = this.getReadableDatabase();

        long count = DatabaseUtils.queryNumEntries(db, TBL_NAME_POINTS);

        db.close();
        return count;
    }

    public ArrayList<DataPoint> getDataPoints() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + ATR_ID + ", " + ATR_SESSION + ", " + ATR_TS +
                ", " + ATR_LAT + "," + ATR_LON + ", " + ATR_NOISE + " FROM " + TBL_NAME_POINTS, null);
        ArrayList<DataPoint> dataArrayList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                dataArrayList.add(new DataPoint(cursor.getInt(0), cursor.getInt(1),
                        cursor.getLong(2), cursor.getFloat(3),
                        cursor.getFloat(4), cursor.getFloat(5)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataArrayList;
    }

    public ArrayList<DataPoint> getDataPoints(int session) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + ATR_ID + ", " + ATR_SESSION + ", " + ATR_TS +
                ", " + ATR_LAT + "," + ATR_LON + ", " + ATR_NOISE + " FROM " + TBL_NAME_POINTS + " WHERE " +
                ATR_SESSION + " = " + session, null);
        ArrayList<DataPoint> dataArrayList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                dataArrayList.add(new DataPoint(cursor.getInt(0), cursor.getInt(1),
                        cursor.getLong(2), cursor.getFloat(3),
                        cursor.getFloat(4), cursor.getFloat(5)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataArrayList;
    }

    public ArrayList<String> getDataPointsAsStrings() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TBL_NAME_POINTS, null);
        ArrayList<String> dataArrayList = new ArrayList<>();

        DecimalFormat formatterGPS = new DecimalFormat("#0.000000");
        DecimalFormat formatterDB = new DecimalFormat("#0.00");
        String pattern = "dd.MM.yyyy hh:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        if (cursor.moveToFirst()) {
            do {
                String date = simpleDateFormat.format(new Date(cursor.getLong(2)));
                dataArrayList.add(cursor.getInt(1) + ") " + date.toString() + " – " +
                        formatterGPS.format(cursor.getDouble(3)) + ", " +
                        formatterGPS.format(cursor.getDouble(4)) + "; " +
                        formatterDB.format(cursor.getDouble(5)) + " dB");
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataArrayList;
    }

    public ArrayList<String> getGroupedDataPointsAsStrings() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + ATR_SESSION + ", MAX(" + ATR_TS + "), MIN(" +
                ATR_TS + "), COUNT(" + ATR_ID + ") FROM " + TBL_NAME_POINTS + " GROUP BY " + ATR_SESSION, null);
        ArrayList<String> dataArrayList = new ArrayList<>();

        DecimalFormat formatterDB = new DecimalFormat("#0.00");
        String pattern = "dd.MM.yyyy hh:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        if (cursor.moveToFirst()) {
            do {
                String dateEnd = simpleDateFormat.format(new Date(cursor.getLong(1)));
                String dateStart = simpleDateFormat.format(new Date(cursor.getLong(2)));
                dataArrayList.add(cursor.getInt(0) + ") " + dateStart.toString() + " – "
                        + dateEnd.toString() + ", Měření: " + cursor.getInt(3));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataArrayList;
    }

    public void deleteDataPoints() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_NAME_POINTS);
        db.close();
    }

    public void deleteDataPointsById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_NAME_POINTS + " WHERE " + ATR_ID +
                 " = " + id);
        db.close();
    }

    public void deleteDataPointsBySession(int session) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_NAME_POINTS + " WHERE " + ATR_SESSION +
                " = " + session);
        db.close();
    }

    //--------------------STREET DATA------------------------------------

    public void addStreetData(int from, int to, int sidewalk, int sidewalk_width, int green, int comfort) {
        ContentValues values = new ContentValues();
        values.put(ATR_FROM, from);
        values.put(ATR_TO, to);
        values.put(ATR_SIDEWALK, sidewalk);
        values.put(ATR_SIDEWALK_WIDTH, sidewalk_width);
        values.put(ATR_GREEN, green);
        values.put(ATR_COMFORT, comfort);

        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TBL_NAME_STREETS,null, values);
    }

    public long getStreetDataCount() {
        SQLiteDatabase db = this.getReadableDatabase();

        long count = DatabaseUtils.queryNumEntries(db, TBL_NAME_STREETS);

        db.close();
        return count;
    }

    public ArrayList<StreetData> getStreetData() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + ATR_ID + ", " + ATR_TO + ", " + ATR_FROM +
                ", " + ATR_SIDEWALK + "," + ATR_SIDEWALK_WIDTH + ", " + ATR_GREEN + ", " +
                ATR_COMFORT + " FROM " + TBL_NAME_STREETS, null);
        ArrayList<StreetData> dataArrayList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                dataArrayList.add(new StreetData(cursor.getInt(0), cursor.getInt(1),
                        cursor.getInt(2), cursor.getInt(3),
                        cursor.getInt(4), cursor.getInt(5),
                        cursor.getInt(6)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataArrayList;
    }

    public void deleteStreetData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_NAME_STREETS);
        db.close();
    }

    public void deleteStreetDataById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_NAME_STREETS + " WHERE " + ATR_ID +
                " = " + id);
        db.close();
    }
}

