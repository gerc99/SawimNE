/**
 *  MicroEmulator
 *  Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 *
 *  @version $Id: AndroidRecordStoreManager.java 2134 2009-08-27 10:30:20Z barteo $
 */

package org.microemu.util;

import android.content.Context;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class AndroidRecordStoreManager {

    private final static String RECORD_STORE_HEADER_SUFFIX = ".rsh";

    private final static String RECORD_STORE_RECORD_SUFFIX = ".rsr";

    private final static Object NULL_STORE = new Object();

    private Context context;

    private ConcurrentHashMap<String, Object> recordStores = null;

    public AndroidRecordStoreManager(Context context) {
        this.context = context;
    }

    public String getName() {
        return "Android record store";
    }

    private synchronized void initializeIfNecessary() {
        if (recordStores == null) {
            recordStores = new ConcurrentHashMap<String, Object>();
            String[] list = context.fileList();
            if (list != null && list.length > 0) {
                for (int i = 0; i < list.length; i++) {
                    if (list[i].endsWith(RECORD_STORE_HEADER_SUFFIX)) {
                        recordStores.put(
                                list[i].substring(0, list[i].length() - RECORD_STORE_HEADER_SUFFIX.length()),
                                NULL_STORE);
                    }
                }
            }
        }
    }

    public void deleteRecordStore(final String recordStoreName) {
        initializeIfNecessary();

        Object value = recordStores.get(recordStoreName);
        if (value == null) {
            return;
        }
        if (value instanceof RecordStoreImpl && ((RecordStoreImpl) value).isOpen()) {
            return;
        }

        RecordStoreImpl recordStoreImpl = null;
        try {
            DataInputStream dis = new DataInputStream(context.openFileInput(getHeaderFileName(recordStoreName)));
            recordStoreImpl = new RecordStoreImpl(this);
            recordStoreImpl.readHeader(dis);
            dis.close();
        } catch (IOException e) {
        }

        if (recordStoreImpl != null) {
            recordStoreImpl.setOpen(true);
            recordStoreImpl.rebuild();
            try {
                while (recordStoreImpl.hasNextElement()) {
                    if (recordStoreImpl.nextRecordId() != -1)
                        context.deleteFile(getRecordFileName(recordStoreName, recordStoreImpl.nextRecordId()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            recordStoreImpl.setOpen(false);
        }
        context.deleteFile(getHeaderFileName(recordStoreName));

        recordStores.remove(recordStoreName);
    }

    public RecordStoreImpl openRecordStore(String recordStoreName, boolean createIfNecessary) {
        initializeIfNecessary();

        RecordStoreImpl recordStoreImpl = null;
        try {
            DataInputStream dis = new DataInputStream(
                    context.openFileInput(getHeaderFileName(recordStoreName)));
            recordStoreImpl = new RecordStoreImpl(this);
            recordStoreImpl.readHeader(dis);
            recordStoreImpl.setOpen(true);
            dis.close();
        } catch (FileNotFoundException e) {
            if (!createIfNecessary) {
                return null;
            }
            recordStoreImpl = new RecordStoreImpl(this, recordStoreName);
            recordStoreImpl.setOpen(true);
            try {
                saveToDisk(recordStoreImpl, -1);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
        }

        recordStores.put(recordStoreName, recordStoreImpl);

        return recordStoreImpl;
    }

    public String[] listRecordStores() {
        initializeIfNecessary();
        String[] result = recordStores.keySet().toArray(new String[0]);
        if (result.length > 0) {
            return result;
        } else {
            return null;
        }
    }

    public void deleteRecord(RecordStoreImpl recordStoreImpl, int recordId)
            throws Exception {
        deleteFromDisk(recordStoreImpl, recordId);
    }

    public void loadRecord(RecordStoreImpl recordStoreImpl, int recordId) {
        try {
            DataInputStream dis = new DataInputStream(
                    context.openFileInput(getRecordFileName(recordStoreImpl.getName(), recordId)));
            if (dis.available() > 0) {
                recordStoreImpl.readRecord(dis);
                dis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveRecord(RecordStoreImpl recordStoreImpl, int recordId)
            throws Exception {
        saveToDisk(recordStoreImpl, recordId);
    }

    public void deleteStores() {
        String[] stores = listRecordStores();
        for (int i = 0; i < stores.length; i++) {
            String store = stores[i];
            try {
                deleteRecordStore(store);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void deleteFromDisk(RecordStoreImpl recordStore, int recordId)
            throws Exception {
        DataOutputStream dos = new DataOutputStream(
                context.openFileOutput(getHeaderFileName(recordStore.getName()), Context.MODE_PRIVATE));
        recordStore.writeHeader(dos);
        dos.close();

        context.deleteFile(getRecordFileName(recordStore.getName(), recordId));
    }

    /**
     * @param recordId -1 for storing only header
     */
    private synchronized void saveToDisk(RecordStoreImpl recordStore, int recordId)
            throws Exception {
        DataOutputStream dos = new DataOutputStream(
                context.openFileOutput(getHeaderFileName(recordStore.getName()), Context.MODE_PRIVATE));
        recordStore.writeHeader(dos);
        dos.close();

        if (recordId != -1) {
            dos = new DataOutputStream(
                    context.openFileOutput(getRecordFileName(recordStore.getName(), recordId), Context.MODE_PRIVATE));
            recordStore.writeRecord(dos, recordId);
            dos.close();
        }
    }

    public int getSizeAvailable(RecordStoreImpl recordStoreImpl) {
        // FIXME should return free space on device
        return 1024 * 1024;
    }

    private String getHeaderFileName(String recordStoreName) {
        return recordStoreName + RECORD_STORE_HEADER_SUFFIX;
    }

    private String getRecordFileName(String recordStoreName, int recordId) {
        return recordStoreName + "." + recordId + RECORD_STORE_RECORD_SUFFIX;
    }

}
