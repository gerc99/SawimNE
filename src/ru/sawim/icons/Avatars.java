package ru.sawim.icons;

import android.graphics.*;

import ru.sawim.R;
import ru.sawim.SawimApplication;

public class Avatars {

    public static Bitmap getRoundedBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final Bitmap outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        final Path path = new Path();
        path.addCircle(
                (float)(width / 2)
                , (float)(height / 2)
                , (float) Math.min(width, (height / 2))
                , Path.Direction.CCW);

        final Canvas canvas = new Canvas(outputBitmap);
        canvas.clipPath(path);
        canvas.drawBitmap(bitmap, 0, 0, null);
        return outputBitmap;
    }

    public static Bitmap getRoundedBitmap_(Bitmap bitmap) {
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

    public static Bitmap getRoundedBitmap2(Bitmap bitmap) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Rect rect = new Rect(0, 0, bitmapWidth, bitmapHeight);
        RectF rectF = new RectF(rect);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);

        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);

        canvas.drawOval(rectF, paint);

        return output;
    }

    public static Bitmap getRoundedBitmap(String letter, int backColor, int letterColor, int avatarSize) {
        int bitmapWidth = avatarSize;
        int bitmapHeight = avatarSize;

        Bitmap output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Rect rect = new Rect(0, 0, bitmapWidth, bitmapHeight);
        RectF rectF = new RectF(rect);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(backColor);

        canvas.drawOval(rectF, paint);

        Paint paintLetter = new Paint();
        paintLetter.setColor(letterColor);
        float target = (float) rect.height() * .6f;
        paintLetter.setTextSize(target);
        paintLetter.setAntiAlias(true);
        paintLetter.setTextAlign(Paint.Align.CENTER);
        Rect bounds = new Rect();
        paintLetter.getTextBounds(letter, 0, letter.length(), bounds);
        int y = output.getHeight() / 2 + bounds.height() / 2;
        canvas.drawText(letter, rect.width() / 2, y, paintLetter);
        return output;
    }

    public static Bitmap getSquareBitmap(String letter, int backColor, int letterColor, int avatarSize) {
        Bitmap output = null;
        try {
            output = Bitmap.createBitmap(avatarSize, avatarSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            Paint paintMain = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint paintLetter = new Paint();
            Rect rect = new Rect(0, 0, avatarSize, avatarSize);
            paintMain.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paintMain.setColor(backColor);
            paintLetter.setColor(letterColor);
            float target = (float) rect.height() * .6f;
            paintLetter.setTextSize(target);
            paintLetter.setAntiAlias(true);
            paintLetter.setTextAlign(Paint.Align.CENTER);
            Rect bounds = new Rect();
            paintLetter.getTextBounds(letter, 0, letter.length(), bounds);
            int y = (output.getHeight() + bounds.height()) / 2;
            canvas.drawRect(0, 0, avatarSize, avatarSize, paintMain);
            canvas.drawText(letter, rect.width() / 2, y, paintLetter);
            paintMain.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(output, rect, rect, paintMain);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return output;
    }

    public static int getColorForName(String character) {
        int colors[] = SawimApplication.getContext().getResources().getIntArray(R.array.avatar_colors);
        if (character.isEmpty()) {
            return colors[colors.length - 1];
        }
        return colors[(int) ((character.hashCode() & 0xffffffffl) % colors.length)];
    }
}
