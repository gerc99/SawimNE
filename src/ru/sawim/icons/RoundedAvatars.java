package ru.sawim.icons;

import android.graphics.*;

public class RoundedAvatars {
    public static Bitmap getRoundedBitmap(Bitmap bitmap) {
        Bitmap output = null;
        try {
            output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(output);
            final int color = Color.RED;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            final RectF rectF = new RectF(rect);
            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawOval(rectF, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            if (!bitmap.isRecycled()) bitmap.recycle();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return output;
    }

    public static Bitmap getRoundedBitmapNotExsist(int AVATAR_SIZE) {
        Bitmap output = null;
        try {
            output = Bitmap.createBitmap(AVATAR_SIZE,AVATAR_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            final int color = 0xff33B5E5;
            final Paint paint_main = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint paint_s = new Paint();
            final Rect rect = new Rect(0, 0, AVATAR_SIZE,AVATAR_SIZE);
            paint_main.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint_main.setColor(color);
            paint_s.setColor(Color.WHITE);
            float target = (float)rect.height()*.6f;
            paint_s.setTextSize(target);
            paint_s.setAntiAlias(true);
            paint_s.setTextAlign(Paint.Align.CENTER);
            Rect bounds = new Rect();
            paint_s.setTextAlign(Paint.Align.CENTER);
            String gText = "S";
            paint_s.getTextBounds(gText, 0, gText.length(), bounds);
            int y = (output.getHeight() + bounds.height())/2;
            canvas.drawCircle(AVATAR_SIZE / 2, AVATAR_SIZE / 2, AVATAR_SIZE / 2, paint_main);
            canvas.drawText("S", rect.width() / 2, y, paint_s);
            paint_main.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(output, rect, rect, paint_main);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return output;
    }

}
