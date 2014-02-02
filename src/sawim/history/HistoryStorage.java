package sawim.history;

import protocol.Contact;
import sawim.comm.Util;
import sawim.io.Storage;

import javax.microedition.rms.RecordStore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class HistoryStorage {

    private static final String PREFIX = "hist";

    private Contact contact;
    private String uniqueUserId;
    private String storageName;
    private Storage historyStore;
    private int currRecordCount = -1;
    //private AndroidHistoryStorage androidStorage;

    public HistoryStorage(Contact contact) {
        this.contact = contact;
        uniqueUserId = contact.getUserId();
        storageName = getRSName();
        //androidStorage = new AndroidHistoryStorage(this);
    }

    public Contact getContact() {
        return contact;
    }

    //public AndroidHistoryStorage getAndroidStorage() {
    //    return androidStorage;
    //}

    public static HistoryStorage getHistory(Contact contact) {
        return new HistoryStorage(contact);
    }

    private boolean openHistory(boolean create) {
        if (null == historyStore) {
            try {
                historyStore = new Storage(storageName);
                historyStore.open(create);
            } catch (Exception e) {
                historyStore = null;
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

    public synchronized void addText(String text, boolean incoming,
                                     String from, long gmtTime) {
        boolean isOpened = openHistory(true);
        if (!isOpened) {
            return;
        }
        byte type = (byte) (incoming ? 0 : 1);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream das = new DataOutputStream(baos);
            das.writeByte(type);
            das.writeUTF(from);
            das.writeUTF(text);
            das.writeUTF(Util.getLocalDateString(gmtTime, false));
            byte[] buffer = baos.toByteArray();
            historyStore.addRecord(buffer);
        } catch (Exception e) {
            // do nothing
        }
        closeHistory();
        currRecordCount = -1;
        //androidStorage.addText(text, incoming, from, gmtTime);
    }

    RecordStore getRS() {
        if (historyStore == null) return null;
        return historyStore.getRS();
    }

    private String getRSName() {
        return Storage.getStorageName(PREFIX + getUniqueUserId());
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
        //currRecordCount = androidStorage.getHistorySize();
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
        //return androidStorage.getRecord(recNo);
    }

    public void removeHistory() {
        closeHistory();
        removeRMS(storageName);
    }

    private void removeRMS(String rms) {
        new Storage(rms).delete();
    }

    public void clearAll(boolean except) {
        closeHistory();
        String exceptRMS = (except ? storageName : null);
        String[] stores = Storage.getList();

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