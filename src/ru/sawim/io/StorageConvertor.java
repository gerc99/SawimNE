package ru.sawim.io;

import android.content.Context;
import android.util.Log;
import protocol.Protocol;
import ru.sawim.SawimApplication;
import ru.sawim.comm.Util;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.roster.RosterHelper;

import java.io.*;

public final class StorageConvertor {
    private static final String LOG_TAG = "sawim-rms";
    private static final byte[] fileIdentifier = {0x4d, 0x49, 0x44, 0x52, 0x4d, 0x53};
    private final static String RECORD_STORE_HEADER_SUFFIX = ".rsh";
    private final static String RECORD_STORE_RECORD_SUFFIX = ".rsr";

    private StorageConvertor() { }

    static void convertStorage(String fromStorageName, Storage toStorage) {
        String normalizedName = normalizeStorageName(fromStorageName);
        if (isExists(normalizedName)) {
            Context context = SawimApplication.getContext();
            Log.d(LOG_TAG, "Converting " + fromStorageName);

            String headerFileName = normalizedName + RECORD_STORE_HEADER_SUFFIX;
            int recordsCount = readHeaderFile(headerFileName);
            Log.d(LOG_TAG, "recordsCount = " + recordsCount);

            for (int recordId = 0; recordId < recordsCount; ++recordId) {
                String recordFileName = normalizedName + "." + (recordId + 1) + RECORD_STORE_RECORD_SUFFIX;
                byte[] data = readFile(recordFileName);
                toStorage.addRecord(data);
                context.deleteFile(recordFileName);

                Log.d(LOG_TAG, "Done converting record #" + (recordId + 1));
            }
            context.deleteFile(headerFileName);
            Log.d(LOG_TAG, "Done converting " + fromStorageName);
        }
    }

    public static void historyConvert() {
        Context context = SawimApplication.getContext();
        final String PREFIX = "hist";
        for (int i = 0; i < context.fileList().length; ++i) {
            String fileName = context.fileList()[i];
            if (!fileName.startsWith(PREFIX)) {
                continue;
            }
            if (fileName.endsWith(RECORD_STORE_HEADER_SUFFIX)) {
                try {
                    String headerFileName = fileName;
                    int recordsCount = readHeaderFile(headerFileName);
                    Log.d(LOG_TAG, "recordsCount = " + recordsCount);

                    fileName = fileName.substring(0, fileName.length() - RECORD_STORE_HEADER_SUFFIX.length());
                    int count = RosterHelper.getInstance().getProtocolCount();
                    for (int pi = 0; pi < count; ++pi) {
                        Protocol p = RosterHelper.getInstance().getProtocol(pi);
                        String contactId = fileName.substring(PREFIX.length(), fileName.length());
                        HistoryStorage historyStorage = HistoryStorage.getHistory(p.getUserId(), contactId);
                        for (int recordId = 0; recordId < recordsCount; ++recordId) {
                            String recordFileName = fileName + "." + (recordId + 1) + RECORD_STORE_RECORD_SUFFIX;

                            byte[] data = readFile(recordFileName);

                            ByteArrayInputStream bais = new ByteArrayInputStream(data);
                            DataInputStream is = new DataInputStream(bais);
                            boolean isIncoming = is.readByte() == 0;
                            String from = is.readUTF();
                            String text = is.readUTF();
                            long date = Util.createLocalDate(is.readUTF());

                            historyStorage.addText(0, isIncoming, from, text, date, (short) 0);

                            is.close();
                            Log.d(LOG_TAG, "Done converting record #" + (recordId + 1));
                        }
                    }
                    for (int recordId = 0; recordId < recordsCount; ++recordId) {
                        String recordFileName = fileName + "." + (recordId + 1) + RECORD_STORE_RECORD_SUFFIX;
                        context.deleteFile(recordFileName);
                    }
                    context.deleteFile(headerFileName);
                    Log.d(LOG_TAG, "Done converting " + fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static int readHeaderFile(String headerFileName) {
        Context context = SawimApplication.getContext();
        int recordsCount = 0;
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(context.openFileInput(headerFileName));

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
            recordsCount = dis.readInt();  // Size
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException ignore) {
            }
        }
        return recordsCount;
    }

    private static byte[] readFile(String recordFileName) {
        Context context = SawimApplication.getContext();
        DataInputStream dis = null;
        byte data[] = new byte[0];
        try {
            dis = new DataInputStream(context.openFileInput(recordFileName));
            dis.readInt(); // Record ID
            dis.readInt(); // Tag

            int recordSize = dis.readInt();
            data = new byte[recordSize];
            dis.read(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException ignore) {
            }
        }
        return data;
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
