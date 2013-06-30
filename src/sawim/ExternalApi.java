package sawim;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import ru.sawim.photo.CameraActivity;
import sawim.history.HistoryStorage;
import sawim.modules.DebugLog;
import sawim.modules.photo.PhotoListener;

import java.io.InputStream;

public class ExternalApi {

    public static ExternalApi instance = new ExternalApi();
    private FragmentActivity activity;

    public void setActivity(FragmentActivity activity) {
        this.activity = activity;
    }

    private PhotoListener photoListener = null;
    private FileTransfer fileTransferListener = null;
    private static final int RESULT_PHOTO = FragmentActivity.RESULT_FIRST_USER + 1;
    private static final int RESULT_EXTERNAL_PHOTO = FragmentActivity.RESULT_FIRST_USER + 2;
    private static final int RESULT_EXTERNAL_FILE = FragmentActivity.RESULT_FIRST_USER + 3;

    public void startCamera(PhotoListener listener, int width, int height) {
        photoListener = listener;
        if (1000 < Math.max(width, height)) {
            try {
                Intent extCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (!isCallable(extCameraIntent)) throw new Exception("not found");
                activity.startActivityForResult(extCameraIntent, RESULT_EXTERNAL_PHOTO);
                return;
            } catch (Exception ignored) {
            }
        }
        Intent cameraIntent = new Intent(activity, CameraActivity.class);
        cameraIntent.putExtra("width", width);
        cameraIntent.putExtra("height", height);
        activity.startActivityForResult(cameraIntent, RESULT_PHOTO);
    }

    public boolean pickFile(FileTransfer listener) {
        try {
            fileTransferListener = listener;
            Intent theIntent = new Intent(Intent.ACTION_GET_CONTENT);
            theIntent.setType("file/*");
            theIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            if (!isCallable(theIntent)) return false;
            activity.startActivityForResult(theIntent, RESULT_EXTERNAL_FILE);
            return true;
        } catch (Exception e) {
            sawim.modules.DebugLog.panic("pickFile", e);
            return false;
        }
    }

    private boolean isCallable(Intent intent) {
        return !activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty();
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        DebugLog.println("result " + requestCode + " " + resultCode + " " + data);
        if (null == data) return false;
        if (FragmentActivity.RESULT_OK != resultCode) return false;
        try {
            if (RESULT_PHOTO == requestCode) {
                if (null == photoListener) return false;
                photoListener.processPhoto(data.getByteArrayExtra("photo"));
                photoListener = null;
                return true;

            } else if (RESULT_EXTERNAL_PHOTO == requestCode) {
                if (null == photoListener) return false;
                Uri uriImage = data.getData();
                InputStream in = activity.getContentResolver().openInputStream(uriImage);
                byte[] img = new byte[in.available()];
                in.read(img);
                photoListener.processPhoto(img);
                photoListener = null;
                return true;

            } else if (RESULT_EXTERNAL_FILE == requestCode) {
                Uri fileUri = data.getData();
                InputStream is = activity.getContentResolver().openInputStream(fileUri);
                fileTransferListener.onFileSelect(is, getFileName(fileUri));
                fileTransferListener = null;
                return true;
            }
        } catch (Throwable ignored) {
            DebugLog.panic("activity", ignored);
        }
        return false;
    }

    private String getFileName(Uri fileUri) {
        String file = getRealPathFromUri(fileUri);
        return file.substring(file.lastIndexOf('/') + 1);
    }

    private String getRealPathFromUri(Uri uri) {
        try {
            if ("content".equals(uri.getScheme())) {
                String[] proj = {MediaStore.MediaColumns.DATA};
                Cursor cursor = activity.managedQuery(uri, proj, null, null, null);
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                cursor.moveToFirst();
                return cursor.getString(columnIndex);
            }
            if ("file".equals(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception ignored) {
        }
        return uri.toString();
    }

    public void showHistory(HistoryStorage history) {
        String historyFilePath = history.getAndroidStorage().getTextFile();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("file://" + historyFilePath);
        intent.setDataAndType(uri, "text/plain");
        activity.startActivity(intent);
    }
}
