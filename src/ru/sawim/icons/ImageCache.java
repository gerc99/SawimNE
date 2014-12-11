package ru.sawim.icons;


import android.graphics.Bitmap;
import android.text.TextUtils;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.comm.LruCache;
import ru.sawim.widget.Util;

import java.io.*;
import java.util.concurrent.Executor;

/**
 * Created by gerc on 06.09.2014.
 */
public class ImageCache {

    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
    private LruCache<String, Bitmap> bitmapLruCache = new LruCache<>(((int) Runtime.getRuntime().maxMemory() / 1024) / 8); // Use 1/8th of the available memory for this memory cache.
    private static ImageCache instance;
    private static final int AVATAR_SIZE = Util.dipToPixels(SawimApplication.getContext(), SawimApplication.AVATAR_SIZE);
    private ImageCache() {
    }

    public static ImageCache getInstance() {
        ImageCache localInstance = instance;
        if (localInstance == null) {
            synchronized (ImageCache.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new ImageCache();
                }
            }
        }
        return localInstance;
    }

    public Bitmap get(final File pathCacheFolder, Executor executor, final String hash, final Bitmap defaultImage, final OnImageLoadListener onImageLoadListener) {
        Bitmap bitmap = defaultImage;
        if (!TextUtils.isEmpty(hash)) {
            bitmap = bitmapLruCache.get(hash);
            if (bitmap == null) {
                if (defaultImage != null) {
                    bitmap = defaultImage;
                }
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = bitmapLruCache.get(hash);
                        if (bitmap == null) {
                            File file = getFile(pathCacheFolder, hash);
                            if (file.exists()) {
                                FileInputStream inputStream = null;
                                try {
                                    inputStream = new FileInputStream(file);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                byte fileContent[] = new byte[(int) file.length()];
                                try {
                                    if (inputStream != null) {
                                        inputStream.read(fileContent);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    if (inputStream != null) {
                                        try {
                                            inputStream.close();
                                        } catch (IOException ignore) {
                                        }
                                    }
                                }
                                Bitmap bmp = Util.decodeBitmap(fileContent, AVATAR_SIZE);
                                bitmap = RoundedAvatars.getRoundedBitmap(Util.getAvatarBitmap(bmp, AVATAR_SIZE, (bmp != null) ? bmp.getPixel(0, 0) : 0x00000000));
                                if (bitmap != null) {
                                    bitmapLruCache.put(hash, bitmap);
                                    if (onImageLoadListener != null) {
                                        onImageLoadListener.onLoad();
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
        return bitmap;
    }

    public boolean save(File pathCacheFolder, String id, String hash, byte[] avatarBytes) {
        File file = getFile(pathCacheFolder, hash);
        Bitmap bmp = Util.decodeBitmap(avatarBytes, AVATAR_SIZE);
        Bitmap bitmap = Util.getAvatarBitmap(bmp, AVATAR_SIZE, (bmp != null) ? bmp.getPixel(0,0) : 0x00000000);
        if (bitmap != null) {
            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                bitmap.compress(COMPRESS_FORMAT, 95, os);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        return false;
    }

    public void remove(final File pathCacheFolder, String hash) {
        File file = getFile(pathCacheFolder, hash);
        file.delete();
    }

    public boolean hasFile(File pathCacheFolder, String hash) {
        String[] files = pathCacheFolder.list();
        for (String file : files) {
            if (file.equals(hash)) {
                return true;
            }
        }
        return false;
    }

    private File getFile(File pathCacheFolder, String hash) {
        return new File(pathCacheFolder, hash.replace('/', '_').concat(".").concat(COMPRESS_FORMAT.name()));
    }

    public interface OnImageLoadListener {
        void onLoad();
    }
}
