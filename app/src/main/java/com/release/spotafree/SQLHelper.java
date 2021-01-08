package com.release.spotafree;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Downloads.db";
    public static final String TABLE_NAME = "Downloaded";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "NAME";
    public static final String COL_3 = "URL";

    public SQLHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
        SQLiteDatabase db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(" CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME STRING, URL STRING)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void insertData(String name, String url){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2,name);
        contentValues.put(COL_3,url);

        long res = db.insert(TABLE_NAME,null,contentValues);

    }

    public void deleteData(String name){
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = "NAME=?";
        String[] whereArgs = new String[]{name};
        db.delete(TABLE_NAME,whereClause,whereArgs);
    }

    public Cursor getAllData(){

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME,null);
        return res;

    }

    public void updateData(String score){

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2,score);
        db.update(TABLE_NAME,contentValues,"ID = ?", new String[]{"1"});

    }

}
