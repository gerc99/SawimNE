package ru.sawim;

import DrawControls.icons.Icon;
import DrawControls.icons.Image;
import DrawControls.icons.ImageList;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import sawim.ui.base.Scheme;
import ru.sawim.activities.SawimActivity;

import javax.microedition.io.ConnectionNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.05.13
 * Time: 18:52
 * To change this template use File | Settings | File Templates.
 */
public class General {

    private static General instance;
    public static boolean initialized = false;

    public void init(Context c) {
        instance = this;
    }

    public static General getInstance() {
        return instance;
    }

    public static Bitmap iconToBitmap(Icon icon) {
        return Bitmap.createBitmap(icon.getImage().getBitmap(), icon.x, icon.y, icon.getWidth(), icon.getHeight());
    }
    public static Bitmap imageToBitmap(Image image) {
        return Bitmap.createBitmap(image.getBitmap(), 0, 0, image.getWidth(), image.getHeight());
    }

    public static int getColor(int color) {
        return 0xff000000 | Scheme.getScheme()[color];
    }
    public static int getColorWithAlpha(int color) {
        return 0xdd000000 | Scheme.getScheme()[color];
    }

    public InputStream getResourceAsStream(Context c, Class origClass, String name) {
        try {
            if (name.startsWith("/")) {
                return c.getAssets().open(name.substring(1));
            } else {
                Package p = origClass.getPackage();
                if (p == null) {
                    return c.getAssets().open(name);
                } else {
                    String folder = origClass.getPackage().getName().replace('.', '/');
                    return c.getAssets().open(folder + "/" + name);
                }
            }
        } catch (IOException e) {
            //Logger.debug(e); // large output with BombusMod
            return null;
        }
    }

    public boolean platformRequest(Context c, String url) throws ConnectionNotFoundException {
        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            throw new ConnectionNotFoundException();
        }
        return true;
    }

    public static Bitmap avatarBitmap(byte[] buffer) {
        if (buffer == null) return null;
        DisplayMetrics metrics = new DisplayMetrics();
        SawimActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float scaleWidth = metrics.scaledDensity;
        float scaleHeight = metrics.scaledDensity;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
        int width = bitmap.getWidth();
        if (width > metrics.widthPixels)  {
            double k = (double)width/(double)metrics.widthPixels;
            int h = (int) (bitmap.getWidth()/k);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, metrics.widthPixels, h, matrix, true);
            bitmap.setDensity(metrics.densityDpi);
        } else {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.setDensity(metrics.densityDpi);
        }
        return bitmap;
    }

    private OnUpdateChat updateChatListener;
    public void setOnUpdateChat(OnUpdateChat l) {
        updateChatListener = l;
    }
    public OnUpdateChat getUpdateChatListener() {
        return updateChatListener;
    }
    public interface OnUpdateChat {
        void updateChat();
    }
}
