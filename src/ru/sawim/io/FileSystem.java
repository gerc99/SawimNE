package ru.sawim.io;

import android.os.Environment;
import protocol.net.TcpSocket;
import ru.sawim.SawimException;

import java.io.*;

public class FileSystem {

    public static final String HISTORY = "history";
    public static final String RES = "res/";
    public static final String AVATARS = ".avatars/";

    File file;

    public static String getSawimHome() {
        return "/sawimne/";
    }

    public static FileSystem getInstance() {
        return new FileSystem();
    }

    public static File openDir(String dir) {
        File file = null;
        FileSystem fileSystem = new FileSystem();
        String path = fileSystem.getCardDir().getAbsolutePath() + FileSystem.getSawimHome() + dir;
        try {
            fileSystem.openFile(path);
            file = fileSystem.getFile();
            if (!file.exists())
                file.mkdirs();
        } catch (SawimException e) {
            e.printStackTrace();
        }
        return file;
    }

    public InputStream openSawimFile(String file) {
        byte[] buffer;
        try {
            openFile(getCardDir().getAbsolutePath() + getSawimHome() + RES + file);
            InputStream in = openInputStream();
            buffer = new byte[in.available()];
            in.read(buffer);
            TcpSocket.close(in);
        } catch (Exception e) {
            buffer = null;
        }
        close();
        if (null != buffer) {
            return new ByteArrayInputStream(buffer);
        }
        return null;
    }

    public void openFile(String path) throws SawimException {
        try {
            file = new File(path);
        } catch (SecurityException e) {
            file = null;
            throw new SawimException(193, 1);
        } catch (Exception e) {
            file = null;
            e.printStackTrace();
            throw new SawimException(191, 1);
        }
    }

    public void mkdir(String path) {
        try {
            file = new File(getCardDir().getAbsolutePath(), path);
            try {
                if (!file.mkdir()) {
                    throw new IOException("Can't create directory " + file.getAbsolutePath());
                }
            } finally {
                close();
            }
        } catch (IOException e) {
        }
    }

    public boolean exists() {
        return (null != file) && file.exists();
    }

    public static File getCardDir() {
        File ext = Environment.getExternalStorageDirectory();
        if (isAccessible(ext)) return ext;
        if (isAccessible(new File("/sdcard"))) return new File("/sdcard");
        if (isAccessible(new File("/mnt/sdcard"))) return new File("/mnt/sdcard");
        if (isAccessible(new File("/mnt/ext_card"))) return new File("/mnt/ext_card");
        return ext;
    }

    private static boolean isAccessible(File file) {
        if ((null == file) || !file.exists() || file.isHidden() || !file.isDirectory() || !file.canRead()) {
            return false;
        }
        return true;
    }

    public File getFile() {
        return file;
    }

    public InputStream openInputStream() throws IOException {
        return new FileInputStream(file);
    }

    public OutputStream openOutputStream() throws Exception {
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Unable to delete " + file.getAbsolutePath());
            }
        }
        if (!file.createNewFile()) {
            throw new IOException("File already exists  " + file.getAbsolutePath());
        }
        return new FileOutputStream(file);
    }

    public OutputStream openForAppendOutputStream() throws Exception {
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("File already exists  " + file.getAbsolutePath());
            }
        }
        return new FileOutputStream(file, true);
    }

    public String getAbsolutePath() throws Exception {
        return file.getAbsolutePath();
    }

    public void close() {
        file = null;
    }

    public long fileSize() throws Exception {
        return (null == file) ? -1 : file.length();
    }

    public String getName() {
        return (null == file) ? null : file.getName();
    }

    public byte[] getFileContent(String path) {
        byte[] content = null;
        InputStream in = null;
        try {
            openFile(getCardDir().getAbsolutePath() + path);
            in = openInputStream();
            int fileSize = (int) fileSize();
            content = new byte[fileSize];
            int bReadSum = 0;
            do {
                int bRead = in.read(content, bReadSum, content.length - bReadSum);
                if (-1 == bRead) {
                    throw new IOException("EOF");
                }
                bReadSum += bRead;
            } while (bReadSum < content.length);
        } catch (Throwable ignored) {
            content = null;
        }
        TcpSocket.close(in);
        close();
        return content;
    }
}


