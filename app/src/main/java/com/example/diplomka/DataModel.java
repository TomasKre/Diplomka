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

    public static final String TBL_NAME = "tbStreeData";

    public static final String ATR_ID = "id";
    public static final String ATR_SESSION = "session";
    public static final String ATR_TS = "dt";
    public static final String ATR_LAT = "lat";
    public static final String ATR_LON = "lon";
    public static final String ATR_NOISE = "noise";

    public DataModel(Context ctx){
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query;

        query = "CREATE TABLE " + TBL_NAME + " (" +
                ATR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ATR_SESSION + " INTEGER, " +
                ATR_TS + " INTEGER, " +
                ATR_LAT + " REAL, " +
                ATR_LON + " REAL, " +
                ATR_NOISE + " REAL)";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query;
        query = "DROP TABLE IF EXISTS " + TBL_NAME;
        db.execSQL(query);
        onCreate(db);
    }

    public void addData(long ts, int session, double lat, double lon, double noise) {
        ContentValues values = new ContentValues();
        values.put(ATR_TS, ts);
        values.put(ATR_SESSION, session);
        values.put(ATR_LAT, lat);
        values.put(ATR_LON, lon);
        values.put(ATR_NOISE, noise);

        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TBL_NAME,null, values);

    }

    public long getDataCount() {
        SQLiteDatabase db = this.getReadableDatabase();

        long count = DatabaseUtils.queryNumEntries(db, TBL_NAME);

        db.close();
        return count;
    }

    public ArrayList<DataPoint> getData() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TBL_NAME, null);
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

    public ArrayList<String> getDataAsStrings() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TBL_NAME, null);
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

    public void deleteData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_NAME);
        db.close();
    }

    public void deleteDataById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_NAME + " WHERE " + ATR_ID +
                 " = " + id);
        db.close();
    }
}

