package ru.sawim.modules;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import org.json.JSONObject;
import protocol.Contact;
import protocol.Protocol;
import protocol.net.TcpSocket;
import ru.sawim.*;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.io.FileBrowserListener;
import ru.sawim.modules.photo.PhotoListener;
import ru.sawim.roster.RosterHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public final class FileTransfer implements FileBrowserListener, PhotoListener, Runnable {

    private static final String TAG = FileTransfer.class.getSimpleName();

    private static final int CANCELED_STATUS = 0;
    private static final int ERROR_STATUS = 1;
    private int status = -1;

    private String filename;
    private String filePath;
    private InputStream fis;
    private int fsize;
    private String startTime;
    private Chat chat;

    public FileTransfer(Protocol p, Contact _cItem) {
        chat = p.getChat(_cItem);
    }

    public void showDialog(BaseActivity activity) {
        if (filename != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setCancelable(true);
            builder.setTitle(R.string.sending_file);
            String statusStr = JLocale.getString(R.string.sending_file);
            if (status == CANCELED_STATUS) {
                statusStr = JLocale.getString(R.string.stopped);
            } else if (status == ERROR_STATUS) {
                statusStr = JLocale.getString(R.string.error);
            }
            builder.setMessage(
                    JLocale.getString(R.string.path) + ": " + filePath
                            + "\n" + JLocale.getString(R.string.size) + ": " + getFileSize()
                            + "\n" + JLocale.getString(R.string.chat) + ": " + chat.getContact().getUserId()
                            + "\n" + JLocale.getString(R.string.upload_time) + ": " + startTime
                            + "\n" + JLocale.getString(R.string.status) + ": "
                            + statusStr);
            if (status == ERROR_STATUS) {
                builder.setNegativeButton(JLocale.getString(R.string.repeat),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                setProgress(0);
                                SawimApplication.getExecutor().execute(FileTransfer.this);
                                chat.activate();
                                chat.addFileProgress(JLocale.getString(R.string.sending_file), getProgressText());
                                dialog.cancel();
                            }
                        });
            } else {
                builder.setNegativeButton(JLocale.getString(R.string.cancel_upload),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                cancelUpload();
                                dialog.cancel();
                            }
                        });
            }
            builder.setPositiveButton(JLocale.getString(R.string.close),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int id) {
                            dialog.cancel();
                        }
                    });
            builder.create().show();
        }
    }

    public Contact getReceiver() {
        return chat.getContact();
    }

    private void setData(InputStream is, int size) {
        fis = is;
        fsize = size;

        long time = SawimApplication.getCurrentGmtTime();
        boolean today = (SawimApplication.getCurrentGmtTime() - 24 * 60 * 60 < time);
        startTime = ru.sawim.comm.Util.getLocalDateString(time, today);
    }

    public InputStream getFileIS() {
        return fis;
    }

    public int getFileSize() {
        return fsize;
    }

    public void startFileTransfer() {
        if (BaseActivity.getExternalApi().pickFile(this)) {
            return;
        }
        ru.sawim.modules.DebugLog.panic("show file browser");
    }

    public void startPhotoTransfer() {
        BaseActivity.getExternalApi().startCamera(this, 1024, 768);
    }

    public void onFileSelect(BaseActivity activity, Uri fileUri) {
        try {
            InputStream in = activity.getContentResolver().openInputStream(fileUri);
            setFileName(ExternalApi.getFileName(fileUri, activity));
            filePath = ExternalApi.getPath(activity, fileUri);
            int fileSize = in.available();
            byte[] image = null;

            if (Util.isImageFile(filename)) {
                image = new byte[fileSize];
                TcpSocket.readFully(in, image, 0, image.length);
            }
            if (image != null) {
                in = new ByteArrayInputStream(image);
                fileSize = in.available();
            }
            setData(in, fileSize);
            start();
        } catch (Exception e) {
            closeFile();
            handleException(new SawimException(191, 6));
        }
    }

    private void start() {
        setProgress(0);
        SawimApplication.getExecutor().execute(this);
        addProgress();
    }

    private void cancelUpload(){
        try {
            status = CANCELED_STATUS;
            destroy();

            SawimNotification.clear(getId());
            SawimNotification.fileProgress(getId(), getProgressText(), -1, JLocale.getString(R.string.canceled));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processPhoto(BaseActivity activity, final byte[] data) {
        setData(new ByteArrayInputStream(data), data.length);
        String timestamp = Util.getLocalDateString(SawimApplication.getCurrentGmtTime(), false);
        String photoName = "photo-"
                + timestamp.replace('.', '-').replace(' ', '-')
                + ".jpg";
        setFileName(photoName);
        start();
    }

    public String getProgressText() {
        return filename + " - " + StringConvertor.bytesToSizeString(getFileSize(), false);
    }

    public int getId() {
        return (chat.getContact().getUserId() + getProgressText()).hashCode();
    }

    private void changeFileProgress(int percent, int message) {
        SawimNotification.fileProgress(getId(), getProgressText(), percent, JLocale.getString(message));
    }

    public void cancel() {
        status = CANCELED_STATUS;
        changeFileProgress(0, R.string.canceled);
        closeFile();
    }

    public boolean isCanceled() {
        return status == CANCELED_STATUS;
    }

    private void addProgress() {
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
            if (0 == percent) {
                return;
            }
            changeFileProgress(percent, R.string.sending_file);
            if (100 == percent) {
                changeFileProgress(percent, R.string.complete);
                RosterHelper.getInstance().removeTransfer(getId(), true);
                SawimNotification.clear(getId());
                closeFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleException(SawimException e) {
        if (isCanceled()) {
            destroy();
            return;
        }
        status = ERROR_STATUS;
        changeFileProgress(-1, R.string.error);
        chat.addFileProgress(JLocale.getString(R.string.error), e.getLocalizedMessage());
    }

    private void closeFile() {
        TcpSocket.close(fis);
        fis = null;
    }

    public void destroy() {
        try {
            closeFile();
            RosterHelper.getInstance().removeTransfer(getId(), false);
            SawimApplication.gc();
        } catch (Exception ignored) {
        }
    }

    public void run() {
        try {
            InputStream in = getFileIS();
            int size = getFileSize();
            sendFileThroughServer(in, size);
        } catch (SawimException e) {
            handleException(e);
        } catch (Exception e) {
            DebugLog.panic("FileTransfer.run", e);
            handleException(new SawimException(194, 2));
        }
        destroy();
    }

    private void setFileName(String name) {
        name = name.replace(':', '.');
        name = name.replace('/', '_');
        name = name.replace('\\', '_');
        name = name.replace('%', '_');
        name = name.replace(' ', '_');
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
                int recived = 0;
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    if (read > 0) {
                        recived += read;
                        out.write(buffer, 0, read);
                        try {
                            Thread.sleep(500);
                        } catch (Exception ignored) {
                        }
                        int percent = 100 * recived / fileSize - 1;
                        if (percent != -1)
                            setProgress(percent);
                        if (fileSize != 0) {
                            if (isCanceled()) {
                                throw new SawimException(194, 1);
                            }
                            out.flush();
                        }
                    }
                }
                out.flush();
                out.close();

                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                switch (responseCode) {
                    case HttpURLConnection.HTTP_OK: {
                        responseIn = conn.getInputStream();
                        StringBuilder sb = new StringBuilder();
                        Scanner scanner = new Scanner(responseIn);
                        while (scanner.hasNext()) {
                            sb.append(scanner.next());
                        }
                        JSONObject root = new JSONObject(sb.toString());
                        url = root.getJSONObject("data").getString("link");
                        break;
                    }
                    case HttpURLConnection.HTTP_BAD_GATEWAY: {
                        throw new SawimException(194, 0);
                    }
                    default: {
                        Log.i(TAG, "responseCode=" + conn.getResponseCode());
                        responseIn = conn.getErrorStream();
                        StringBuilder sb = new StringBuilder();
                        Scanner scanner = new Scanner(responseIn);
                        while (scanner.hasNext()) {
                            sb.append(scanner.next()).append('\n');
                        }
                        Log.i(TAG, "error response: " + sb.toString());
                        url = sb.toString();
                        break;
                    }
                }
            } catch (java.net.SocketTimeoutException e) {
                throw new SawimException(118, 0);
            }
            catch (Exception ex) {
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
                header.writeLenAndUtf8String("");//description
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
        messText.append("File: ").append(filename).append("\n");
        messText.append("Size: ")
                .append(StringConvertor.bytesToSizeString(fileSize, false))
                .append("\n");
        messText.append("Link: ").append(url);

        chat.getProtocol().sendMessage(chat.getContact(), messText.toString(), true);
        setProgress(100);
    }
}