package com.example.diplomka;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DataModel extends SQLiteOpenHelper {
    protected static final String DB_NAME = "dbStreetData";
    protected static final int DB_VERSION = 4;

    public static final String ATR_ID = "id";
    public static final String ATR_PART = "part";

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
    public static final String ATR_INPUT = "user_input";

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
                ATR_NOISE + " REAL, " +
                ATR_PART + " INTEGER)";
        db.execSQL(query);

        query = "CREATE TABLE " + TBL_NAME_STREETS + " (" +
                ATR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ATR_FROM + " INTEGER, " +
                ATR_TO + " INTEGER, " +
                ATR_PART + " INTEGER, " +
                ATR_SIDEWALK + " INTEGER, " +
                ATR_SIDEWALK_WIDTH + " INTEGER, " +
                ATR_GREEN + " INTEGER, " +
                ATR_COMFORT + " INTEGER, " +
                ATR_INPUT + " INTEGER)";
        db.execSQL(query);
        db.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query;
        query = "DROP TABLE IF EXISTS " + TBL_NAME_POINTS;
        db.execSQL(query);
        query = "DROP TABLE IF EXISTS " + TBL_NAME_STREETS;
        db.execSQL(query);
        onCreate(db);
        db.close();
    }

    //---------------DATA POINTS--------------------------------------

    public void addDataPoints(long ts, int session, double lat, double lon, double noise, int part) {
        ContentValues values = new ContentValues();
        values.put(ATR_TS, ts);
        values.put(ATR_SESSION, session);
        values.put(ATR_LAT, lat);
        values.put(ATR_LON, lon);
        values.put(ATR_NOISE, noise);
        values.put(ATR_PART, part);

        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TBL_NAME_POINTS,null, values);
        db.close();
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
                ", " + ATR_LAT + "," + ATR_LON + ", " + ATR_NOISE + ", " + ATR_PART + " FROM " +
                TBL_NAME_POINTS, null);
        ArrayList<DataPoint> dataArrayList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                dataArrayList.add(new DataPoint(cursor.getInt(0), cursor.getInt(1),
                        cursor.getLong(2), cursor.getFloat(3),
                        cursor.getFloat(4), cursor.getFloat(5),
                        cursor.getInt(6)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataArrayList;
    }

    public ArrayList<DataPoint> getDataPoints(int session) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + ATR_ID + ", " + ATR_SESSION + ", " + ATR_TS +
                ", " + ATR_LAT + "," + ATR_LON + ", " + ATR_NOISE + ", " + ATR_PART + " FROM " +
                TBL_NAME_POINTS + " WHERE " + ATR_SESSION + " =?", new String[]{Integer.toString(session)});

        ArrayList<DataPoint> dataArrayList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                dataArrayList.add(new DataPoint(cursor.getInt(0), cursor.getInt(1),
                        cursor.getLong(2), cursor.getFloat(3),
                        cursor.getFloat(4), cursor.getFloat(5),
                        cursor.getInt(6)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataArrayList;
    }

    public ArrayList<DataPoint> getDataPoints(int session, int part) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + ATR_ID + ", " + ATR_SESSION + ", " + ATR_TS +
                ", " + ATR_LAT + "," + ATR_LON + ", " + ATR_NOISE + ", " + ATR_PART + " FROM " +
                TBL_NAME_POINTS + " WHERE " + ATR_SESSION + " =? AND " + ATR_PART + "=? ORDER BY " +
                ATR_TS, new String[]{Integer.toString(session), Integer.toString(part)});

        ArrayList<DataPoint> dataArrayList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                dataArrayList.add(new DataPoint(cursor.getInt(0), cursor.getInt(1),
                        cursor.getLong(2), cursor.getFloat(3),
                        cursor.getFloat(4), cursor.getFloat(5),
                        cursor.getInt(6)));
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

    public void updateDataPoints(int id, int part) {
        ContentValues values = new ContentValues();
        values.put(ATR_PART, part);

        SQLiteDatabase db = this.getWritableDatabase();
        db.update(TBL_NAME_POINTS, values, "id=?",new String[]{Integer.toString(id)});
        db.close();
    }

    public void updateSplitDataPoints(int session, long dt, int part, int newPart) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ATR_PART, newPart);

        db.update(TBL_NAME_POINTS, values, ATR_ID + " IN (SELECT " + ATR_ID +
                " FROM " + TBL_NAME_POINTS + " WHERE " + ATR_SESSION + "=? AND " + ATR_TS + ">=? AND " +
                ATR_PART + "=?)",
                new String[]{Integer.toString(session), Long.toString(dt), Integer.toString(part)});
        db.close();
    }

    public void deleteDataPoints() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_NAME_POINTS);
        db.close();
    }

    public void deleteSoloDataPoints() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_NAME_POINTS + " WHERE " + ATR_SESSION + " IN( SELECT " +
                ATR_SESSION + " FROM " + TBL_NAME_POINTS + " GROUP BY " + ATR_SESSION +
                " HAVING COUNT(*) == 1 )");
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

    public void addStreetData(int from, int to, int part, int sidewalk, int sidewalk_width,
                              int green, int comfort, int input) {
        ContentValues values = new ContentValues();
        values.put(ATR_FROM, from);
        values.put(ATR_TO, to);
        values.put(ATR_PART, part);
        values.put(ATR_SIDEWALK, sidewalk);
        values.put(ATR_SIDEWALK_WIDTH, sidewalk_width);
        values.put(ATR_GREEN, green);
        values.put(ATR_COMFORT, comfort);
        values.put(ATR_INPUT, input);

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

        Cursor cursor = db.rawQuery("SELECT " + ATR_ID + ", " + ATR_FROM + ", " + ATR_TO +
                ", " + ATR_PART + ", " + ATR_SIDEWALK + "," + ATR_SIDEWALK_WIDTH + ", " +
                ATR_GREEN + ", " + ATR_COMFORT + ", " + ATR_INPUT + " FROM " + TBL_NAME_STREETS, null);
        ArrayList<StreetData> dataArrayList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                dataArrayList.add(new StreetData(cursor.getInt(0), cursor.getInt(1),
                        cursor.getInt(2), cursor.getInt(3),
                        cursor.getInt(4), cursor.getInt(5),
                        cursor.getInt(6), cursor.getInt(7),
                        (cursor.getInt(8)) == 1));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataArrayList;
    }

    public ArrayList<StreetData> getStreetData(int session) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + ATR_ID + ", " + ATR_FROM + ", " + ATR_TO +
                ", " + ATR_PART + ", " + ATR_SIDEWALK + "," + ATR_SIDEWALK_WIDTH + ", " + ATR_GREEN
                + ", " + ATR_COMFORT + ", " + ATR_INPUT + " FROM " + TBL_NAME_STREETS + " WHERE " + ATR_FROM +
                " IN (SELECT " + ATR_ID + " FROM " + TBL_NAME_POINTS + " WHERE " + ATR_SESSION +
                " = ? ) ORDER BY " + ATR_FROM, new String[]{Integer.toString(session)});
        ArrayList<StreetData> dataArrayList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                dataArrayList.add(new StreetData(cursor.getInt(0), cursor.getInt(1),
                        cursor.getInt(2), cursor.getInt(3),
                        cursor.getInt(4), cursor.getInt(5),
                        cursor.getInt(6), cursor.getInt(7),
                        (cursor.getInt(8)) == 1));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return dataArrayList;
    }

    public void updateStreetData(int from, int to, int part) {
        ContentValues values = new ContentValues();
        values.put(ATR_FROM, from);
        values.put(ATR_TO, to);
        values.put(ATR_PART, part);

        SQLiteDatabase db = this.getWritableDatabase();
        db.update(TBL_NAME_STREETS, values, "point_from=? AND point_to=?",
                new String[]{Integer.toString(from), Integer.toString(to)});
        db.close();
    }

    public void updateSplitStreetData(int from_id, int part, int newPart) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT " + ATR_ID + ", " + ATR_FROM + ", " + ATR_TO +
                ", " + ATR_PART + ", " + ATR_SIDEWALK + "," + ATR_SIDEWALK_WIDTH + ", " +
                ATR_GREEN + ", " + ATR_COMFORT + ", " + ATR_INPUT + " FROM " + TBL_NAME_STREETS +
                " WHERE " + ATR_FROM + "<? AND " + ATR_TO + ">? AND " + ATR_PART + "=?",
                new String[]{Integer.toString(from_id), Integer.toString(from_id), Integer.toString(part)});
        ArrayList<StreetData> dataArrayList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                dataArrayList.add(new StreetData(cursor.getInt(0), cursor.getInt(1),
                        cursor.getInt(2), cursor.getInt(3),
                        cursor.getInt(4), cursor.getInt(5),
                        cursor.getInt(6), cursor.getInt(7),
                        (cursor.getInt(8)) == 1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        if (dataArrayList.size() > 1) {
            Log.v("DataModel", "Error split street data");
            return;
        }

        SQLiteDatabase dbw = this.getWritableDatabase();

        dataArrayList.get(0);
        ContentValues values = new ContentValues();
        values.put(ATR_TO, from_id);

        dbw.update(TBL_NAME_STREETS, values, ATR_ID + "=?",
                new String[]{Integer.toString(dataArrayList.get(0).id)});

        values = new ContentValues();
        values.put(ATR_FROM, from_id);
        values.put(ATR_TO, dataArrayList.get(0).to);
        values.put(ATR_PART, newPart);
        values.put(ATR_SIDEWALK, dataArrayList.get(0).sidewalk);
        values.put(ATR_SIDEWALK_WIDTH, dataArrayList.get(0).sidewalk_width);
        values.put(ATR_GREEN, dataArrayList.get(0).green);
        values.put(ATR_COMFORT, dataArrayList.get(0).comfort);
        values.put(ATR_INPUT, dataArrayList.get(0).isInput);

        dbw.insert(TBL_NAME_STREETS,null, values);
        dbw.close();
    }

    public void updateStreetData(int from, int to, int sidewalk, int sidewalk_width,
                                 int green, int comfort) {
        ContentValues values = new ContentValues();
        values.put(ATR_FROM, from);
        values.put(ATR_TO, to);
        values.put(ATR_SIDEWALK, sidewalk);
        values.put(ATR_SIDEWALK_WIDTH, sidewalk_width);
        values.put(ATR_GREEN, green);
        values.put(ATR_COMFORT, comfort);
        values.put(ATR_INPUT, 1);

        SQLiteDatabase db = this.getWritableDatabase();
        db.update(TBL_NAME_STREETS, values, "point_from=? AND point_to=?",
                new String[]{Integer.toString(from), Integer.toString(to)});
        db.close();
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

    public void deleteStreetDataBySession(int session) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_NAME_STREETS + " WHERE " + ATR_ID +
                " = " + session);
        db.close();
    }
}

