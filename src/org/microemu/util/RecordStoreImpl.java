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
                return 0;
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

    public void closeRecordStore() {
        if (!open) {
            return;
        }
        records.clear();
        open = false;
    }

    public String getName() {
        if (!open) {
            return null;
        }
        return recordStoreName;
    }

    public int getVersion() {
        synchronized (this) {
            return version;
        }
    }

    public int getNumRecords() {
        return size;
    }

    public int getSize() {
        if (!open) {
            return 0;
        }
        // TODO include size overhead such as the data structures used to hold the state of the record store
        // Preload all records
        rebuild();

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

    public int getSizeAvailable() {
        if (!open) {
            return 0;
        }
        return recordStoreManager.getSizeAvailable(this);
    }

    public long getLastModified() {
        synchronized (this) {
            return lastModified;
        }
    }

    public int getNextRecordID() {
        // lastRecordId needs to hold correct number, all records have to be preloaded
        rebuild();

        synchronized (this) {
            return lastRecordId + 1;
        }
    }

    public int addRecord(byte[] data, int offset, int numBytes) {
        if (data == null && numBytes > 0) {
            return -1;
        }
        if (numBytes > recordStoreManager.getSizeAvailable(this)) {
            return -1;
        }

        // lastRecordId needs to hold correct number, all records have to be preloaded
        rebuild();

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

        try {
            recordStoreManager.saveRecord(this, nextRecordID);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        rebuild();

        return nextRecordID;
    }

    public void deleteRecord(int recordId) {
        if (!open) {
            return;
        }

        synchronized (this) {
            // throws Exception when no record found
            getRecord(recordId);
            records.remove(new Integer(recordId));
            version++;
            lastModified = System.currentTimeMillis();
            size--;
        }

        try {
            recordStoreManager.deleteRecord(this, recordId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        rebuild();
    }

    public int getRecordSize(int recordId) {
        synchronized (this) {
            byte[] data = (byte[]) records.get(new Integer(recordId));
            if (data == null) {
                recordStoreManager.loadRecord(this, recordId);
                data = (byte[]) records.get(new Integer(recordId));
                if (data == null) {
                    return 0;
                }
            }
            return data.length;
        }
    }

    public int getRecord(int recordId, byte[] buffer, int offset) {
        int recordSize;
        synchronized (this) {
            recordSize = getRecordSize(recordId);
            System.arraycopy(records.get(new Integer(recordId)), 0, buffer, offset, recordSize);
        }

        return recordSize;
    }

    public byte[] getRecord(int recordId) {
        byte[] data;
        synchronized (this) {
            data = new byte[getRecordSize(recordId)];
            getRecord(recordId, data, 0);
        }
        return data.length < 1 ? null : data;
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes) {
        if (!open) {
            return;
        }

        // FIXME fixit
        if (numBytes > recordStoreManager.getSizeAvailable(this)) {
            return;
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
        try {
            recordStoreManager.saveRecord(this, recordId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        rebuild();
    }

    public int getHeaderSize() {
        // TODO fixit
        return recordStoreName.length() + 4 + 8 + 4;
    }

    public int getRecordHeaderSize() {
        return 4 + 4;
    }

    private Vector enumerationRecords = new Vector();
    private int currentRecord;

    public int numRecords() {
        return enumerationRecords.size();
    }

    public byte[] nextRecord()
            throws Exception {
        if (!isOpen() || currentRecord >= numRecords()) {
            return null;
        }
        byte[] result = ((EnumerationRecord) enumerationRecords.elementAt(currentRecord)).value;
        currentRecord++;
        return result;
    }

    public int nextRecordId()
            throws Exception {
        if (currentRecord >= numRecords()) {
            return -1;
        }
        int result = ((EnumerationRecord) enumerationRecords.elementAt(currentRecord)).recordId;
        currentRecord++;

        return result;
    }

    public byte[] previousRecord()
            throws Exception {
        if (!isOpen() || currentRecord < 0) {
            return null;
        }
        currentRecord--;
        byte[] result = ((EnumerationRecord) enumerationRecords.elementAt(currentRecord)).value;
        return result;
    }

    public int previousRecordId()
            throws Exception {
        if (currentRecord < 0) {
            return -1;
        }

        currentRecord--;
        int result = ((EnumerationRecord) enumerationRecords.elementAt(currentRecord)).recordId;

        return result;
    }

    public boolean hasNextElement() {
        return currentRecord != numRecords();
    }

    public boolean hasPreviousElement() {
        return currentRecord != 0;
    }

    public void reset() {
        currentRecord = 0;
    }

    public void rebuild() {
        enumerationRecords.removeAllElements();

        //
        // filter
        //
        synchronized (this) {
            try {
                int recordId = 1;
                int i = 0;
                while (i < getNumRecords()) {
                    try {
                        byte[] data = getRecord(recordId);
                        i++;
                        enumerationRecords.add(new EnumerationRecord(recordId, data));
                    } catch (Exception e) {
                    }
                    recordId++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class EnumerationRecord {
        int recordId;

        byte[] value;

        EnumerationRecord(int recordId, byte[] value) {
            this.recordId = recordId;
            this.value = value;
        }
    }
}