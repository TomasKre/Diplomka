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

public class DataModelFinal extends SQLiteOpenHelper {
    protected static final String DB_NAME = "dbStreetData";
    protected static final int DB_VERSION = 1;

    public static final String TBL_NAME = "tbStreetData";

    public static final String ATR_ID = "id";
    public static final String ATR_FROM = "point_from";
    public static final String ATR_TO = "point_to";
    public static final String ATR_SIDEWALK = "sidewalk";
    public static final String ATR_SIDEWALK_WIDTH = "sidewalk_width";
    public static final String ATR_GREEN = "green";
    public static final String ATR_COMFORT = "comfort";

    public DataModelFinal(Context ctx){
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query;

        query = "CREATE TABLE " + TBL_NAME + " (" +
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
        query = "DROP TABLE IF EXISTS " + TBL_NAME;
        db.execSQL(query);
        onCreate(db);
    }

    public void addData(int from, int to, int sidewalk, int sidewalk_width, int green, int comfort) {
        ContentValues values = new ContentValues();
        values.put(ATR_FROM, from);
        values.put(ATR_TO, to);
        values.put(ATR_SIDEWALK, sidewalk);
        values.put(ATR_SIDEWALK_WIDTH, sidewalk_width);
        values.put(ATR_GREEN, green);
        values.put(ATR_COMFORT, comfort);

        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TBL_NAME,null, values);

    }

    public long getDataCount() {
        SQLiteDatabase db = this.getReadableDatabase();

        long count = DatabaseUtils.queryNumEntries(db, TBL_NAME);

        db.close();
        return count;
    }

    public ArrayList<StreetData> getData() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + ATR_ID + ", " + ATR_TO + ", " + ATR_FROM +
                ", " + ATR_SIDEWALK + "," + ATR_SIDEWALK_WIDTH + ", " + ATR_GREEN + ", " +
                ATR_COMFORT + " FROM " + TBL_NAME, null);
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

