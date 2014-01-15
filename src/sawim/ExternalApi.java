package sawim;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import protocol.net.TcpSocket;
import ru.sawim.photo.CameraActivity;
import sawim.modules.DebugLog;
import sawim.modules.photo.PhotoListener;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExternalApi {

    public static ExternalApi instance = new ExternalApi();
    private FragmentActivity activity;

    public void setActivity(FragmentActivity activity) {
        this.activity = activity;
    }

    private PhotoListener photoListener = null;
    private FileTransfer fileTransferListener = null;
    private Uri imageUrl = null;
    private static final int RESULT_PHOTO = FragmentActivity.RESULT_FIRST_USER + 1;
    private static final int RESULT_EXTERNAL_PHOTO = FragmentActivity.RESULT_FIRST_USER + 2;
    public static final int RESULT_EXTERNAL_FILE = FragmentActivity.RESULT_FIRST_USER + 3;

    public void startCamera(PhotoListener listener, int width, int height) {
        photoListener = listener;
        if (1000 < Math.max(width, height)) {
            try {
                Intent extCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (!isCallable(extCameraIntent)) throw new Exception("not found");
                imageUrl = Uri.fromFile(getOutputMediaFile());
                extCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUrl);
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
        if (FragmentActivity.RESULT_OK != resultCode) return false;
        try {
            if (RESULT_PHOTO == requestCode) {
                if (null == photoListener) return false;
                photoListener.processPhoto(data.getByteArrayExtra("photo"));
                photoListener = null;
                return true;

            } else if (RESULT_EXTERNAL_PHOTO == requestCode) {
                if (null == photoListener) return false;
                // remove copy of image
                if ((null != data) && (null != data.getData()) && (null != imageUrl)) {
                    Uri file = Uri.parse("file://" + getRealPathFromUri(data.getData(), activity));
                    DebugLog.println("pickFile " + imageUrl + " " + file);
                    if (!imageUrl.equals(file)) {
                        new File(file.getPath()).delete();
                    }
                }

                Uri uriImage = imageUrl;
                DebugLog.println("pickFile " + uriImage);
                InputStream in = activity.getContentResolver().openInputStream(uriImage);
                byte[] img = new byte[in.available()];
                TcpSocket.readFully(in, img, 0, img.length);
                photoListener.processPhoto(img);

                imageUrl = null;
                photoListener = null;
                return true;

            } else if (RESULT_EXTERNAL_FILE == requestCode) {
                Uri fileUri = data.getData();
                InputStream is = activity.getContentResolver().openInputStream(fileUri);
                fileTransferListener.onFileSelect(is, getFileName(fileUri, activity));
                fileTransferListener = null;
                return true;
            }
        } catch (Throwable ignored) {
            DebugLog.panic("activity", ignored);
        }
        return false;
    }

    public static String getFileName(Uri fileUri, FragmentActivity activity) {
        String file = getRealPathFromUri(fileUri, activity);
        return file.substring(file.lastIndexOf('/') + 1);
    }

    private static String getRealPathFromUri(Uri uri, FragmentActivity activity) {
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

    /*public void showHistory(HistoryStorage history) {
        String historyFilePath = history.getAndroidStorage().getTextFile();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("file://" + historyFilePath);
        intent.setDataAndType(uri, "text/plain");
        activity.startActivity(intent);
    }*/

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_PICTURES), "Sawim");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("Sawim", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath(), "IMG_"+ timeStamp + ".jpg");
    }
}
