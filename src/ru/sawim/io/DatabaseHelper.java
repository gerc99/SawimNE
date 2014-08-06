package ru.sawim.io;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by gerc on 03.08.2014.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sawim.db";
    private static final int DATABASE_VERSION = 1;

    public static final String ROW_AUTO_ID = "_id";
    public static final String ACCOUNT_ID = "account_id";
    public static final String CONTACT_ID = "contact_id";
    public static final String ROW_DATA = "row_data";

    public static final String TABLE_CHAT_HISTORY = "history";
    public static final String INCOMING = "incoming";
    public static final String SENDING_STATE = "sanding_state";
    public static final String AUTHOR = "author";
    public static final String MESSAGE = "msgtext";
    public static final String DATE = "date";

    private static final String CREATE_CHAT_HISTORY_TABLE = "create table if not exists "
            + TABLE_CHAT_HISTORY + " ("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + ACCOUNT_ID + " text not null, "
            + CONTACT_ID + " text not null, "
            + SENDING_STATE + " int, "
            + INCOMING + " int, "
            + AUTHOR + " text not null, "
            + MESSAGE + " text not null, "
            + DATE + " long not null, "
            + ROW_DATA + " int);";

    private String sqlCreateEntries;
    private String tableName;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public DatabaseHelper(Context context, String baseName, String sqlCreateEntries, String tableName, int version) {
        super(context, baseName, null, version);
        this.sqlCreateEntries = sqlCreateEntries;
        this.tableName = tableName;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CHAT_HISTORY_TABLE);
        if (sqlCreateEntries != null) {
            db.execSQL(sqlCreateEntries);
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        onCreate(db);
    }
}
