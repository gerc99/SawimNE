package ru.sawim.io;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ru.sawim.SawimApplication;

/**
 * Created by gerc on 03.08.2014.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private String sqlCreateEntries;
    private String tableName;

    public DatabaseHelper(Context context) {
        super(context, SawimApplication.DATABASE_NAME, null, SawimApplication.DATABASE_VERSION);
    }

    public DatabaseHelper(Context context, String baseName, String sqlCreateEntries, String tableName, int version) {
        super(context, baseName, null, version);
        this.sqlCreateEntries = sqlCreateEntries;
        this.tableName = tableName;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SawimProvider.CREATE_CHAT_HISTORY_TABLE);
        db.execSQL(SawimProvider.CREATE_UNREAD_MESSAGES_TABLE);
        db.execSQL(SawimProvider.CREATE_AVATAR_HASHES_TABLE);
        if (sqlCreateEntries != null) {
            db.execSQL(sqlCreateEntries);
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 1 && newVersion >= 2) {
            db.execSQL(SawimProvider.CREATE_UNREAD_MESSAGES_TABLE);
        }
        if (oldVersion <= 3 && newVersion >= 4) {
            db.execSQL(SawimProvider.CREATE_AVATAR_HASHES_TABLE);
        }
    }

    public void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        onCreate(db);
    }
}
