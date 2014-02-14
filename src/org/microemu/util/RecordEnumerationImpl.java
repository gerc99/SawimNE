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

import java.lang.Exception;
import java.util.Vector;


public class RecordEnumerationImpl {
    private RecordStoreImpl recordStoreImpl;
    private boolean keepUpdated;

    private Vector enumerationRecords = new Vector();
    private int currentRecord;

    private RecordListener recordListener = new RecordListener() {

        public void recordAdded(RecordStoreImpl recordStore, int recordId) {
            rebuild();
        }

        public void recordChanged(RecordStoreImpl recordStore, int recordId) {
            rebuild();
        }

        public void recordDeleted(RecordStoreImpl recordStore, int recordId) {
            rebuild();
        }

    };

    public RecordEnumerationImpl(RecordStoreImpl recordStoreImpl, boolean keepUpdated) {
        this.recordStoreImpl = recordStoreImpl;
        this.keepUpdated = keepUpdated;

        rebuild();

        if (keepUpdated) {
            recordStoreImpl.addRecordListener(recordListener);
        }
    }

    public int numRecords() {
        return enumerationRecords.size();
    }

    public byte[] nextRecord()
            throws Exception, Exception, Exception {
        if (!recordStoreImpl.isOpen()) {
            throw new Exception();
        }

        if (currentRecord >= numRecords()) {
            throw new Exception();
        }

        byte[] result = ((EnumerationRecord) enumerationRecords.elementAt(currentRecord)).value;
        currentRecord++;

        return result;
    }

    public int nextRecordId()
            throws Exception {
        if (currentRecord >= numRecords()) {
            throw new Exception();
        }

        int result = ((EnumerationRecord) enumerationRecords.elementAt(currentRecord)).recordId;
        currentRecord++;

        return result;
    }

    public byte[] previousRecord()
            throws Exception {
        if (!recordStoreImpl.isOpen()) {
            throw new Exception();
        }
        if (currentRecord < 0) {
            throw new Exception();
        }

        currentRecord--;
        byte[] result = ((EnumerationRecord) enumerationRecords.elementAt(currentRecord)).value;

        return result;
    }

    public int previousRecordId()
            throws Exception {
        if (currentRecord < 0) {
            throw new Exception();
        }

        currentRecord--;
        int result = ((EnumerationRecord) enumerationRecords.elementAt(currentRecord)).recordId;

        return result;
    }

    public boolean hasNextElement() {
        if (currentRecord == numRecords()) {
            return false;
        } else {
            return true;
        }
    }

    public boolean hasPreviousElement() {
        if (currentRecord == 0) {
            return false;
        } else {
            return true;
        }
    }

    public void reset() {
        currentRecord = 0;
    }

    public void rebuild() {
        enumerationRecords.removeAllElements();

        //
        // filter
        //
        synchronized (recordStoreImpl) {
            try {
                int recordId = 1;
                int i = 0;
                while (i < recordStoreImpl.getNumRecords()) {
                    try {
                        byte[] data = recordStoreImpl.getRecord(recordId);
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

    public void keepUpdated(boolean keepUpdated) {
        if (keepUpdated) {
            if (!this.keepUpdated) {
                rebuild();
                recordStoreImpl.addRecordListener(recordListener);
            }
        } else {
            recordStoreImpl.removeRecordListener(recordListener);
        }

        this.keepUpdated = keepUpdated;
    }

    public boolean isKeptUpdated() {
        return keepUpdated;
    }


    public void destroy() {
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