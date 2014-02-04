package sawim.modules;

import android.content.Context;
import android.os.Vibrator;
import ru.sawim.SawimApplication;
import ru.sawim.SawimApplication;
import ru.sawim.sound.SoundPlayer;
import sawim.Options;
import sawim.comm.Util;

public class Notify implements Runnable {

    public static final int NOTIFY_MESSAGE = 0;
    public static final int NOTIFY_ONLINE = 1;
    public static final int NOTIFY_TYPING = 2;
    public static final int NOTIFY_MULTIMESSAGE = 3;
    public static final int NOTIFY_ALARM = 4;
    public static final int NOTIFY_RECONNECT = 5;
    public static final int NOTIFY_BLOG = 6;
    private static final int VIBRA_OFF = 0;
    private static final int VIBRA_ON = 1;
    private static final int VIBRA_LOCKED_ONLY = 2;
    private static Notify _this = new Notify();
    private static String[] files = {null, null, null, null, null, null, null};
    private static String fileVibrate = null;
    final long[] pattern = {0, 200, 100, 100, 100, 200, 0};
    private final Object syncObject = new Object();
    private String nextMelody = null;
    private long nextPlayTime = 0;
    private int playingType = 0;
    private boolean isPlay;
    private SoundPlayer androidPlayer;

    private Notify() {
    }

    public static Notify getSound() {
        return _this;
    }

    private static boolean isSoundNotification(int notType) {
        switch (notType) {
            case NOTIFY_MESSAGE:
                return 0 < Options.getInt(Options.OPTION_MESS_NOTIF_MODE);

            case NOTIFY_ONLINE:
                return 0 < Options.getInt(Options.OPTION_ONLINE_NOTIF_MODE);
            case NOTIFY_BLOG:
                return Options.getBoolean(Options.OPTION_BLOG_NOTIFY);

            case NOTIFY_MULTIMESSAGE:
            case NOTIFY_TYPING:
                return false;
        }
        return true;
    }

    public static boolean isSound(int notType) {
        return !Options.getBoolean(Options.OPTION_SILENT_MODE) && isSoundNotification(notType);
    }

    private String getMimeType(String ext) {
        if ("mp3".equals(ext)) {
            return "audio/mpeg";
        }
        if ("mid".equals(ext) || "midi".equals(ext)) {
            return "audio/midi";
        }
        if ("amr".equals(ext)) {
            return "audio/amr";
        }
        if ("mmf".equals(ext)) {
            return "audio/mmf";
        }
        if ("imy".equals(ext)) {
            return "audio/iMelody";
        }
        if ("aac".equals(ext)) {
            return "audio/aac";
        }
        if ("m4a".equals(ext)) {
            return "audio/m4a";
        }
        return "audio/X-wav";
    }

    public boolean vibrate(final int duration) {
        final Vibrator vibrator = (Vibrator) SawimApplication.getInstance().getSystemService(Context.VIBRATOR_SERVICE);
        if (null == vibrator) {
            return false;
        }
        if (600 < duration) {
            vibrator.vibrate(pattern, -1);
        } else if (0 < duration) {
            vibrator.vibrate(duration);
        } else {
            vibrator.cancel();
        }
        return true;
    }

    private int getVolume() {
        return Options.getInt(Options.OPTION_NOTIFY_VOLUME);
    }

    private boolean isCompulsory(int notType) {
        switch (notType) {
            case NOTIFY_MESSAGE:
            case NOTIFY_MULTIMESSAGE:
            case NOTIFY_ALARM:
            case NOTIFY_BLOG:

                return true;
        }
        return false;
    }

    private void playNotify() {
        int notType = playingType;
        int vibrate = 0;
        if (NOTIFY_ALARM == notType) {
            vibrate = 1500;
        } else {

            int vibraKind = Options.getInt(Options.OPTION_VIBRATOR);
            if ((VIBRA_LOCKED_ONLY == vibraKind)) {
                vibraKind = VIBRA_OFF;
            }
            if ((VIBRA_OFF != vibraKind)
                    && ((NOTIFY_MESSAGE == notType) || (NOTIFY_MULTIMESSAGE == notType))) {
                vibrate = SawimApplication.isPaused() ? 500 : 200;
            }
        }

        int volume = getVolume();
        boolean play = isPlay && (volume > 0);
        String file = play ? files[notType] : null;

        if (0 < vibrate) {
            vibrate(vibrate);
        }
        if (null != file) {
            safePlay(file);

        }
    }

    private void closePlayer() {
        if (null != androidPlayer) {
            androidPlayer.close();
            androidPlayer = null;
        }
    }

    public void run() {
        try {
            playNotify();
        } catch (OutOfMemoryError err) {
        }
    }

    private void playNotification(boolean isPl, int notType) {
        isPlay = isPl;
        final long now = System.currentTimeMillis();
        long next = nextPlayTime;
        if (!isCompulsory(playingType) && isCompulsory(notType)) {
            next = 0;
        }
        if (now < next) {
            return;
        }
        if (NOTIFY_ALARM == notType) {
            if (!Options.getBoolean(Options.OPTION_ALARM)) {
                return;
            }
            next = now + 3000;

        } else {
            next = now + 2000;
        }
        nextPlayTime = next;
        playingType = notType;
        new Thread(this).start();
    }

    public void playSoundNotification(int notType) {
        synchronized (syncObject) {
            playNotification(isSound(notType), notType);
        }
    }

    public void playSoundForExtra(int notType) {
        synchronized (syncObject) {
            playNotification(true, notType);
        }
    }

    public void playSoundNotification(boolean isPl, int notType) {
        synchronized (syncObject) {
            playNotification(isPl, notType);
        }
    }

    private void safePlay(String file) {
        try {
            closePlayer();
            createPlayer(file);
            play();
        } catch (Exception e) {
            closePlayer();
        } catch (OutOfMemoryError err) {
            closePlayer();
        }
    }

    private boolean testSoundFile(String source) {
        try {
            createPlayer(source);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            closePlayer();
        }
    }

    private void createPlayer(String source) throws Exception {
        androidPlayer = new SoundPlayer();
        androidPlayer.play(source, getVolume());
    }

    private void play() throws Exception {
        androidPlayer.start();
    }

    private String selectSoundType(String name) {
        String[] exts = Util.explode("mp3|wav|mid|midi|mmf|amr|imy|aac|m4a", '|');
        for (int i = 0; i < exts.length; ++i) {
            String testFile = name + exts[i];
            if (testSoundFile(testFile)) {
                return testFile;
            }
        }
        return null;
    }

    public void changeSoundMode(boolean showPopup) {
        boolean newValue = !Options.getBoolean(Options.OPTION_SILENT_MODE);
        closePlayer();
        Options.setBoolean(Options.OPTION_SILENT_MODE, newValue);
        Options.safeSave();
        if (showPopup) {
            String text = newValue ? "#sound_is_off" : "#sound_is_on";
            //new Popup(JLocale.getString(text)).show();
        }
        vibrate(newValue ? 0 : 100);
    }

    public void initSounds() {
        files[NOTIFY_ONLINE] = selectSoundType("/online.");

        files[NOTIFY_MESSAGE] = selectSoundType("/message.");
        files[NOTIFY_TYPING] = selectSoundType("/typing.");
        files[NOTIFY_ALARM] = selectSoundType("/alarm.");
        files[NOTIFY_RECONNECT] = selectSoundType("/reconnect.");
        files[NOTIFY_BLOG] = selectSoundType("/blog.");

        if (testSoundFile("/vibrate.imy")) {
            fileVibrate = "/vibrate.imy";
        }
    }

    public boolean hasAnySound() {
        for (int i = 0; i < files.length; ++i) {
            if (null != files[i]) {
                return true;
            }
        }
        return false;
    }
}