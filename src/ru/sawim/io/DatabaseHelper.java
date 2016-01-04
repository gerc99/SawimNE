package ru.sawim.io;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ru.sawim.SawimApplication;

/**
 * Created by gerc on 03.08.2014.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String TABLE_CHAT_HISTORY = "history";

    public static final String ROW_AUTO_ID = "_id";
    public static final String ACCOUNT_ID = "account_id";
    public static final String GROUP_NAME = "group_name";
    public static final String GROUP_ID = "group_id";
    public static final String GROUP_IS_EXPAND = "group_is_expand";
    public static final String CONTACT_ID = "contact_id";
    public static final String CONTACT_NAME = "contact_name";
    public static final String STATUS = "status";
    public static final String STATUS_TEXT = "status_text";
    public static final String IS_CONFERENCE = "is_conference";
    public static final String CONFERENCE_MY_NAME = "conference_my_name";
    public static final String CONFERENCE_IS_AUTOJOIN = "is_conference_autojoin";
    public static final String ROW_DATA = "row_data";
    public static final String UNREAD_MESSAGES_COUNT = "unread_messages_count";
    public static final String AVATAR_HASH = "avatar_hash";
    public static final String FIRST_SERVER_MESSAGE_ID = "first_server_msg_id";
    public static final String SUB_CONTACT_RESOURCE = "sub_contact_resources";
    public static final String SUB_CONTACT_STATUS = "sub_contact_status";
    public static final String SUB_CONTACT_PRIORITY = "sub_contact_priority";
    public static final String SUB_CONTACT_PRIORITY_A = "sub_contact_priority_a";

    public static final String INCOMING = "incoming";
    public static final String SENDING_STATE = "sanding_state";
    public static final String AUTHOR = "author";
    public static final String MESSAGE = "msgtext";
    public static final String DATE = "date";

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

    public DatabaseHelper(Context context) {
        super(context, SawimApplication.DATABASE_NAME, null, SawimApplication.DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CHAT_HISTORY_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion <= 10) {
            try {
                db.execSQL("DROP TABLE IF EXISTS " + RosterStorage.subContactsTable);
                db.execSQL("DROP TABLE IF EXISTS " + RosterStorage.storeName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
