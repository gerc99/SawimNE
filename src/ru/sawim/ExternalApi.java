package ru.sawim;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import protocol.net.TcpSocket;
import ru.sawim.activities.BaseActivity;
import ru.sawim.io.FileBrowserListener;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.photo.CameraActivity;
import ru.sawim.modules.photo.PhotoListener;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExternalApi {

    private Fragment fragment;

    private PhotoListener photoListener = null;
    private FileBrowserListener fileTransferListener = null;
    private Uri imageUrl = null;
    private static final int RESULT_PHOTO = FragmentActivity.RESULT_FIRST_USER + 1;
    private static final int RESULT_EXTERNAL_PHOTO = FragmentActivity.RESULT_FIRST_USER + 2;
    public static final int RESULT_EXTERNAL_FILE = FragmentActivity.RESULT_FIRST_USER + 3;

    public void setFragment(Fragment a) {
        fragment = a;
    }

    public void startCamera(PhotoListener listener, int width, int height) {
        photoListener = listener;
        if (1000 < Math.max(width, height)) {
            try {
                Intent extCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (!isCallable(extCameraIntent)) throw new Exception("not found");
                imageUrl = Uri.fromFile(getOutputMediaFile());
                extCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUrl);
                fragment.startActivityForResult(extCameraIntent, RESULT_EXTERNAL_PHOTO);
                return;
            } catch (Exception ignored) {
            }
        }
        Intent cameraIntent = new Intent(fragment.getActivity(), CameraActivity.class);
        cameraIntent.putExtra(CameraActivity.WIDTH, width);
        cameraIntent.putExtra(CameraActivity.HEIGHT, height);
        fragment.startActivityForResult(cameraIntent, RESULT_PHOTO);
    }

    public boolean pickFile(FileBrowserListener listener) {
        try {
            fileTransferListener = listener;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            if (!isCallable(intent)) return false;
            fragment.startActivityForResult(Intent.createChooser(intent, null), RESULT_EXTERNAL_FILE);
            return true;
        } catch (Exception e) {
            ru.sawim.modules.DebugLog.panic("pickFile", e);
            return false;
        }
    }

    private boolean isCallable(Intent intent) {
        return !fragment.getActivity().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty();
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        DebugLog.println("result " + requestCode + " " + resultCode + " " + data);
        if (FragmentActivity.RESULT_OK != resultCode) return false;
        try {
            if (RESULT_PHOTO == requestCode) {
                if (null == photoListener) return false;
                photoListener.processPhoto((BaseActivity) fragment.getActivity(), data.getByteArrayExtra(CameraActivity.PHOTO));
                photoListener = null;
                return true;

            } else if (RESULT_EXTERNAL_PHOTO == requestCode) {
                if (null == photoListener) return false;
                // remove copy of image
                if ((null != data) && (null != data.getData()) && (null != imageUrl)) {
                    Uri file = Uri.parse("file://" + getPath(fragment.getActivity(), data.getData()));
                    DebugLog.println("pickFile " + imageUrl + " " + file);
                    if (!imageUrl.equals(file)) {
                        new File(file.getPath()).delete();
                    }
                }

                Uri uriImage = imageUrl;
                DebugLog.println("pickFile " + uriImage);
                InputStream in = fragment.getActivity().getContentResolver().openInputStream(uriImage);
                byte[] img = new byte[in.available()];
                TcpSocket.readFully(in, img, 0, img.length);
                photoListener.processPhoto((BaseActivity) fragment.getActivity(), img);

                imageUrl = null;
                photoListener = null;
                return true;

            } else if (RESULT_EXTERNAL_FILE == requestCode) {
                fileTransferListener.onFileSelect((BaseActivity) fragment.getActivity(), data.getData());
                fileTransferListener = null;
                return true;
            }
            fragment = null;
        } catch (Throwable ignored) {
        }
        return false;
    }

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
        return new File(mediaStorageDir.getPath(), "IMG_" + timeStamp + ".jpg");
    }

    public static String getFileName(Uri fileUri, FragmentActivity activity) {
        String file = getPath(activity, fileUri);
        return file.substring(file.lastIndexOf('/') + 1);
    }

    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= 19;
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
