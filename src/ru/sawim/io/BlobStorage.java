package ru.sawim.io;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import ru.sawim.SawimApplication;
import ru.sawim.comm.StringConvertor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;


public final class BlobStorage {

    private static final String WHERE_ID = DatabaseHelper.ROW_AUTO_ID + " = ?";

    private String name;

    SqlAsyncTask thread = new SqlAsyncTask("BlobStorage");

    public static String[] getList() {
        Context context = SawimApplication.getInstance();
        return context.databaseList();
    }

    public void delete(final String tableName) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                try {
                    SawimApplication.getDatabaseHelper().getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + tableName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public BlobStorage(final String recordStoreName) {
        name = recordStoreName;
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                String CREATE_BLOB_TABLE = "create table if not exists "
                        + recordStoreName + " ("
                        + DatabaseHelper.ROW_AUTO_ID + " integer primary key autoincrement, "
                        + DatabaseHelper.ROW_DATA + " blob);";
                SawimApplication.getDatabaseHelper().getWritableDatabase().execSQL(CREATE_BLOB_TABLE);
            }
        });
    }

    public void open() {
        StorageConvertor.convertStorage(name, this);
    }

    public void close() {
        //SawimApplication.getDatabaseHelper().close();
    }

    public void addRecord(final byte data[]) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {

            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.ROW_DATA, data);
                SawimApplication.getDatabaseHelper().getWritableDatabase().insert(name, null, values);
            }
        });
    }

    public byte[] getRecord(int id) {
        Cursor cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().query(name, null,
                WHERE_ID, new String[]{String.valueOf(id)}, null, null, null);
        byte[] bytes = new byte[0];
        if (cursor.moveToFirst()) {
            bytes = cursor.getBlob(cursor.getColumnIndex(DatabaseHelper.ROW_DATA));
        }
        cursor.close();
        return bytes;
    }

    public void setRecord(final int id, final byte data[]) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.ROW_DATA, data);
                SawimApplication.getDatabaseHelper().getWritableDatabase().update(name, values, WHERE_ID, new String[]{String.valueOf(id)});
            }
        });
    }

    public void deleteRecord(final int id) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SawimApplication.getDatabaseHelper().getWritableDatabase().delete(name, WHERE_ID, new String[]{String.valueOf(id)});
            }
        });
    }

    public int getNumRecords() {
        String selectCount = "SELECT COUNT(*) FROM " + name;
        Cursor cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().rawQuery(selectCount, null);
        int num = 0;
        if (cursor.moveToFirst()) {
            num = cursor.getInt(0);
        }
        cursor.close();
        return num;
    }

    public boolean exist() {
        String[] recordStores = BlobStorage.getList();
        for (String recordStore : recordStores) {
            if (name.equals(recordStore)) {
                return true;
            }
        }
        return false;
    }

    public void saveListOfString(Vector<String> strings) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (int i = 0; i < strings.size(); ++i) {
                String str = strings.elementAt(i);
                dos.writeUTF(StringConvertor.notNull(str));
                addRecord(baos.toByteArray());
                baos.reset();
            }
            baos.close();
        } catch (Exception ignored) {
        }
    }

    public Vector loadListOfString() {
        Vector<String> strings = new Vector<String>(getNumRecords());
        try {
            for (int i = 0; i < getNumRecords(); ++i) {
                byte[] data = getRecord(i + 1);
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais);
                strings.addElement(dis.readUTF());
                bais.close();
            }
        } catch (Exception ignored) {
        }
        return strings;
    }

    public void saveXStatuses(String[] titles, String[] descs) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (int i = 0; i < titles.length; ++i) {
                dos.writeUTF(StringConvertor.notNull(titles[i]));
                dos.writeUTF(StringConvertor.notNull(descs[i]));
            }
            putRecord(1, baos.toByteArray());
        } catch (Exception ignored) {
        }
    }

    public void loadXStatuses(String[] titles, String[] descs) {
        try {
            byte[] buf = getRecord(1);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            DataInputStream dis = new DataInputStream(bais);
            for (int i = 0; i < titles.length; ++i) {
                titles[i] = StringConvertor.notNull(dis.readUTF());
                descs[i] = StringConvertor.notNull(dis.readUTF());
            }
        } catch (Exception ignored) {
        }
    }

    private void initRecords(int count) throws Exception {
        if (getNumRecords() < count) {
            if ((1 < count) && (0 == getNumRecords())) {
                byte[] version = StringConvertor.stringToByteArrayUtf8(SawimApplication.VERSION);
                addRecord(version);
            }
            while (getNumRecords() < count) {
                addRecord(new byte[0]);
            }
        }
    }

    private void putRecord(int num, byte[] data) throws Exception {
        initRecords(num);
        setRecord(num, data);
    }
}

