package ru.sawim.modules.history;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import protocol.Contact;
import ru.sawim.SawimApplication;
import ru.sawim.chat.Chat;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.Util;
import ru.sawim.io.DatabaseHelper;

public class HistoryStorage {

    private static final String CHAT_HISTORY_TABLE = "messages";
    private static final String COLUMN_ID = "_id";
    private static final String INCOMING = "incoming";
    private static final String AUTHOR = "author";
    private static final String MESSAGE = "msgtext";
    private static final String DATE = "date";
    private static final String DB_CREATE = "create table if not exists " +
            CHAT_HISTORY_TABLE + " (" + COLUMN_ID + " integer primary key autoincrement, " +
            INCOMING + " integer, " +
            AUTHOR + " text not null, " +
            MESSAGE + " text not null, " +
            DATE + " long not null );";

    private static final String PREFIX = "hist";

    private Contact contact;
    private String uniqueUserId;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public HistoryStorage(Contact contact) {
        this.contact = contact;
        uniqueUserId = contact.getUserId();
    }

    public Contact getContact() {
        return contact;
    }

    public static HistoryStorage getHistory(Contact contact) {
        return new HistoryStorage(contact);
    }

    public boolean openHistory() {
        if (null == dbHelper) {
            try {
                dbHelper = new DatabaseHelper(SawimApplication.getContext(), getDBName(), DB_CREATE, CHAT_HISTORY_TABLE, 3);
                db = dbHelper.getWritableDatabase();
            } catch (Exception e) {
                dbHelper = null;
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void closeHistory() {
        if (null != dbHelper) {
            dbHelper.close();
        }
        dbHelper = null;
    }

    public synchronized void addText(MessData md) {
        boolean isOpened = openHistory();
        if (!isOpened) {
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(INCOMING, md.isIncoming() ? 0 : 1);
            values.put(AUTHOR, md.getNick());
            values.put(MESSAGE, md.getText().toString());
            values.put(DATE, md.getTime());
            db.insert(CHAT_HISTORY_TABLE, null, values);
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

    public void fillFromHistory(Chat chat) {
        openHistory();
        try {
            Cursor cursor = db.query(CHAT_HISTORY_TABLE, null, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
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
}
