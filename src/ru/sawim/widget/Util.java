package ru.sawim.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 26.09.13
 * Time: 18:58
 * To change this template use File | Settings | File Templates.
 */
public class Util {

    public static Bitmap avatarBitmap(Activity activity, byte[] buffer) {
        if (buffer == null) return null;
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return decodeBitmap(buffer, metrics.widthPixels, metrics.heightPixels);
    }

    public static Bitmap getAvatarBitmap(byte[] buffer, int size) {
        Bitmap avatarTmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
        if (avatarTmp == null) return null;
        Bitmap scaled = ThumbnailUtils.extractThumbnail(avatarTmp, size, size, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return scaled;
    }

    public static Bitmap getAvatarBitmap(Bitmap avatarTmp, int size, int backgroundColor) {
        Bitmap avatar;
        if (avatarTmp == null) return null;
        int h = avatarTmp.getHeight();
        int w = avatarTmp.getWidth();
        if (h > w) {
            w = (w * size) / h;
            h = size;
        } else {
            h = (h * size) / w;
            w = size;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(avatarTmp, w, h, true);
        if (h == w) {
            avatar = scaled;
        } else {
            avatar = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(avatar);
            c.drawColor(backgroundColor);
            c.drawBitmap(scaled, (size - w) / 2, (size - h) / 2, null);
        }
        return avatar;
    }

    public static Bitmap decodeBitmap(byte[] bytes, int maxSize) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        int h = opts.outHeight;
        int w = opts.outWidth;
        int scaleFactor = 1;
        while (h / 2 >= maxSize && w / 2 >= maxSize) {
            w /= 2; h /= 2;
            scaleFactor *= 2;
        }
        opts = new BitmapFactory.Options();
        opts.inSampleSize = scaleFactor;
        opts.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
    }

    public static Bitmap decodeBitmap(byte[] bytes, int reqWidth, int reqHeight) {
        Bitmap tmp;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        opts = new BitmapFactory.Options();
        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
        opts.inJustDecodeBounds = false;
        tmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
        return tmp;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    public static CharSequence highlightText(String search, String originalText, int color) {
        if (search == null || search.isEmpty()) {
            return originalText;
        }
        int start = originalText.toLowerCase().indexOf(search);
        if (start < 0) {
            return originalText;
        } else {
            Spannable highlighted = new SpannableString(originalText);
            while (start >= 0) {
                int spanStart = start;
                int spanEnd = start + search.length();
                highlighted.setSpan(new BackgroundColorSpan(color), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = originalText.indexOf(search, spanEnd);
            }
            return highlighted;
        }
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

    public static void hideKeyboard(Activity activity) {
        if (activity.getCurrentFocus() != null)
            ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    public static int getSystemBackground(Context context) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
        int background = a.getResourceId(0, 0);
        a.recycle();
        return background;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getColorFromAttribute(Context context, int resid) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(resid, typedValue, true);
        int color = typedValue.data;
        return color;
    }

    public static Drawable getDrawableFromAttribute(Context context, int theme, int resid) {
        TypedArray a = context.getTheme().obtainStyledAttributes(theme, new int[]{resid});
        int attributeResourceId = a.getResourceId(0, 0);
        Drawable drawable = context.getResources().getDrawable(attributeResourceId);
        a.recycle();
        return drawable;
    }

    public static int dipToPixels(Context context, int dipValue) {
        Resources r = context.getResources();
        int px = (int) (dipValue * r.getDisplayMetrics().density + 0.5f);
        return px;
    }

    public static void setSelectableItemBackground(View view) {
        TypedArray typedArray = view.getContext().obtainStyledAttributes(new int[]{R.attr.selectableItemBackground});
        int backgroundResource = typedArray.getResourceId(0, 0);
        view.setBackgroundResource(backgroundResource);
        typedArray.recycle();
    }
}
