/*
 *  MicroEmulator
 *  Copyright (C) 2001-2005 Bartek Teodorczyk <barteo@barteo.net>
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
 */

package org.microemu.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


public class RecordStoreImpl {
    private static final byte[] fileIdentifier = {0x4d, 0x49, 0x44, 0x52, 0x4d, 0x53};

    private static final byte versionMajor = 0x03;

    private static final byte versionMinor = 0x00;

    private int lastRecordId = 0;

    private int size = 0;

    private Hashtable records = new Hashtable();

    private String recordStoreName;

    private int version = 0;

    private long lastModified = 0;

    private transient boolean open;

    private transient AndroidRecordStoreManager recordStoreManager;

    private transient Vector recordListeners = new Vector();


    public RecordStoreImpl(AndroidRecordStoreManager recordStoreManager, String recordStoreName) {
        this.recordStoreManager = recordStoreManager;
        if (recordStoreName.length() <= 32) {
            this.recordStoreName = recordStoreName;
        } else {
            this.recordStoreName = recordStoreName.substring(0, 32);
        }
        this.open = false;
    }

    public RecordStoreImpl(AndroidRecordStoreManager recordStoreManager)
            throws IOException {
        this.recordStoreManager = recordStoreManager;
    }

    public int readHeader(DataInputStream dis)
            throws IOException {
        for (int i = 0; i < fileIdentifier.length; i++) {
            if (dis.read() != fileIdentifier[i]) {
                throw new IOException();
            }
        }
        dis.read(); // Major version number
        dis.read(); // Minor version number
        dis.read(); // Encrypted flag

        recordStoreName = dis.readUTF();
        lastModified = dis.readLong();
        version = dis.readInt();
        dis.readInt(); // TODO AuthMode
        dis.readByte(); // TODO Writable
        size = dis.readInt();

        return size;
    }

    public void readRecord(DataInputStream dis)
            throws IOException {
        int recordId = dis.readInt();
        if (recordId > lastRecordId) {
            lastRecordId = recordId;
        }
        dis.readInt(); // TODO Tag
        byte[] data = new byte[dis.readInt()];
        dis.read(data, 0, data.length);
        this.records.put(recordId, data);
    }

    public void writeHeader(DataOutputStream dos)
            throws IOException {
        dos.write(fileIdentifier);
        dos.write(versionMajor);
        dos.write(versionMinor);
        dos.write(0); // Encrypted flag

        dos.writeUTF(recordStoreName);
        dos.writeLong(lastModified);
        dos.writeInt(version);
        dos.writeInt(0); // TODO AuthMode
        dos.writeByte(0); // TODO Writable
        dos.writeInt(size);
    }

    public void writeRecord(DataOutputStream dos, int recordId)
            throws IOException {
        dos.writeInt(recordId);
        dos.writeInt(0); // TODO Tag
        try {
            byte[] data = getRecord(recordId);
            if (data == null) {
                dos.writeInt(0);
            } else {
                dos.writeInt(data.length);
                dos.write(data);
            }
        } catch (Exception e) {
            throw new IOException();
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void closeRecordStore()
            throws Exception {
        if (!open) {
            throw new Exception();
        }
        if (recordListeners != null) {
            recordListeners.removeAllElements();
        }
        records.clear();
        open = false;
    }

    public String getName()
            throws Exception {
        if (!open) {
            throw new Exception();
        }
        return recordStoreName;
    }

    public int getVersion()
            throws Exception {
        if (!open) {
            throw new Exception();
        }
        synchronized (this) {
            return version;
        }
    }

    public int getNumRecords()
            throws Exception {
        if (!open) {
            throw new Exception();
        }
        return size;
    }

    public int getSize()
            throws Exception {
        if (!open) {
            throw new Exception();
        }
        // TODO include size overhead such as the data structures used to hold the state of the record store
        // Preload all records
        enumerateRecords(false);

        int result = 0;
        Enumeration keys = records.keys();
        while (keys.hasMoreElements()) {
            int key = ((Integer) keys.nextElement()).intValue();
            try {
                byte[] data = getRecord(key);
                if (data != null) {
                    result += data.length;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public int getSizeAvailable()
            throws Exception {
        if (!open) {
            throw new Exception();
        }
        return recordStoreManager.getSizeAvailable(this);
    }

    public long getLastModified()
            throws Exception {
        if (!open) {
            throw new Exception();
        }

        synchronized (this) {
            return lastModified;
        }
    }

    public void addRecordListener(RecordListener listener) {
        if (!recordListeners.contains(listener)) {
            recordListeners.addElement(listener);
        }
    }

    public void removeRecordListener(RecordListener listener) {
        recordListeners.removeElement(listener);
    }

    public int getNextRecordID()
            throws Exception {
        if (!open) {
            throw new Exception();
        }
        // lastRecordId needs to hold correct number, all records have to be preloaded
        enumerateRecords(false);

        synchronized (this) {
            return lastRecordId + 1;
        }
    }

    public int addRecord(byte[] data, int offset, int numBytes)
            throws Exception {
        if (!open) {
            throw new Exception();
        }
        if (data == null && numBytes > 0) {
            throw new NullPointerException();
        }
        if (numBytes > recordStoreManager.getSizeAvailable(this)) {
            throw new Exception();
        }

        // lastRecordId needs to hold correct number, all records have to be preloaded
        enumerateRecords(false);

        byte[] recordData = new byte[numBytes];
        if (data != null) {
            System.arraycopy(data, offset, recordData, 0, numBytes);
        }

        int nextRecordID = getNextRecordID();
        synchronized (this) {
            records.put(nextRecordID, recordData);
            version++;
            lastModified = System.currentTimeMillis();
            lastRecordId++;
            size++;
        }

        recordStoreManager.saveRecord(this, nextRecordID);

        fireRecordListener(RecordListener.RECORD_ADD, nextRecordID);

        return nextRecordID;
    }


    public void deleteRecord(int recordId)
            throws Exception {
        if (!open) {
            throw new Exception();
        }

        synchronized (this) {
            // throws Exception when no record found
            getRecord(recordId);
            records.remove(new Integer(recordId));
            version++;
            lastModified = System.currentTimeMillis();
            size--;
        }

        recordStoreManager.deleteRecord(this, recordId);

        fireRecordListener(RecordListener.RECORD_DELETE, recordId);
    }


    public int getRecordSize(int recordId)
            throws Exception, Exception, Exception {
        if (!open) {
            throw new Exception();
        }

        synchronized (this) {
            byte[] data = (byte[]) records.get(new Integer(recordId));
            if (data == null) {
                recordStoreManager.loadRecord(this, recordId);
                data = (byte[]) records.get(new Integer(recordId));
                if (data == null) {
                    throw new Exception();
                }
            }

            return data.length;
        }
    }

    public int getRecord(int recordId, byte[] buffer, int offset)
            throws Exception {
        int recordSize;
        synchronized (this) {
            recordSize = getRecordSize(recordId);
            System.arraycopy(records.get(new Integer(recordId)), 0, buffer, offset, recordSize);
        }

        fireRecordListener(RecordListener.RECORD_READ, recordId);

        return recordSize;
    }


    public byte[] getRecord(int recordId)
            throws Exception {
        if (!open) {
            throw new Exception();
        }
        byte[] data;
        synchronized (this) {
            data = new byte[getRecordSize(recordId)];
            getRecord(recordId, data, 0);
        }
        return data.length < 1 ? null : data;
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
            throws Exception {
        if (!open) {
            throw new Exception();
        }

        // FIXME fixit
        if (numBytes > recordStoreManager.getSizeAvailable(this)) {
            throw new Exception();
        }
        byte[] recordData = new byte[numBytes];
        System.arraycopy(newData, offset, recordData, 0, numBytes);
        synchronized (this) {
            // throws Exception when no record found
            getRecord(recordId);
            records.put(recordId, recordData);
            version++;
            lastModified = System.currentTimeMillis();
        }
        recordStoreManager.saveRecord(this, recordId);
        fireRecordListener(RecordListener.RECORD_CHANGE, recordId);
    }

    public RecordEnumerationImpl enumerateRecords(boolean keepUpdated)
            throws Exception {
        if (!open) {
            throw new Exception();
        }
        return new RecordEnumerationImpl(this, keepUpdated);
    }

    public int getHeaderSize() {
        // TODO fixit
        return recordStoreName.length() + 4 + 8 + 4;
    }

    public int getRecordHeaderSize() {
        return 4 + 4;
    }

    private void fireRecordListener(int type, int recordId) {
        if (recordListeners != null) {
            for (Enumeration e = recordListeners.elements(); e.hasMoreElements(); ) {
                RecordListener l = (RecordListener) e.nextElement();
                switch (type) {
                        case RecordListener.RECORD_ADD:
                            l.recordAdded(this, recordId);
                            break;
                        case RecordListener.RECORD_CHANGE:
                            l.recordChanged(this, recordId);
                            break;
                        case RecordListener.RECORD_DELETE:
                            l.recordDeleted(this, recordId);
                    }
            }
        }
    }
}