package ru.sawim.io;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Gerc on 05.05.14.
 */
public final class DatabaseHelper extends SQLiteOpenHelper {

    private String sqlCreateEntries;
    private String tableName;

    public DatabaseHelper(Context context, String baseName, String sqlCreateEntries, String tableName, int version) {
        super(context, baseName, null, version);
        this.sqlCreateEntries = sqlCreateEntries;
        this.tableName = tableName;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sqlCreateEntries);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTable(db);
    }

    public void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        onCreate(db);
    }
}
