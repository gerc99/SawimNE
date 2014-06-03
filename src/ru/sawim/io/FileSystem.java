
package ru.sawim.io;

import org.microemu.util.FileSystemFileConnection;
import protocol.TcpSocket;
import ru.sawim.SawimException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileSystem {

    public static final String HISTORY = "history";
    public static final String RES = "res";

    private FileSystemFileConnection fileConnection;

    public static String getSawimHome() {
        return "/e:/sawimne/";
    }

    public static FileSystem getInstance() {
        return new FileSystem();
    }

    public InputStream openSawimFile(String file) {
        byte[] buffer = null;
        try {
            openFile(getSawimHome() + RES + "/" + file);
            InputStream in = fileConnection.openInputStream();
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

    public long totalSize() throws Exception {
        return fileConnection.totalSize();
    }

    public void openFile(String file) throws SawimException {
        try {
            fileConnection = FileSystemFileConnection.open("file://" + file);
        } catch (SecurityException e) {
            fileConnection = null;
            throw new SawimException(193, 1);
        } catch (Exception e) {
            fileConnection = null;
            e.printStackTrace();
            throw new SawimException(191, 1);
        }
    }

    public void mkdir(String path) {
        try {
            FileSystemFileConnection fc = FileSystemFileConnection.open("file://" + path);
            try {
                fc.mkdir();
            } finally {
                fc.close();
            }
        } catch (IOException e) {

        }
    }

    public boolean exists() {
        return (null != fileConnection) && fileConnection.exists();
    }

    public InputStream openInputStream() throws IOException {
        return fileConnection.openInputStream();
    }

    public OutputStream openOutputStream() throws Exception {
        if (fileConnection.exists()) {
            fileConnection.delete();
        }
        fileConnection.create();
        return fileConnection.openOutputStream();
    }

    public OutputStream openForAppendOutputStream() throws Exception {
        if (!fileConnection.exists()) {
            fileConnection.create();
        }
        return fileConnection.openOutputStream(true);
    }

    public String getAbsolutePath() throws Exception {
        return fileConnection.getAbsolutePath();
    }

    public void close() {
        try {
            if (null != fileConnection) {
                fileConnection.close();
            }
            fileConnection = null;
        } catch (Exception e) {
        }
    }

    public long fileSize() throws Exception {
        return (null == fileConnection) ? -1 : fileConnection.fileSize();
    }

    public String getName() {
        return (null == fileConnection) ? null : fileConnection.getName();
    }

    public byte[] getFileContent(String path) {
        byte[] content = null;
        InputStream in = null;
        try {
            openFile(path);
            in = fileConnection.openInputStream();
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


