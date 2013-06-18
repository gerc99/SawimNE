



package sawim.history;


import sawim.cl.ContactList;
import sawim.io.Storage;
import protocol.Contact;

import javax.microedition.rms.RecordStore;




public class HistoryStorage {
    
    
    
    
    

    private static final String PREFIX = "hist";

    private Contact contact;
    private String uniqueUserId;
    private String storageName;
    private Storage historyStore;
    private int currRecordCount = -1;
    
    private AndroidHistoryStorage androidStorage;
    

    public HistoryStorage(Contact contact) {
        this.contact = contact;
        uniqueUserId = ContactList.getInstance().getProtocol(contact).getUniqueUserId(contact);
        storageName = getRSName();
        
        androidStorage = new AndroidHistoryStorage(this);
        
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
        
        androidStorage.addText(text, incoming, from, gmtTime);
        



















        
    }

    
    RecordStore getRS() {
        return historyStore.getRS();
    }

    
    private String getRSName() {
        return Storage.getStorageName(PREFIX + getUniqueUserId());
    }
    String getUniqueUserId() {
        return uniqueUserId;
    }

    
    public int getHistorySize() {
        
        currRecordCount = androidStorage.getHistorySize();
        











        
        return currRecordCount;
    }

    
    public CachedRecord getRecord(int recNo) {
        
        return androidStorage.getRecord(recNo);
        




















        
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


