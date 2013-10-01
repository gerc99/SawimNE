package ru.sawim.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import ru.sawim.General;

import java.io.*;
import java.net.*;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 26.09.13
 * Time: 18:58
 * To change this template use File | Settings | File Templates.
 */
public class Util {

    public static boolean isTablet(Context context) {
        boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }

    public static Bitmap decodeAndResizeBitmap(String url) {
        Bitmap bitmap = null;
        try {
            BufferedInputStream is = new BufferedInputStream(new URL(url).openConnection().getInputStream());
            is.mark(0);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, o);
            o.inJustDecodeBounds = false;
            is.reset();
            int imageWidth = o.outWidth;
            float s = imageWidth / o.outHeight;
            int screenWidth = General.currentActivity == null ? 300 : General.currentActivity.getWindowManager().getDefaultDisplay().getWidth();
            if (imageWidth > screenWidth) imageWidth = screenWidth;
            Log.e("ResizeBitmap", "" + o.outHeight);
            bitmap = BitmapFactory.decodeStream(is, null, o);
            bitmap = Bitmap.createScaledBitmap(bitmap, imageWidth, (int)(imageWidth / s), true);
            is.close();
            bitmap.setDensity(0);
        } catch (Exception e) {
            Log.e("ResizeBitmap", "Remote Image Exception", e);
        }
        return bitmap;
    }

    public static Bitmap avatarBitmap(byte[] buffer) {
        if (buffer == null) return null;
        DisplayMetrics metrics = new DisplayMetrics();
        General.currentActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
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

    public static View getDivider(Context context, boolean vertical, int color) {
        View v = new View(context);
        if (vertical)
            v.setLayoutParams(new LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.WRAP_CONTENT));
        else
            v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        v.setBackgroundColor(color);
        return v;
    }

    public static int dipToPixels(Context context, int dipValue) {
        Resources r = context.getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dipValue, r.getDisplayMetrics());
        return px;
    }
}
