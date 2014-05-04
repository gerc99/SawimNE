package ru.sawim.modules.history;

import android.content.ContentValues;
import android.util.Log;
import protocol.Contact;
import ru.sawim.comm.Util;
import ru.sawim.io.Storage;

import java.io.*;

public class HistoryStorage {

    private static final String PREFIX = "hist";

    private Contact contact;
    private String uniqueUserId;
    private Storage historyStore;
    private int currRecordCount = -1;

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

    private boolean openHistory(boolean create) {
        if (null == historyStore) {
            try {
                historyStore = new Storage(getRSName());
                //historyStore = new Storage(getRSName(),
                //        "create table if not exists messages (_id INTEGER PRIMARY KEY AUTOINCREMENT, incoming integer, author text not null, msgtext text not null, date longer );");
                historyStore.open();
            } catch (Exception e) {
                historyStore = null;
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void openHistory() {
        openHistory(false);
    }

    public void closeHistory() {
        if (null != historyStore) {
            historyStore.close();
        }
        historyStore = null;
        currRecordCount = -1;
    }

    synchronized void closeHistoryView() {
        closeHistory();
    }

    public synchronized void addText(final String text, final boolean incoming,
                                     final String from, final long gmtTime) {
        boolean isOpened = openHistory(true);
        if (!isOpened) {
            return;
        }
        byte type = (byte) (incoming ? 0 : 1);
        try {
            /*ContentValues values = new ContentValues();
            values.put("incoming", incoming ? 0 : 1);
            values.put("author", from);
            values.put("msgtext", text);
            values.put("date", Util.getLocalDateString(gmtTime, false));
            historyStore.addRecord("messages", values);*/
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream das = new DataOutputStream(baos);
            das.writeByte(type);
            das.writeUTF(from);
            das.writeUTF(text);
            das.writeUTF(Util.getLocalDateString(gmtTime, false));
            byte[] buffer = baos.toByteArray();
            historyStore.addRecord(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Storage getRS() {
        return historyStore;
    }

    private String getRSName() {
        return PREFIX + getUniqueUserId();
    }

    String getUniqueUserId() {
        return uniqueUserId;
    }

    public int getHistorySize() {
        if (currRecordCount < 0) {
            openHistory(false);
            currRecordCount = 0;
            try {
                if (null != historyStore) {
                    currRecordCount = historyStore.getNumRecords();
                }
            } catch (Exception e) {
                // do nothing
            }
        }
        return currRecordCount;
    }

    public CachedRecord getRecord(int recNo) {
        if (null == historyStore) {
            openHistory(false);
        }
        CachedRecord result = new CachedRecord();
        try {
            byte[] data = historyStore.getRecord(recNo + 1);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            result.type = dis.readByte();
            result.from = dis.readUTF();
            result.text = dis.readUTF();
            result.date = dis.readUTF();

        } catch (Exception e) {
            result.type = 0;
            result.from = "";
            result.text = "";
            result.date = "";
        }
        return result;
    }

    public void removeHistory() {
        closeHistory();
        removeRMS(getRSName());
    }

    private void removeRMS(String rms) {
        Storage.delete(rms);
    }

    public void dropTable() {
        historyStore.dropTable();
    }

    public void clearAll(boolean except) {
        closeHistory();
        String exceptRMS = (except ? getRSName() : null);
        String[] stores = Storage.getList();

        for (int i = 0; i < stores.length; ++i) {
            String store = stores[i];
            Log.e("gg", ""+store);
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
