package ru.sawim.io;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import ru.sawim.SawimApplication;

/**
 * Created by gerc on 03.09.2014.
 */
public class SawimProvider extends ContentProvider {

    private static final String TABLE_CHAT_HISTORY = "history";
    private static final String TABLE_UNREAD_MESSAGES = "history_unread_messages";
    private static final String TABLE_AVATAR_HASHES = "history_avatar_hashes";

    public static final String ROW_AUTO_ID = "_id";
    public static final String ACCOUNT_ID = "account_id";
    public static final String CONTACT_ID = "contact_id";
    public static final String ROW_DATA = "row_data";

    public static final String INCOMING = "incoming";
    public static final String SENDING_STATE = "sanding_state";
    public static final String AUTHOR = "author";
    public static final String MESSAGE = "msgtext";
    public static final String DATE = "date";

    public static final String UNREAD_MESSAGES_COUNT = "unread_messages_count";

    public static final String AVATAR_HASH = "avatar_hash";

    public static final String CREATE_CHAT_HISTORY_TABLE = "create table if not exists "
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

    public static final String CREATE_UNREAD_MESSAGES_TABLE = "create table if not exists "
            + TABLE_UNREAD_MESSAGES + " ("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + ACCOUNT_ID + " text not null, "
            + CONTACT_ID + " text not null, "
            + UNREAD_MESSAGES_COUNT + " int);";

    public static final String CREATE_AVATAR_HASHES_TABLE = "create table if not exists "
            + TABLE_AVATAR_HASHES + " ("
            + ROW_AUTO_ID + " integer primary key autoincrement, "
            + CONTACT_ID + " text not null, "
            + AVATAR_HASH + " text not null);";

    private static final int URI_HISTORY = 1;
    private static final int URI_UNREAD_MESSAGES = 2;
    private static final int URI_AVATAR_HASHES = 3;

    public static Uri HISTORY_RESOLVER_URI = Uri.parse("content://"
            + SawimApplication.AUTHORITY + "/" + TABLE_CHAT_HISTORY);
    public static Uri HISTORY_UNREAD_MESSAGES_RESOLVER_URI = Uri.parse("content://"
            + SawimApplication.AUTHORITY + "/" + TABLE_UNREAD_MESSAGES);
    public static Uri AVATAR_HASHES_RESOLVER_URI = Uri.parse("content://"
            + SawimApplication.AUTHORITY + "/" + TABLE_AVATAR_HASHES);

    DatabaseHelper dbHelper;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(SawimApplication.AUTHORITY, TABLE_CHAT_HISTORY, URI_HISTORY);
        uriMatcher.addURI(SawimApplication.AUTHORITY, TABLE_UNREAD_MESSAGES, URI_UNREAD_MESSAGES);
        uriMatcher.addURI(SawimApplication.AUTHORITY, TABLE_AVATAR_HASHES, URI_AVATAR_HASHES);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String table = getTableName(uri);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase sqLiteDB = dbHelper.getWritableDatabase();
        long rowId = sqLiteDB.insert(getTableName(uri), null, values);
        Uri resultUri = ContentUris.withAppendedId(uri, rowId);
        getContext().getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase sqLiteDB = dbHelper.getWritableDatabase();
        int rows = sqLiteDB.delete(getTableName(uri), selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return rows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase sqLiteDB = dbHelper.getWritableDatabase();
        int rows = sqLiteDB.update(getTableName(uri), values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return rows;
    }

    private static String getTableName(Uri uri) {
        String table;
        switch (uriMatcher.match(uri)) {
            case URI_HISTORY:
                table = TABLE_CHAT_HISTORY;
                break;
            case URI_UNREAD_MESSAGES:
                table = TABLE_UNREAD_MESSAGES;
                break;
            case URI_AVATAR_HASHES:
                table = TABLE_AVATAR_HASHES;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        return table;
    }
}