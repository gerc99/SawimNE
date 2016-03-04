package ru.sawim.icons;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import protocol.xmpp.Vcard;
import protocol.xmpp.Xmpp;
import ru.sawim.SawimApplication;
import ru.sawim.comm.LruCache;
import ru.sawim.io.FileSystem;
import ru.sawim.roster.RosterHelper;
import ru.sawim.widget.Util;

import java.io.*;

/**
 * Created by gerc on 06.09.2014.
 */
public class AvatarCache {

    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
    private LruCache<String, Bitmap> bitmapLruCache = new LruCache<>(((int) Runtime.getRuntime().maxMemory() / 1024) / 8); // Use 1/8th of the available memory for this memory cache.
    private static AvatarCache instance;

    private static final int AVATAR_SIZE = Util.dipToPixels(SawimApplication.getContext(), SawimApplication.AVATAR_SIZE);
    private static final File avatarsFolder = FileSystem.openDir(FileSystem.AVATARS);

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private AvatarCache() {
    }

    public static AvatarCache getInstance() {
        AvatarCache localInstance = instance;
        if (localInstance == null) {
            synchronized (AvatarCache.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new AvatarCache();
                }
            }
        }
        return localInstance;
    }

    public void load(final String id, final String hash, final String nick, final OnImageLoadListener onImageLoadListener) {
        Bitmap bitmap = bitmapLruCache.get(id);
        if (onImageLoadListener != null) {
            onImageLoadListener.onLoad(bitmap);
        }
        if (bitmap == null) {
            SawimApplication.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = bitmapLruCache.get(id);
                    if (bitmap == null) {
                        File file = getFile(avatarsFolder, id, hash == null ? "" : hash);
                        if (file.exists()) {
                            bitmap = Util.getAvatarBitmap(ru.sawim.comm.Util.fileToArrayBytes(file), AVATAR_SIZE);
                            post(bitmap, id, onImageLoadListener);
                        } else {
                            if (TextUtils.isEmpty(hash)) {
                                String character = String.valueOf(nick.charAt(0));
                                bitmap = Avatars.getRoundedBitmap(character, Avatars.getColorForName(character), Color.WHITE, AVATAR_SIZE);
                                save(file, bitmap);
                                post(bitmap, id, onImageLoadListener);
                            } else {
                                Vcard.getVCard(((Xmpp) RosterHelper.getInstance().getProtocol(0)).getConnection(), id, new Vcard.OnAvatarLoadListener() {
                                    @Override
                                    public void onLoaded(String avatarHash, byte[] avatarBytes) {
                                        if (id != null) {
                                            File file = getFile(avatarsFolder, id, avatarHash);
                                            if (file.exists()) {
                                                Bitmap bitmap = Util.getAvatarBitmap(ru.sawim.comm.Util.fileToArrayBytes(file), AVATAR_SIZE);
                                                post(bitmap, id, onImageLoadListener);
                                                return;
                                            }
                                            Bitmap bitmap = Util.getAvatarBitmap(avatarBytes, AVATAR_SIZE);
                                            if (bitmap == null) return;
                                            bitmap = Avatars.getRoundedBitmap(bitmap);
                                            save(file, bitmap);
                                            post(bitmap, id, onImageLoadListener);
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }
    }

    private void post(final Bitmap bitmap, final String id, final OnImageLoadListener onImageLoadListener) {
        if (bitmap != null) {
            bitmapLruCache.put(id, bitmap);
            if (onImageLoadListener != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onImageLoadListener != null) {
                            onImageLoadListener.onLoad(bitmap);
                        }
                    }
                });
            }
        }
    }

    public boolean save(File file, Bitmap bitmap) {
        if (file == null) return false;
        if (file.exists()) return true;
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
                    } catch (IOException ignore) {}
                }
            }
        }
        return false;
    }

    public void remove(final File pathCacheFolder, String id, String hash) {
        File file = getFile(pathCacheFolder, id, hash);
        file.delete();
    }

    public boolean hasFile(File pathCacheFolder, String hash) {
        String[] files = pathCacheFolder.list();
        if (files == null) return false;
        for (String file : files) {
            if (file.equals(hash)) {
                return true;
            }
        }
        return false;
    }

    private File getFile(File pathCacheFolder, String id, String hash) {
        return new File(pathCacheFolder, id.replace('/', '_').concat("_").concat(hash.replace('/', '_').concat(".").concat(COMPRESS_FORMAT.name())));
    }

    public interface OnImageLoadListener {
        void onLoad(Bitmap b);
    }
}
