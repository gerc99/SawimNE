package DrawControls.icons;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import ru.sawim.General;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

public class ImageList {

    private Icon[] icons;
    protected int width = 0;
    protected int height = 0;
    private static Hashtable files = new Hashtable();
    private static ImageList instance;

    public ImageList() {
         instance = this;
    }

    public static ImageList getInstance() {
        return instance;
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

        this.width = width;
        this.height = height;
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