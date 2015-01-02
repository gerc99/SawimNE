package ru.sawim.icons;

import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import ru.sawim.R;
import ru.sawim.SawimApplication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

public class ImageList {

    private static Hashtable files = new Hashtable();
    private static ImageList instance;
    private Icon[] icons;
    private int width = 0;
    private int height = 0;

    public ImageList() {
        instance = this;
    }

    public static ImageList getInstance() {
        return instance;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    static public ImageList createImageList(String resName) {
        ImageList imgs = (ImageList) files.get(resName);
        if (null != imgs) {
            return imgs;
        }
        Vector tmpIcons = new Vector();
        ImageList icons = new ImageList();
        if (resName.equals("/jabber-status.png")) {
            tmpIcons.addElement(new Icon((BitmapDrawable) SawimApplication.getInstance().getResources().
                    getDrawable(R.drawable.online)));
            tmpIcons.addElement(new Icon((BitmapDrawable) SawimApplication.getInstance().getResources().
                    getDrawable(R.drawable.offline)));
            tmpIcons.addElement(new Icon((BitmapDrawable) SawimApplication.getInstance().getResources().
                    getDrawable(R.drawable.away)));
            tmpIcons.addElement(new Icon((BitmapDrawable) SawimApplication.getInstance().getResources().
                    getDrawable(R.drawable.dnd)));
            icons.add(tmpIcons);
        } else if (resName.equals("/vk-status.png")) {
            tmpIcons.addElement(new Icon((BitmapDrawable) SawimApplication.getInstance().getResources().
                    getDrawable(R.drawable.vk_online)));
            tmpIcons.addElement(new Icon((BitmapDrawable) SawimApplication.getInstance().getResources().
                    getDrawable(R.drawable.vk_offline)));
            icons.add(tmpIcons);
        } else {
            try {
                icons.load(resName, -1, -1);
            } catch (Exception e) {
            }
        }
        files.put(resName, icons);
        return icons;
    }

    public void load(String resName, int count) throws IOException {
        Image resImage = loadImage(resName);
        if (null == resImage) {
            return;
        }
        int imgHeight = resImage.getHeight();
        int imgWidth = resImage.getWidth();
        width = imgWidth / count;
        height = imgHeight;

        Vector tmpIcons = new Vector();
        int size = (int) SawimApplication.getInstance().getResources().getDisplayMetrics().density;
        for (int y = 0; y < imgHeight; y += height) {
            for (int x = 0; x < imgWidth; x += width) {
                Bitmap bitmap = Bitmap.createScaledBitmap(
                        Bitmap.createBitmap(resImage.getBitmap(), x, y, width, height), width * size, height * size, true);
                bitmap.setDensity(0);
                BitmapDrawable drawable = new BitmapDrawable(SawimApplication.getInstance().getResources(), bitmap);
                drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * 0.5), (int) (drawable.getIntrinsicHeight() * 0.5));
                drawable.setTargetDensity(SawimApplication.getInstance().getResources().getDisplayMetrics());
                tmpIcons.addElement(new Icon(drawable));
            }
        }
        add(tmpIcons);
    }

    private void add(Vector tmpIcons) {
        icons = new Icon[tmpIcons.size()];
        tmpIcons.copyInto(icons);
    }

    public Icon iconAt(int index) {
        if (0 <= index && index < size()) {
            return icons[index];
        }
        return null;
    }

    public int size() {
        return (null == icons) ? 0 : icons.length;
    }

    public void load(String resName, int width, int height) throws IOException {
        Image resImage = loadImage(resName);
        if (null == resImage) {
            return;
        }
        int imgHeight = resImage.getHeight();
        int imgWidth = resImage.getWidth();

        if (width == -1) {
            width = Math.min(imgHeight, imgWidth);
        }
        if (height == -1) {
            height = imgHeight;
        }

        Vector tmpIcons = new Vector();
        for (int y = 0; y < imgHeight; y += height) {
            for (int x = 0; x < imgWidth; x += width) {
                Bitmap bitmap = scalingIconForDPI(Bitmap.createBitmap(resImage.getBitmap(), x, y, width, height));
                BitmapDrawable drawable = new BitmapDrawable(SawimApplication.getInstance().getResources(), bitmap);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                tmpIcons.addElement(new Icon(drawable));
            }
        }
        add(tmpIcons);
    }

    public void loadSmiles() throws IOException {
        TypedArray smileyDrawables = SawimApplication.getInstance().getResources().obtainTypedArray(R.array.default_smileys_images);
        Vector tmpIcons = new Vector();
        for (int i = 0; i < smileyDrawables.length(); ++i) {
            BitmapDrawable smile = ((BitmapDrawable) SawimApplication.getInstance().getResources()
                    .getDrawable(smileyDrawables.getResourceId(i, 0)));
            smile.setBounds(0, 0, smile.getBitmap().getWidth(), smile.getBitmap().getHeight());
            tmpIcons.addElement(new Icon(smile));
        }
        add(tmpIcons);
    }

    public static Bitmap scalingIconForDPI(Bitmap originBitmap) {
        if (originBitmap != null) {
                switch (SawimApplication.getInstance().getResources().getDisplayMetrics().densityDpi) {
                    case 120:
                        if (originBitmap.getWidth() >= 6 && !SawimApplication.getContext().getResources().getBoolean(R.bool.is_tablet) && originBitmap.getHeight() == 16)
                            originBitmap = Bitmap.createScaledBitmap(originBitmap, 6, 6, true);
                        else if (originBitmap.getWidth() >= 8  && originBitmap.getHeight() == 16)
                            originBitmap = Bitmap.createScaledBitmap(originBitmap, 8, 8, true);
                        else originBitmap = Bitmap.createScaledBitmap(originBitmap, 16, 16, true);
                        break;
                    case 160:
                        if (originBitmap.getWidth() > 8 && originBitmap.getHeight() == 16)
                            originBitmap = Bitmap.createScaledBitmap(originBitmap, 8, 8, true);
                        break;
                    case 180:
                        if (originBitmap.getWidth() >= 24 && !SawimApplication.getContext().getResources().getBoolean(R.bool.is_tablet))
                            originBitmap = Bitmap.createScaledBitmap(originBitmap, 24, 24, true);
                        break;
                    default:
                        return originBitmap;
                }
                originBitmap.setDensity(SawimApplication.getInstance().getResources().getDisplayMetrics().densityDpi);
        }
        return originBitmap;
    }

    public static Bitmap scalingCaptchaIconForDPI(Bitmap originBitmap) {
        BitmapDrawable output = null;
        int density = (int) SawimApplication.getInstance().getResources().getDisplayMetrics().density;
        if (originBitmap != null) {
            originBitmap = Bitmap.createScaledBitmap(originBitmap, originBitmap.getWidth() * density, originBitmap.getHeight() * density, true);
            originBitmap = originBitmap.copy(Bitmap.Config.ARGB_4444, false);
            originBitmap.setDensity(SawimApplication.getInstance().getResources().getDisplayMetrics().densityDpi);
            output = new BitmapDrawable(SawimApplication.getInstance().getResources(), originBitmap);
            output.setBounds(0, 0, output.getIntrinsicWidth() * density, output.getIntrinsicHeight() * density);
        }
        return output.getBitmap();
    }

    public Image loadImage(String resName) {
        try {
            return createImage(resName);
        } catch (Exception e) {
        } catch (OutOfMemoryError out) {
        }
        return null;
    }

    public Image createImage(String name) throws IOException {
        InputStream is = SawimApplication.getResourceAsStream(name);
        if (is == null) {
            throw new IOException(name + " could not be found.");
        }
        return createImage(is);
    }

    public Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        return new Image(BitmapFactory.decodeByteArray(imageData, imageOffset, imageLength));
    }

    public Image createImage(InputStream is) throws IOException {
        byte[] imageBytes = new byte[1024];
        int num;
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        while ((num = is.read(imageBytes)) != -1) {
            ba.write(imageBytes, 0, num);
        }

        byte[] bytes = ba.toByteArray();
        return new Image(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
    }

    public Image createImage(int width, int height) {
        return new Image(width, height, false, 0x00FFFFFF);
    }
}