package com.example.diplomka;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

public class DataModel extends SQLiteOpenHelper {
    protected static final String DB_NAME = "dbStreetData";
    protected static final int DB_VERSION = 1;

    public static final String TBL_NAME = "tbStreeData";

    public static final String ATR_ID = "id";
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

    public void addData(long ts, double lat, double lon, double noise) {
        ContentValues values = new ContentValues();
        values.put(ATR_TS, ts);
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
                dataArrayList.add(new DataPoint(cursor.getInt(0), cursor.getLong(1),
                        cursor.getFloat(2), cursor.getFloat(3), cursor.getFloat(4)));
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
        if (cursor.moveToFirst()) {
            do {
                Date date = new Date(cursor.getLong(1));
                dataArrayList.add(date + " – " +
                        formatterGPS.format(cursor.getDouble(2)) + ", " +
                        formatterGPS.format(cursor.getDouble(3)) + "; " +
                        formatterDB.format(cursor.getDouble(4)) + " dB");
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataArrayList;
    }

    public void deleteData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM "+ TBL_NAME);
        db.close();
    }
}

