package ru.sawim.icons;

import android.graphics.Bitmap;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 08.06.13
 * Time: 20:41
 * To change this template use File | Settings | File Templates.
 */
public class Image {

    private Bitmap bitmap;

    public Image(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Image(int width, int height, boolean withAlpha, int fillColor) {
        if (withAlpha) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(fillColor);
        } else {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            bitmap.eraseColor(0xff000000 | fillColor);
        }
    }

    public Image scale(int w, int h) {
        return new Image(Bitmap.createScaledBitmap(bitmap, w, h, false));
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getWidth() {
        return bitmap == null ? 0 : bitmap.getWidth();
    }

    public int getHeight() {
        return bitmap == null ? 0 : bitmap.getHeight();
    }
}
