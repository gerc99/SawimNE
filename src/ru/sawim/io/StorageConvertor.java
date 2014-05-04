package ru.sawim.io;

import android.content.Context;
import android.util.Log;
import ru.sawim.SawimApplication;

import java.io.*;

final class StorageConvertor {
    private static final String LOG_TAG = "sawim-rms";
    private static final byte[] fileIdentifier = {0x4d, 0x49, 0x44, 0x52, 0x4d, 0x53};
    private final static String RECORD_STORE_HEADER_SUFFIX = ".rsh";
    private final static String RECORD_STORE_RECORD_SUFFIX = ".rsr";

    private StorageConvertor() { }

    static void convertStorage(String fromStorageName, Storage toStorage) {
        String normalizedName = normalizeStorageName(fromStorageName);
        if (isExists(normalizedName)) {
            Context context = SawimApplication.getContext();
            try {
                Log.d(LOG_TAG, "Converting " + fromStorageName);

                String headerFileName = normalizedName + RECORD_STORE_HEADER_SUFFIX;
                DataInputStream dis = new DataInputStream(context.openFileInput(headerFileName));

                // reading header
                for (byte identifier : fileIdentifier) {
                    if (dis.read() != identifier) {
                        throw new IOException();
                    }
                }
                dis.read(); // Major version number
                dis.read(); // Minor version number
                dis.read(); // Encrypted flag
                dis.readUTF();  // RS name
                dis.readLong(); // Last modified
                dis.readInt();  // Version
                dis.readInt();  // AuthMode
                dis.readByte(); // Writable
                int recordsCount = dis.readInt();  // Size

                Log.d(LOG_TAG, "recordsCount = " + recordsCount);

                for (int recordId = 0; recordId < recordsCount; ++recordId) {
                    String recordFileName = normalizedName + "." + (recordId + 1) + RECORD_STORE_RECORD_SUFFIX;

                    DataInputStream dataStream = new DataInputStream(context.openFileInput(recordFileName));
                    dataStream.readInt(); // Record ID
                    dataStream.readInt(); // Tag

                    int recordSize = dataStream.readInt();
                    byte data[] = new byte[recordSize];
                    dataStream.read(data);
                    dataStream.close();

                    toStorage.addRecord(data);
                    context.deleteFile(recordFileName);

                    Log.d(LOG_TAG, "Done converting record #" + (recordId + 1));
                }

                context.deleteFile(headerFileName);

                Log.d(LOG_TAG, "Done converting " + fromStorageName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String normalizeStorageName(String name) {
        return name.length() > 32 ? name.substring(0, 32) : name;
    }

    private static boolean isExists(String recordStoreName) {
        Context context = SawimApplication.getContext();
        String recordFileName = recordStoreName + RECORD_STORE_HEADER_SUFFIX;
        String[] fileNameList = context.fileList();
        for (String fileName : fileNameList) {
             if (fileName.equals(recordFileName)) {
                 return true;
             }
        }
        return false;
    }
}
- 
