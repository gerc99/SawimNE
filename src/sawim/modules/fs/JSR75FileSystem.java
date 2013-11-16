
package sawim.modules.fs;

import protocol.net.TcpSocket;
import sawim.SawimException;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

public class JSR75FileSystem {

    private FileConnection fileConnection;

    public Vector getDirectoryContents(String currDir, boolean onlyDirs)
            throws SawimException {

        Vector filelist = new Vector();
        try {
            if (currDir.equals(FileSystem.ROOT_DIRECTORY)) {
                Enumeration roots = FileSystemRegistry.listRoots();
                while (roots.hasMoreElements()) {
                    filelist.addElement(new FileNode(currDir, (String) roots.nextElement()));
                }

            } else {
                FileConnection fileconn = (FileConnection) Connector.open(
                        "file://" + currDir, Connector.READ);

                Enumeration list = fileconn.list();
                filelist.addElement(new FileNode(currDir, FileSystem.PARENT_DIRECTORY));
                while (list.hasMoreElements()) {
                    String filename = (String) list.nextElement();
                    if (onlyDirs && !filename.endsWith("/")) {
                        continue;
                    }
                    filelist.addElement(new FileNode(currDir, filename));
                }
                fileconn.close();
            }
        } catch (SecurityException e) {
            throw new SawimException(193, 0);
        } catch (Exception e) {
            throw new SawimException(191, 0);
        }
        return filelist;
    }

    public long totalSize() throws Exception {
        return fileConnection.totalSize();
    }

    public void openFile(String file) throws SawimException {
        try {
            fileConnection = (FileConnection) Connector.open("file://" + file);
        } catch (SecurityException e) {
            fileConnection = null;
            throw new SawimException(193, 1);
        } catch (Exception e) {
            fileConnection = null;
            throw new SawimException(191, 1);
        }
    }

    public void mkdir(String path) {
        try {
            FileConnection fc = (FileConnection) Connector.open("file://" + path);
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


    public InputStream openInputStream() throws Exception {
        return fileConnection.openInputStream();
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


