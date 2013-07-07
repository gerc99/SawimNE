package ru.sawim;

import DrawControls.icons.Icon;
import DrawControls.icons.Image;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.text.ClipboardManager;
import android.util.DisplayMetrics;
import sawim.Options;
import sawim.Updater;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.MessData;
import ru.sawim.activities.SawimActivity;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.modules.*;
import sawim.search.Search;
import sawim.util.JLocale;

import javax.microedition.io.ConnectionNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.05.13
 * Time: 18:52
 * To change this template use File | Settings | File Templates.
 */
public class General {

    private static General instance;
    public static final String NAME = "Sawim NE";
    public static final String VERSION = "1.2";
    public static final String PHONE = "android/" + android.os.Build.MODEL
            + "/" + android.os.Build.VERSION.RELEASE;

    private boolean paused = true;

    public void init() {
        instance = this;
    }

    public static General getInstance() {
        return instance;
    }

    public static long getCurrentGmtTime() {
        return System.currentTimeMillis() / 1000
                + Options.getInt(Options.OPTION_LOCAL_OFFSET) * 3600;
    }

    private static void platformRequestUrl(String url) throws ConnectionNotFoundException {
        if (-1 == url.indexOf(':')) {
            url = "xmpp:" + url;
        }
        if (url.startsWith("xmpp:")) {
            Search search = ContactList.getInstance().getManager().getCurrentProtocol().getSearchForm();
            search.show(Util.getUrlWithoutProtocol(url));
            return;
        }
    }

    public static java.io.InputStream getResourceAsStream(String name) {
        InputStream in;
        in = sawim.modules.fs.FileSystem.openSawimFile(name);
        if (null == in) {
            try {
                in = SawimApplication.getInstance().getAssets().open(name.substring(1));
            } catch (Exception ignored) {
            }
        }
        return in;
    }

    public void startApp() {
        if (!paused && (null != General.instance)) {
            return;
        }
        General.instance = this;
        instance.paused = false;

        ru.sawim.config.HomeDirectory.init();
        JLocale.loadLanguageList();
        Scheme.load();
        Options.loadOptions();
        new ru.sawim.config.Options().load();
        JLocale.setCurrUiLanguage(Options.getString(Options.OPTION_UI_LANGUAGE));
        Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
        Updater.startUIUpdater();

        try {
            Notify.getSound().initSounds();
            gc();
            Emotions.instance.load();
            StringConvertor.load();
            Answerer.getInstance().load();
            gc();
            DebugLog.startTests();

            Options.loadAccounts();
            ContactList.getInstance().initUI();
            ContactList.getInstance().initAccounts();
            ContactList.getInstance().loadAccounts();
            sawim.modules.tracking.Tracking.loadTrackingFromRMS();
        } catch (Exception e) {
            DebugLog.panic("init", e);
            DebugLog.instance.activate();
        }
    }

    public void quit() {
        ContactList cl = ContactList.getInstance();
        boolean wait;
        try {
            wait = cl.disconnect();
        } catch (Exception e) {
            return;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {

        }
        cl.safeSave();
        if (wait) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
            }
        }
        ChatHistory.instance.saveUnreadMessages();
    }

    public static boolean isPaused() {
        return instance.paused;
    }

    public static void maximize() {
        AutoAbsence.instance.online();
        instance.paused = false;
    }

    public static void minimize() {
        sawim.modules.AutoAbsence.instance.userActivity();
        instance.paused = true;
        AutoAbsence.instance.away();
    }

    public static void gc() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (Exception e) {
        }
    }

    public static Bitmap iconToBitmap(Icon icon) {
        return Bitmap.createBitmap(icon.getImage().getBitmap(), icon.x, icon.y, icon.getWidth(), icon.getHeight());
    }
    public static Bitmap imageToBitmap(Image image) {
        return Bitmap.createBitmap(image.getBitmap(), 0, 0, image.getWidth(), image.getHeight());
    }

    public static int getColor(int color) {
        return 0xff000000 | Scheme.getScheme()[color];
    }
    public static int getColorWithAlpha(int color) {
        return 0xdd000000 | Scheme.getScheme()[color];
    }

    public InputStream getResourceAsStream(Context c, Class origClass, String name) {
        try {
            if (name.startsWith("/")) {
                return c.getAssets().open(name.substring(1));
            } else {
                Package p = origClass.getPackage();
                if (p == null) {
                    return c.getAssets().open(name);
                } else {
                    String folder = origClass.getPackage().getName().replace('.', '/');
                    return c.getAssets().open(folder + "/" + name);
                }
            }
        } catch (IOException e) {
            //Logger.debug(e); // large output with BombusMod
            return null;
        }
    }

    public boolean platformRequest(Context c, String url) throws ConnectionNotFoundException {
        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            throw new ConnectionNotFoundException();
        }
        return true;
    }

    public static boolean isTablet(Context context) {
        boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }

    public static Bitmap avatarBitmap(byte[] buffer) {
        if (buffer == null) return null;
        DisplayMetrics metrics = new DisplayMetrics();
        SawimActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
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

    private OnUpdateChat updateChatListener;
    public void setOnUpdateChat(OnUpdateChat l) {
        updateChatListener = l;
    }

    public OnUpdateChat getUpdateChatListener() {
        return updateChatListener;
    }

    public interface OnUpdateChat {
        void updateChat();
        void addMessage(Chat chat, MessData messData);
        void updateMucList();
    }
}
