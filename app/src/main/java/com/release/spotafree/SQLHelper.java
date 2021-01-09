package com.release.spotafree;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "downloads.db";
    public static final String TABLE_NAME = "Downloaded";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "NAME";
    public static final String COL_3 = "PLAYLIST_URL";
    public static final String COL_4 = "PLAYLIST_FOLDER_DIR";
    public static final String COL_5 = "DUPLICATE_DIR";
    public static final String COL_6 = "LAST_INDEX";

    public SQLHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
        SQLiteDatabase db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(" CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME STRING, PLAYLIST_URL STRING," +
                "PLAYLIST_FOLDER_DIR STRING, DUPLICATE_DIR STRING, LAST_INDEX INTEGER)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void insertData(String name, String playlistDir, String duplicateDir, String playlistUrl){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2,name);
        contentValues.put(COL_3,playlistUrl);
        contentValues.put(COL_4,playlistDir);
        contentValues.put(COL_5,duplicateDir);
        contentValues.put(COL_6,0);

        long res = db.insert(TABLE_NAME,null,contentValues);
        if(res != -1){
            Log.d("MAIN_LOG","SUCCESSFULLY ADDED TO DB");
        }

    }

    public void deleteData(String name){
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = "NAME=?";
        String[] whereArgs = new String[]{name};
        db.delete(TABLE_NAME,whereClause,whereArgs);
    }

    public void updateLastIndex(String playlistUrl, int lastIndex){

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_6,lastIndex);
        db.update(TABLE_NAME,contentValues,"PLAYLIST_URL = ?", new String[]{playlistUrl});


    }


    public Cursor getAllData(){

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME,null);
        return res;

    }


}
