package ru.sawim.modules;

import android.content.DialogInterface;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.webkit.MimeTypeMap;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.json.JSONException;
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
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static ru.sawim.modules.FileTransfer.TransferStatus.CANCEL;
import static ru.sawim.modules.FileTransfer.TransferStatus.ERROR;

public final class FileTransfer implements FileBrowserListener, PhotoListener, Runnable {
    enum TransferStatus {
        OK, CANCEL, ERROR
    }
    private TransferStatus status = TransferStatus.OK;

    private static final int BUFFER_LEN = 4096;

    private String filename;
    private String filePath;
    private byte[] fileBytes;
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
            if (status == CANCEL) {
                statusStr = JLocale.getString(R.string.stopped);
            } else if (status == ERROR) {
                statusStr = JLocale.getString(R.string.error);
            }
            builder.setMessage(filePath == null ? "" : (JLocale.getString(R.string.path) + ": " + filePath + "\n")
                    + JLocale.getString(R.string.size) + ": " + getFileSize() + "\n"
                    + JLocale.getString(R.string.chat) + ": " + chat.getContact().getUserId() + "\n"
                    + JLocale.getString(R.string.upload_time) + ": " + startTime + "\n"
                    + JLocale.getString(R.string.status) + ": " + statusStr);
            if (status == ERROR) {
                builder.setNegativeButton(JLocale.getString(R.string.repeat),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                SawimNotification.clear(getId());
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

    private void setData(byte[] fileBytes) {
        this.fileBytes = fileBytes;

        long time = SawimApplication.getCurrentGmtTime() / 1000;
        boolean today = (SawimApplication.getCurrentGmtTime() / 1000 - 24 * 60 * 60 < time);
        startTime = ru.sawim.comm.Util.getLocalDateString(time, today);
    }

    public InputStream getFileIS() {
        return new ByteArrayInputStream(fileBytes);
    }

    public int getFileSize() {
        return fileBytes.length;
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
            byte[] fileBytes = new byte[fileSize];
            TcpSocket.readFully(in, fileBytes, 0, fileBytes.length);

            setData(fileBytes);
            start();
        } catch (Exception e) {
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
            status = ERROR;
            destroy();

            SawimNotification.clear(getId());
            SawimNotification.fileProgress(getId(), getProgressText(), -1, JLocale.getString(R.string.canceled));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy_HH-mm");
    public void processPhoto(BaseActivity activity, final byte[] data) {
        setData(data);
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = DATE_FORMAT.format(SawimApplication.getCurrentGmtTime());
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
        status = CANCEL;
        changeFileProgress(0, R.string.canceled);
    }

    public boolean isCanceled() {
        return status == CANCEL;
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
        status = ERROR;
        changeFileProgress(-1, R.string.error);
        chat.addFileProgress(JLocale.getString(R.string.error), e.getLocalizedMessage());
    }

    public void destroy() {
        try {
            RosterHelper.getInstance().removeTransfer(getId(), false);
            SawimApplication.gc();
        } catch (Exception ignored) {
        }
    }

    public void run() {
        try {
            sendFile();
        } catch (SawimException e) {
            e.printStackTrace();
            handleException(e);
        } catch (IOException e) {
            e.printStackTrace();
            handleException(new SawimException(194, 0));
        }
        destroy();
    }

    private void setFileName(String name) {
        filename = name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private void sendFile() throws IOException, SawimException {
        String imageUrl;
        if (Util.isImageFile(filename)) {
            imageUrl = sendImageViaImgur();
        } else {
            imageUrl = sendViaJimmNetRu();
        }

        String fileSize = StringConvertor.bytesToSizeString(
                getFileSize(), false);
        String message = "File: " + filename + "\n" +
                         "Size: " + fileSize + "\n" +
                         "Link: " + imageUrl;
        chat.getProtocol().sendMessage(chat, message);
    }

    private static final String JIMM_NET_RU_UPLOAD_URL= "files.jimm.net.ru";
    private static final int JIMM_NET_RU_UPLOAD_PORT = 2000;
    private static final int PROTOCOL_VERSION = 1;
    private static final String TRANSFER_CLIENT = "sawimne";

    private String sendViaJimmNetRu() throws SawimException {
        int fileSize = getFileSize();
        TcpSocket socket = new TcpSocket();
        InputStream fis = getFileIS();
        try {
            socket.connectTo(JIMM_NET_RU_UPLOAD_URL, JIMM_NET_RU_UPLOAD_PORT);

            Util header = new Util();
            header.writeWordBE(PROTOCOL_VERSION);
            header.writeLenAndUtf8String(filename);
            header.writeLenAndUtf8String(""); // Description
            header.writeLenAndUtf8String(TRANSFER_CLIENT);
            header.writeDWordBE(getFileSize());
            socket.write(header.toByteArray());
            socket.flush();

            byte[] buffer = new byte[BUFFER_LEN];
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

            if (isCanceled()) {
                throw new SawimException(194, 1);
            }
            socket.close();

            return StringConvertor.utf8beByteArrayToString(buffer, 0, length);
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

    private static final String IMGUR_CLIENT_ID = "c90472da1a4d000";
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/image";

    private String sendImageViaImgur() throws IOException, SawimException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", filename, getRequestBody())
                .build();
        Request request = new Request.Builder()
                .header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                .url(IMGUR_UPLOAD_URL)
                .post(requestBody)
                .build();
        JSONObject response = getJSONResponse(request);
        try {
            return response.getJSONObject("data").getString("link");
        } catch (JSONException e) {
            DebugLog.panic("send file: bad response 2 [imgur]", e);
            throw new SawimException(194, 2);
        }
    }

    private JSONObject getJSONResponse(Request request) throws SawimException {
        OkHttpClient client = new OkHttpClient();
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            DebugLog.panic("send file: cannot get response [imgur]", e);
            throw new SawimException(194, 2);
        }
        if (!response.isSuccessful()) {
            throw new SawimException(194, 2);
        }

        try {
            String jsonData = response.body().string();
            return new JSONObject(jsonData);
        } catch (JSONException e) {
            DebugLog.panic("send file: bad response 1 [imgur]", e);
            throw new SawimException(194, 2);
        } catch (IOException e) {
            DebugLog.panic("send file: cannot get response [imgur]", e);
            throw new SawimException(194, 2);
        }
    }

    private RequestBody getRequestBody() {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                MediaType mediaType;
                String extension = MimeTypeMap.getFileExtensionFromUrl(filename);
                if (extension != null) {
                    MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                    String mime = mimeTypeMap.getMimeTypeFromExtension(extension);
                    mediaType = MediaType.parse(mime);
                } else {
                    mediaType = MediaType.parse("application/octet-stream");
                }
                return mediaType;
            }

            @Override
            public long contentLength() {
                return fileBytes.length;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source = Okio.source(getFileIS());
                Buffer buf = new Buffer();

                long bytesSent = 0;

                for (long readCount;
                     (readCount = source.read(buf, BUFFER_LEN)) != -1; ) {
                    if (isCanceled()) {
                        throw new IOException("User canceled file upload");
                    }
                    sink.write(buf, readCount);
                    bytesSent += readCount;

                    int progress = (int)(100 * bytesSent / contentLength() - 1);
                    setProgress(progress);
                }
            }
        };
    }
}
