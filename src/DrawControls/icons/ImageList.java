package DrawControls.icons;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import ru.sawim.General;
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

    public ImageList() {
        instance = this;
    }

    public static ImageList getInstance() {
        return instance;
    }

    static public ImageList createImageList(String resName) {
        ImageList imgs = (ImageList) files.get(resName);
        if (null != imgs) {
            return imgs;
        }
        ImageList icons = new ImageList();
        try {
            icons.load(resName, -1, -1);
        } catch (Exception e) {
        }
        files.put(resName, icons);
        return icons;
    }
    
    /*public void load(String resName, int count) throws IOException {
        Image resImage = loadImage(resName);
        if (null == resImage) {
            return;
        }
        int imgHeight = resImage.getHeight();
        int imgWidth = resImage.getWidth();
        width = imgWidth / count;
        height = imgHeight;

        Vector tmpIcons = new Vector();
        for (int y = 0; y < imgHeight; y += height) {
            for (int x = 0; x < imgWidth; x += width) {
                Bitmap bitmap = Bitmap.createBitmap(resImage.getBitmap(), x, y, width, height);
                Icon icon = new Icon(bitmap);

                tmpIcons.addElement(icon);
            }
        }
        icons = new Icon[tmpIcons.size()];
        tmpIcons.copyInto(icons);
    }*/

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
                //Bitmap bitmap = Bitmap.createBitmap(resImage.getBitmap(), x, y, width, height);
                Bitmap bitmap = scalingIconForDPI(Bitmap.createBitmap(resImage.getBitmap(), x, y, width, height));
                Icon icon = new Icon(bitmap);
                tmpIcons.addElement(icon);
            }
        }
        icons = new Icon[tmpIcons.size()];
        tmpIcons.copyInto(icons);
    }

    public static Bitmap scalingIconForDPI(Bitmap originBitmap) {
        BitmapDrawable output = null;
        if (originBitmap != null) {
            switch (SawimApplication.getInstance().getResources().getDisplayMetrics().densityDpi) {
                case 120:
                    if (originBitmap.getWidth() > 16)
                        originBitmap = Bitmap.createScaledBitmap(originBitmap, 16, 16, true);
                    else
                        return originBitmap;
                    break;
                case 160:
                    if (originBitmap.getWidth() > 24)
                        originBitmap = Bitmap.createScaledBitmap(originBitmap, 24, 24, true);
                    else
                        return originBitmap;
                    break;
                default:
                    return originBitmap;
            }
            originBitmap.setDensity(0);
            output = new BitmapDrawable(SawimApplication.getInstance().getResources(), originBitmap);
            output.setBounds(0, 0, (int) (output.getIntrinsicWidth() * 0.5), (int) (output.getIntrinsicHeight() * 0.5));
        }
        return output.getBitmap();
    }

    public static Bitmap scalingCaptchaIconForDPI(Bitmap originBitmap) {
        BitmapDrawable output = null;
        int density = (int) SawimApplication.getInstance().getResources().getDisplayMetrics().density;
        if (originBitmap != null) {
            originBitmap = Bitmap.createScaledBitmap(originBitmap, originBitmap.getWidth() * density, originBitmap.getHeight() * density, true);
            originBitmap = originBitmap.copy(Bitmap.Config.ARGB_4444, false);
            originBitmap.setDensity(0);
            output = new BitmapDrawable(SawimApplication.getInstance().getResources(), originBitmap);
            output.setBounds(0, 0, (int) (output.getIntrinsicWidth() * density), (int) (output.getIntrinsicHeight() * density));
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
        InputStream is = General.getResourceAsStream(name);
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