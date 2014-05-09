package ru.sawim.modules.history;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import protocol.Contact;
import ru.sawim.SawimApplication;
import ru.sawim.chat.Chat;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.Util;

public class HistoryStorage {

    private static final String CHAT_HISTORY_TABLE = "messages";
    private static final String COLUMN_ID = "_id";
    private static final String INCOMING = "incoming";
    private static final String SENDING_STATE = "sanding_state";
    private static final String AUTHOR = "author";
    private static final String MESSAGE = "msgtext";
    private static final String DATE = "date";
    private static final String DB_CREATE = "create table if not exists " +
            CHAT_HISTORY_TABLE + " (" + COLUMN_ID + " integer primary key autoincrement, " +
            SENDING_STATE + " integer, " +
            INCOMING + " integer, " +
            AUTHOR + " text not null, " +
            MESSAGE + " text not null, " +
            DATE + " long not null );";

    private static final String PREFIX = "hist";

    private Contact contact;
    private String uniqueUserId;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private HistoryStorage(Contact contact) {
        this.contact = contact;
        uniqueUserId = contact.getUserId();
    }

    public Contact getContact() {
        return contact;
    }

    public static HistoryStorage getHistory(Contact contact) {
        return new HistoryStorage(contact);
    }

    private boolean openHistory() {
        if (null == dbHelper) {
            try {
                dbHelper = new DatabaseHelper(SawimApplication.getContext(), getDBName(), DB_CREATE, CHAT_HISTORY_TABLE, 4);
                db = dbHelper.getWritableDatabase();
            } catch (Exception e) {
                dbHelper = null;
                e.printStackTrace();
            }
        }
        return dbHelper != null;
    }

    private void closeHistory() {
        if (null != dbHelper) {
            dbHelper.close();
        }
        dbHelper = null;
    }

    public synchronized void addText(MessData md) {
        if (!openHistory()) {
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(SENDING_STATE, (int) md.getIconIndex());
            values.put(INCOMING, md.isIncoming() ? 0 : 1);
            values.put(AUTHOR, md.getNick());
            values.put(MESSAGE, md.getText().toString());
            values.put(DATE, md.getTime());
            db.insert(CHAT_HISTORY_TABLE, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void updateText(MessData md) {
        if (!openHistory()) {
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(SENDING_STATE, (int) md.getIconIndex());
            values.put(INCOMING, md.isIncoming() ? 0 : 1);
            values.put(AUTHOR, md.getNick());
            values.put(MESSAGE, md.getText().toString());
            values.put(DATE, md.getTime());
            String where = AUTHOR + " = " + md.getNick() + " AND " + MESSAGE + " = " + md.getText().toString();
            db.update(CHAT_HISTORY_TABLE, values, where, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getHistorySize() {
        int num = 0;
        String selectCount = "SELECT COUNT(*) FROM " + CHAT_HISTORY_TABLE;
        openHistory();
        try {
            Cursor cursor = db.rawQuery(selectCount, new String[]{});
            if (cursor.moveToFirst()) {
                num = cursor.getInt(0);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return num;
    }

    private String getDBName() {
        return PREFIX + getUniqueUserId().replace('@', '_').replace('.', '_');
    }

    String getUniqueUserId() {
        return uniqueUserId;
    }

    public long getLastMessageTime() {
        openHistory();
        Cursor latest = db.query(CHAT_HISTORY_TABLE, new String[]{"MAX(" + DATE + ")"}, null, null, null, null, null);
        long lastMessageTime = 0;
        if (latest.moveToFirst()) {
            lastMessageTime = latest.getLong(0);
        }
        latest.close();
        return lastMessageTime;
    }

    public void fillFromHistory(Chat chat) {
        openHistory();
        try {
            Cursor cursor = db.query(CHAT_HISTORY_TABLE, null, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    int sendingState = cursor.getInt(cursor.getColumnIndex(SENDING_STATE));
                    boolean isIncoming = cursor.getInt(cursor.getColumnIndex(INCOMING)) == 0;
                    String from = cursor.getString(cursor.getColumnIndex(AUTHOR));
                    String text = cursor.getString(cursor.getColumnIndex(MESSAGE));
                    long date = Util.createLocalDate(Util.getLocalDateString(cursor.getLong(cursor.getColumnIndex(DATE)), false));
                    PlainMessage message;
                    if (isIncoming) {
                        message = new PlainMessage(from, chat.getProtocol(), date, text, true);
                    } else {
                        message = new PlainMessage(chat.getProtocol(), contact, date, text);
                    }
                    chat.addTextToForm(message, contact.isConference() ? from : chat.getFrom(message),
                            false, Chat.isHighlight(message.getProcessedText(), contact.getMyName()), false);
                    message.setSendingStateFromHistory(contact.getProtocol(), (byte) sendingState);
                } while (cursor.moveToNext());
            }
            cursor.close();
            closeHistory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeHistory() {
        closeHistory();
        removeRMS(getDBName());
    }

    private void removeRMS(String rms) {
        SawimApplication.getContext().deleteDatabase(rms);
    }

    public void dropTable() {
        dbHelper.dropTable(db);
    }

    public void clearAll(boolean except) {
        closeHistory();
        String exceptRMS = (except ? getDBName() : null);
        String[] stores = SawimApplication.getContext().databaseList();

        for (int i = 0; i < stores.length; ++i) {
            String store = stores[i];
            if (!store.startsWith(PREFIX)) {
                continue;
            }
            if (store.equals(exceptRMS)) {
                continue;
            }
            removeRMS(store);
        }
    }

    static class DatabaseHelper extends SQLiteOpenHelper {

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
            if (newVersion > oldVersion) {
            //    db.execSQL("ALTER TABLE " + tableName + " ADD " + SENDING_STATE + " integer");
            }
        }

        public void dropTable(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
            onCreate(db);
        }
    }
}
