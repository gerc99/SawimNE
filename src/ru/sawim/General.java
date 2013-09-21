package ru.sawim;

import DrawControls.icons.ImageList;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import sawim.Options;
import sawim.Updater;
import sawim.chat.ChatHistory;
import ru.sawim.activities.SawimActivity;
import sawim.roster.Roster;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.modules.*;
import sawim.search.Search;
import sawim.util.JLocale;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.05.13
 * Time: 18:52
 * To change this template use File | Settings | File Templates.
 */
public class General {

    public static final String NAME = SawimApplication.getInstance().getString(R.string.app_name);
    public static final String VERSION = SawimApplication.getInstance().getVersion();
    public static final String PHONE = "Android/" + android.os.Build.MODEL
            + "/" + android.os.Build.VERSION.RELEASE;

    public static ImageList affiliationIcons = ImageList.createImageList("/jabber-affiliations.png");
    public static BitmapDrawable usersIcon = ImageList.createImageList("/participants.png").iconAt(0).getImage();
    private static General instance;
    public static SawimActivity sawimActivity;
    public static String message = "";
    private boolean paused = true;
    private static int fontSize;
    public static boolean hideIconsClient;
    public static boolean showStatusLine;
    public static int sortType;

    public static General getInstance() {
        return instance;
    }

    public void startApp() {
        /*if (!paused && (null != General.instance)) {
            return;
        }*/
        instance = this;
        instance.paused = false;

        ru.sawim.config.HomeDirectory.init();
        JLocale.loadLanguageList();
        Scheme.load();
        Options.loadOptions();
        new ru.sawim.config.Options().load();
        JLocale.setCurrUiLanguage(/*Options.getString(Options.OPTION_UI_LANGUAGE)*/JLocale.getSystemLanguage());
        Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
        updateOptions();
        Updater.startUIUpdater();

        try {
            Notify.getSound().initSounds();
            gc();
            Emotions.instance.load();
            StringConvertor.load();
            Answerer.getInstance().load();
            gc();

            Options.loadAccounts();
            Roster.getInstance().initAccounts();
            Roster.getInstance().loadAccounts();
            sawim.modules.tracking.Tracking.loadTrackingFromRMS();
        } catch (Exception e) {
            DebugLog.panic("init", e);
            DebugLog.instance.activate();
        }
        DebugLog.startTests();
    }

    public static void updateOptions() {
        fontSize = Options.getInt(Options.OPTION_FONT_SCHEME);
        showStatusLine = Options.getBoolean(Options.OPTION_SHOW_STATUS_LINE);
        hideIconsClient = Options.getBoolean(Options.OPTION_HIDE_ICONS_CLIENTS);
        sortType = Options.getInt(Options.OPTION_CL_SORT_BY);
    }

    public void quit() {
        Roster cl = Roster.getInstance();
        /*boolean wait;
        try {
            wait = roster.disconnect();
        } catch (Exception e) {
            return;
        }*/
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
        }
        cl.safeSave();
        //if (wait) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e1) {
        }
        //}
        ChatHistory.instance.saveUnreadMessages();
    }

    public static long getCurrentGmtTime() {
        return System.currentTimeMillis() / 1000
                + Options.getInt(Options.OPTION_LOCAL_OFFSET) * 3600;
    }

    public static void openUrl(String url) {
        Search search = Roster.getInstance().getCurrentProtocol().getSearchForm();
        search.show(Util.getUrlWithoutProtocol(url), true);
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

    public static int getFontSize() {
        return fontSize;
    }

    public static boolean isTablet(Context context) {
        boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }

    public static Bitmap avatarBitmap(byte[] buffer) {
        if (buffer == null) return null;
        DisplayMetrics metrics = new DisplayMetrics();
        sawimActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
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

    public static View getDivider(Context context, boolean vertical, int color) {
        View v = new View(context);
        if (vertical)
            v.setLayoutParams(new LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.WRAP_CONTENT));
        else
            v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        v.setBackgroundColor(color);
        return v;
    }

    public static int dipToPixels(Context context, int dipValue) {
        Resources r = context.getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dipValue, r.getDisplayMetrics());
        return px;
    }
}
