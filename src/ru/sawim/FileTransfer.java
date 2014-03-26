package ru.sawim;

import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import org.json.JSONObject;
import protocol.Contact;
import protocol.Protocol;
import protocol.net.TcpSocket;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.chat.Chat;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.fs.FileBrowserListener;
import ru.sawim.modules.fs.JSR75FileSystem;
import ru.sawim.modules.photo.PhotoListener;
import ru.sawim.roster.RosterHelper;
import ru.sawim.util.JLocale;
import ru.sawim.view.FileProgressView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public final class FileTransfer implements FileBrowserListener, PhotoListener, Runnable, FormListener {
    private static final String TAG = FileTransfer.class.getSimpleName();

    private static final int descriptionField = 1000;
    private static final int transferMode = 1001;
    private static final int JNR_SOCKET = 0;
    private static final int JNR_HTTP = 1;
    private static final int JO_HTTP = 2;
    private static final int IBB_MODE = 3;
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
    private Forms forms;
    private FileProgressView fileProgressView;
    private boolean isFinish = false;

    public FileTransfer(Protocol p, Contact _cItem) {
        protocol = p;
        cItem = _cItem;
    }

    public void setFinish(boolean finish) {
        this.isFinish = finish;
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
        if (SawimActivity.externalApi.pickFile(this)) {
            return;
        }
        ru.sawim.modules.DebugLog.panic("show file browser");
    }

    public void startPhotoTransfer() {
        SawimActivity.externalApi.startCamera(this, 1024, 768);
    }

    public void onFileSelect(InputStream in, String fileName) {
        try {
            setFileName(fileName);
            int fileSize = in.available();
            byte[] image = null;

            if (Util.isImageFile(filename)) {
                image = new byte[fileSize];
                TcpSocket.readFully(in, image, 0, image.length);
            }
            setData(image == null ? in : new ByteArrayInputStream(image), fileSize);
            askForNameDesc();
            showPreview(image);
        } catch (Exception e) {
            closeFile();
            handleException(new SawimException(191, 6));
        }
    }

    public void processPhoto(final byte[] data) {
        setData(new ByteArrayInputStream(data), data.length);
        String timestamp = Util.getLocalDateString(SawimApplication.getCurrentGmtTime(), false);
        String photoName = "photo-"
                + timestamp.replace('.', '-').replace(' ', '-')
                + ".jpg";
        setFileName(photoName);
        askForNameDesc();
        showPreview(data);
    }

    private void askForNameDesc() {
        forms = new Forms(R.string.name_desc, this, true);
        forms.addString(String.format("%s: %s", JLocale.getString(R.string.filename), filename), null);
        forms.addTextField(descriptionField, R.string.description, "");
        String items = "jimm.net.ru|www.jimm.net.ru|jimm.org";
        if (cItem instanceof protocol.xmpp.XmppContact) {
            if (cItem.isSingleUserContact() && cItem.isOnline()) {
                items += "|ibb";
            }
        }
        forms.addSelector(transferMode, R.string.send_via, items, 0);
        forms.addString(String.format("%s: %d KB", JLocale.getString(R.string.size), getFileSize() / 1024), null);
        forms.show();
    }

    public void formAction(Forms form, boolean apply) {
        if (apply) {
            description = forms.getTextFieldValue(descriptionField);
            sendMode = forms.getSelectorValue(transferMode);
            showFileProgress();
            if (forms.getSelectorValue(transferMode) == IBB_MODE) {
                try {
                    protocol.sendFile(this, filename, description);
                } catch (Exception ignored) {
                }
            } else {
                setProgress(0);
                new Thread(this).start();
            }
            addProgress();
        } else {
            destroy();
        }
    }

    private String getProgressText() {
        return filename + " - " + StringConvertor.bytesToSizeString(getFileSize(), false);
    }

    private void showFileProgress() {
        if (fileProgressView == null)
            fileProgressView = new FileProgressView();
        fileProgressView.showProgress();
    }

    private void changeFileProgress(int percent, int message) {
        if (fileProgressView != null)
            fileProgressView.changeFileProgress(percent, JLocale.getString(R.string.sending_file),
                    getProgressText() + "\n"
                            + JLocale.getString(message));
    }

    public void cancel() {
        canceled = true;
        changeFileProgress(0, R.string.canceled);
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
        chat.addFileProgress(JLocale.getString(R.string.sending_file), getProgressText());
        RosterHelper.getInstance().addTransfer(this);
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
            changeFileProgress(percent, R.string.sending_file);
            if (100 == percent) {
                RosterHelper.getInstance().removeTransfer(false);
                changeFileProgress(percent, R.string.complete);
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
        changeFileProgress(0, R.string.error);
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
            RosterHelper.getInstance().removeTransfer(false);
            SawimApplication.gc();
        } catch (Exception ignored) {
        }
        forms.back();
        if (isFinish) BaseActivity.getCurrentActivity().finish();
        fileProgressView = null;
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

    private void showPreview(final byte[] image) {
        if (image == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    forms.addBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
                    forms.invalidate(true);
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
        String url = null;
        if (Util.isImageFile(filename)) {
            final String UPLOAD_URL = "https://api.imgur.com/3/image";
            HttpURLConnection conn = null;
            InputStream responseIn = null;
            try {
                conn = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
                conn.setDoOutput(true);
                if (Build.VERSION.SDK_INT > 13) {
                    conn.setRequestProperty("Connection", "close");
                }

                conn.setRequestProperty("Authorization", "Client-ID c90472da1a4d000"); // get on site

                OutputStream out = conn.getOutputStream();
                byte[] buffer = new byte[8192];
                int counter = fileSize;
                while (counter > 0) {
                    int read = fis.read(buffer);
                    out.write(buffer, 0, read);
                    counter -= read;
                    if (fileSize != 0) {
                        if (isCanceled()) {
                            throw new SawimException(194, 1);
                        }
                        out.flush();
                        setProgress((100 - 2) * (fileSize - counter) / fileSize);
                    }
                }
                out.flush();
                out.close();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    responseIn = conn.getInputStream();
                    StringBuilder sb = new StringBuilder();
                    Scanner scanner = new Scanner(responseIn);
                    while (scanner.hasNext()) {
                        sb.append(scanner.next());
                    }

                    JSONObject root = new JSONObject(sb.toString());
                    url = root.getJSONObject("data").getString("link");
                } else {
                    Log.i(TAG, "responseCode=" + conn.getResponseCode());
                    responseIn = conn.getErrorStream();
                    StringBuilder sb = new StringBuilder();
                    Scanner scanner = new Scanner(responseIn);
                    while (scanner.hasNext()) {
                        sb.append(scanner.next() + "\n");
                    }
                    Log.i(TAG, "error response: " + sb.toString());
                    url = sb.toString();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                DebugLog.panic(ex);
                throw new SawimException(194, 0);
            } finally {
                try {
                    responseIn.close();
                } catch (Exception ignore) {
                }
                try {
                    conn.disconnect();
                } catch (Exception ignore) {
                }
            }
        } else {
            TcpSocket socket = new TcpSocket();
            try {
                socket.connectTo("files.jimm.net.ru", 2000);

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
                url = StringConvertor.utf8beByteArrayToString(buffer, 0, length);

                if (isCanceled()) {
                    throw new SawimException(194, 1);
                }
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
        StringBuilder messText = new StringBuilder();
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
    }

    private void sendFileThroughWeb(String host, InputStream fis, int fsize) throws SawimException {
        InputStream is;
        OutputStream os;
        HttpURLConnection sc = null;

        final String url = "http://" + host + "/__receive_file.php";
        try {
            sc = (HttpURLConnection) new URL(url).openConnection();
            sc.setRequestMethod("POST");
            String boundary = "a9f843c9b8a736e53c40f598d434d283e4d9ff72";
            sc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            os = sc.getOutputStream();

            StringBuilder headers = new StringBuilder();
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

            is = sc.getInputStream();
            int respCode = sc.getResponseCode();
            if (HttpURLConnection.HTTP_OK != respCode) {
                throw new SawimException(194, respCode);
            }

            StringBuilder response = new StringBuilder();
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
            respString = respString.replace("\r", "");
            respString = respString.replace("\n", "");

            StringBuilder messText = new StringBuilder();
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
            sc.disconnect();
            os.close();
            is.close();
        } catch (IOException e) {
            sc.disconnect();
            DebugLog.panic("send file", e);
            throw new SawimException(194, 0);
        }
    }
}