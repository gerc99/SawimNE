package sawim;

import android.support.v4.app.FragmentActivity;
import protocol.Contact;
import protocol.Protocol;
import protocol.net.TcpSocket;
import ru.sawim.General;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import sawim.chat.Chat;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.modules.DebugLog;
import sawim.modules.fs.FileBrowser;
import sawim.modules.fs.FileBrowserListener;
import sawim.modules.fs.FileSystem;
import sawim.modules.fs.JSR75FileSystem;
import sawim.modules.photo.PhotoListener;
import sawim.util.JLocale;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class FileTransfer implements FileBrowserListener,
        PhotoListener, Runnable, FormListener {

    private static final int descriptionField = 1000;
    private static final int transferMode = 1001;
    private static final int JNR_SOCKET = 0;
    private static final int JNR_HTTP = 1;
    private static final int JO_HTTP = 2;
    private static final int IBB_MODE = 3;
    private static final int MAX_IMAGE_SIZE = 2 * 1024 * 1024;
    private String filename;
    private String description;
    private int sendMode;
    private InputStream fis;
    private int fsize;
    private boolean canceled = false;
    private Protocol protocol;
    private Contact cItem;
    private Chat chat;
    private JSR75FileSystem file;
    private Forms name_Desc;
    private FragmentActivity activity;

    public FileTransfer(FragmentActivity a, Protocol p, Contact _cItem) {
        activity = a;
        protocol = p;
        cItem = _cItem;
    }

    public static boolean isPhotoSupported() {
        String supports = System.getProperty("video.snapshot.encodings");
        return !StringConvertor.isEmpty(supports);
    }

    public Contact getReceiver() {
        return cItem;
    }

    private void setData(InputStream is, int size) {
        fis = is;
        fsize = size;
    }

    public InputStream getFileIS() {
        return fis;
    }

    public int getFileSize() {
        return fsize;
    }

    public void startFileTransfer() {
        if (SawimActivity.getInstance().pickFile(this)) {
            return;
        }
        sawim.modules.DebugLog.panic("show file browser");
        FileBrowser fsBrowser = new FileBrowser(false);
        fsBrowser.setListener(this);
        fsBrowser.activate();
    }

    public void startPhotoTransfer() {
        SawimActivity.getInstance().startCamera(this, 1024, 768);
    }

    public void onFileSelect(InputStream in, String fileName) {
        try {
            setFileName(fileName);
            int fileSize = in.available();
            setData(in, fileSize);
            askForNameDesc();
        } catch (Exception e) {
            closeFile();
            handleException(new SawimException(191, 6));
        }
    }

    public void onFileSelect(String filename) throws SawimException {
        file = FileSystem.getInstance();
        try {
            file.openFile(filename);
            setFileName(file.getName());

            InputStream is = file.openInputStream();
            int fileSize = (int) file.fileSize();
            byte[] image = null;

            if ((fileSize < MAX_IMAGE_SIZE) && isImageFile()) {
                image = FileSystem.getInstance().getFileContent(filename);
            }
            setData(is, fileSize);
            askForNameDesc();
            showPreview(image);
        } catch (Exception e) {
            closeFile();
            throw new SawimException(191, 3);
        }
    }

    public void onDirectorySelect(String s0) {
    }

    private void askForNameDesc() {
        name_Desc = Forms.getInstance();
        name_Desc.init("name_desc", this);
        name_Desc.addString("filename", filename);
        name_Desc.addTextField(descriptionField, "description", "");
        String items = "jimm.net.ru|www.jimm.net.ru|jimm.org";
        if (cItem instanceof protocol.jabber.JabberContact) {
            if (cItem.isSingleUserContact() && cItem.isOnline()) {
                items += "|ibb";
            }
        }
        name_Desc.addSelector(transferMode, "send_via", items, 0);
        name_Desc.addString(JLocale.getString("size") + ": ", String.valueOf(getFileSize() / 1024) + " kb");
        name_Desc.show();
    }

    public void formAction(Forms form, boolean apply) {
        if (apply) {
            description = name_Desc.getTextFieldValue(descriptionField);
            sendMode = name_Desc.getSelectorValue(transferMode);
            addProgress();
            if (name_Desc.getSelectorValue(transferMode) == IBB_MODE) {
                try {
                    protocol.sendFile(this, filename, description);
                } catch (Exception ignored) {
                }
            } else {
                setProgress(0);
                new Thread(this).start();
            }
            cItem.showFileProgress(activity);
        } else {
            destroy();
            form.back();
            ContactList.getInstance().activate();
        }
    }

    private String getProgressText() {
        return filename + " - " + StringConvertor.bytesToSizeString(getFileSize(), false);
    }

    private void changeFileProgress(int percent, String message) {
        cItem.changeFileProgress(percent, JLocale.getEllipsisString("sending_file"),
                getProgressText() + "\n"
                        + JLocale.getString(message));
    }

    public void cancel() {
        canceled = true;
        changeFileProgress(0, "canceled");
        if (0 < sendMode) {
            closeFile();
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    private void addProgress() {
        chat = protocol.getChat(cItem);
        chat.activate();
        cItem.addFileProgress();
        chat.addFileProgress(JLocale.getEllipsisString("sending_file"), getProgressText());
        ContactList.getInstance().addTransfer(this);
    }

    public void setProgress(int percent) {
        try {
            if (isCanceled()) {
                return;
            }
            if (-1 == percent) {
                percent = 100;
            }
            if ((0 == percent)) {
                return;
            }
            changeFileProgress(percent, JLocale.getEllipsisString("sending_file"));
            if (100 == percent) {
                ContactList.getInstance().removeTransfer(false);
                changeFileProgress(percent, "complete");
                return;
            }
        } catch (Exception ignored) {
        }
    }

    private void handleException(SawimException e) {
        destroy();
        if (isCanceled()) {
            return;
        }
        changeFileProgress(0, JLocale.getString("error") + "\n" + e.getMessage());
    }

    private void closeFile() {
        if (null != file) {
            file.close();
            file = null;
        }
        TcpSocket.close(fis);
        fis = null;
    }

    public void destroy() {
        try {
            closeFile();
            ContactList.getInstance().removeTransfer(false);
            name_Desc.back();
            Sawim.gc();
        } catch (Exception ignored) {
        }
    }

    public void run() {
        try {
            InputStream in = getFileIS();
            int size = getFileSize();
            switch (sendMode) {
                case JNR_SOCKET:
                    sendFileThroughServer(in, size);
                    break;
                case JNR_HTTP:
                    sendFileThroughWeb("files.jimm.net.ru:81", in, size);
                    break;
                case JO_HTTP:
                    sendFileThroughWeb("filetransfer.jimm.org", in, size);
                    break;
            }

        } catch (SawimException e) {
            handleException(e);
        } catch (Exception e) {
            DebugLog.panic("FileTransfer.run", e);
            handleException(new SawimException(194, 2));
        }
        destroy();
    }

    public void processPhoto(final byte[] data) {
        setData(new ByteArrayInputStream(data), data.length);
        String timestamp = Util.getLocalDateString(Sawim.getCurrentGmtTime(), false);
        String photoName = "photo-"
                + timestamp.replace('.', '-').replace(' ', '-')
                + ".jpg";
        setFileName(photoName);
        askForNameDesc();
        showPreview(data);
    }

    private void showPreview(final byte[] image) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    name_Desc.addBitmap(General.avatarBitmap(image));
                } catch (Throwable ignored) {
                }
            }
        }).start();
    }

    private void setFileName(String name) {
        name = name.replace(':', '.');
        name = name.replace('/', '_');
        name = name.replace('\\', '_');
        name = name.replace('%', '_');
        filename = name;
    }

    private String getTransferClient() {
        return "sawimne";
    }

    private void sendFileThroughServer(InputStream fis, int fileSize) throws SawimException {
        TcpSocket socket = new TcpSocket();
        try {
            socket.connectTo("socket://files.jimm.net.ru:2000");

            final int version = 1;
            Util header = new Util();
            header.writeWordBE(version);
            header.writeLenAndUtf8String(filename);
            header.writeLenAndUtf8String(description);
            header.writeLenAndUtf8String(getTransferClient());
            header.writeDWordBE(fileSize);
            socket.write(header.toByteArray());
            socket.flush();

            byte[] buffer = new byte[4 * 1024];
            int counter = fileSize;
            while (counter > 0) {
                int read = fis.read(buffer);
                socket.write(buffer, 0, read);
                counter -= read;
                if (fileSize != 0) {
                    if (isCanceled()) {
                        throw new SawimException(194, 1);
                    }
                    socket.flush();
                    setProgress((100 - 2) * (fileSize - counter) / fileSize);
                }
            }
            socket.flush();

            int length = socket.read();
            if (-1 == length) {
                throw new SawimException(120, 13);
            }
            socket.read(buffer, 0, length);
            String url = StringConvertor.utf8beByteArrayToString(buffer, 0, length);

            if (isCanceled()) {
                throw new SawimException(194, 1);
            }

            StringBuffer messText = new StringBuffer();
            if (!StringConvertor.isEmpty(description)) {
                messText.append(description).append("\n");
            }
            messText.append("File: ").append(filename).append("\n");
            messText.append("Size: ")
                    .append(StringConvertor.bytesToSizeString(fileSize, false))
                    .append("\n");
            messText.append("Link: ").append(url);

            protocol.sendMessage(cItem, messText.toString(), true);
            setProgress(100);
            socket.close();

        } catch (SawimException e) {
            DebugLog.panic("send file", e);
            socket.close();
            throw e;

        } catch (Exception e) {
            DebugLog.panic("send file", e);
            socket.close();
            throw new SawimException(194, 0);
        }
    }

    private void sendFileThroughWeb(String host, InputStream fis, int fsize) throws SawimException {
        InputStream is;
        OutputStream os;
        HttpConnection sc = null;

        final String url = "http://" + host + "/__receive_file.php";
        try {
            sc = (HttpConnection) Connector.open(url, Connector.READ_WRITE);
            sc.setRequestMethod(HttpConnection.POST);
            String boundary = "a9f843c9b8a736e53c40f598d434d283e4d9ff72";
            sc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            os = sc.openOutputStream();

            StringBuffer headers = new StringBuffer();
            headers.append("--").append(boundary).append("\r\n");
            headers.append("Content-Disposition: form-data; name=\"filedesc\"\r\n");
            headers.append("\r\n");
            headers.append(description);
            headers.append("\r\n");
            headers.append("--").append(boundary).append("\r\n");
            headers.append("Content-Disposition: form-data; name=\"Sawimfile\"; filename=\"");
            headers.append(filename).append("\"\r\n");
            headers.append("Content-Type: application/octet-stream\r\n");
            headers.append("Content-Transfer-Encoding: binary\r\n");
            headers.append("\r\n");
            os.write(StringConvertor.stringToByteArrayUtf8(headers.toString()));

            byte[] buffer = new byte[1024 * 2];
            int counter = fsize;
            while (counter > 0) {
                int read = fis.read(buffer);
                os.write(buffer, 0, read);
                counter -= read;
                if (fsize != 0) {
                    if (isCanceled()) {
                        throw new SawimException(194, 1);
                    }
                    setProgress((100 - 2) * (fsize - counter) / fsize);
                }
            }
            String end = "\r\n--" + boundary + "--\r\n";
            os.write(StringConvertor.stringToByteArrayUtf8(end));

            is = sc.openInputStream();
            int respCode = sc.getResponseCode();
            if (HttpConnection.HTTP_OK != respCode) {
                throw new SawimException(194, respCode);
            }

            StringBuffer response = new StringBuffer();
            for (; ; ) {
                int read = is.read();
                if (read == -1) break;
                response.append((char) (read & 0xFF));
            }

            String respString = response.toString();
            int dataPos = respString.indexOf("http://");
            if (-1 == dataPos) {
                DebugLog.println("server say '" + respString + "'");
                throw new SawimException(194, 1);
            }

            if (isCanceled()) {
                throw new SawimException(194, 1);
            }
            respString = Util.replace(respString, "\r", "");
            respString = Util.replace(respString, "\n", "");

            StringBuffer messText = new StringBuffer();
            if (!StringConvertor.isEmpty(description)) {
                messText.append(description).append("\n");
            }
            messText.append("File: ").append(filename).append("\n");
            messText.append("Size: ")
                    .append(StringConvertor.bytesToSizeString(fsize, false))
                    .append("\n");
            messText.append("Link: ").append(respString);

            protocol.sendMessage(cItem, messText.toString(), true);
            setProgress(100);
            TcpSocket.close(sc);
            TcpSocket.close(os);
            TcpSocket.close(is);
        } catch (IOException e) {
            TcpSocket.close(sc);
            DebugLog.panic("send file", e);
            throw new SawimException(194, 0);
        }
    }

    private boolean isImageFile() {
        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png");
    }
}