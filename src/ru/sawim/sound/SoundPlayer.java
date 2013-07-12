package ru.sawim.sound;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import ru.sawim.SawimApplication;
import sawim.modules.fs.FileSystem;
import sawim.modules.fs.JSR75FileSystem;

import java.io.IOException;

/**
 * @author vladimir
 */
public class SoundPlayer implements MediaPlayer.OnCompletionListener {
    private MediaPlayer androidPlayer;

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.release();
    }

    public void close() {
        if (null != androidPlayer) {
            androidPlayer.release();
            androidPlayer = null;
        }
    }

    public void play(String source, int volume) throws IOException {
        AudioManager audioManager = (AudioManager) SawimApplication.getInstance().getSystemService(Context.AUDIO_SERVICE);

        if (AudioManager.RINGER_MODE_NORMAL == audioManager.getRingerMode()) {
            playIt(source.substring(1), volume);
        } else {
            close();
        }
    }

    private void playIt(String source, int volume) throws IOException {
        androidPlayer = new MediaPlayer();
        try {
            String in = openFile(source);
            if (null == in) {
                AssetFileDescriptor afd = SawimApplication.getInstance().getAssets().openFd(source);
                androidPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            } else {
                androidPlayer.setDataSource(in);
            }
            androidPlayer.prepare();
            androidPlayer.setVolume(volume / 100f, volume / 100f);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public static String openFile(String file) {
        JSR75FileSystem fs = FileSystem.getInstance();
        String in = null;
        try {
            fs.openFile(FileSystem.getSawimHome() + FileSystem.RES + "/" + file);
            in = fs.getAbsolutePath();
        } catch (Exception ignored) {
        }
        fs.close();
        return in;
    }

    public void start() {
        if (null != androidPlayer) {
            androidPlayer.setOnCompletionListener(this);
            androidPlayer.start();
        }
    }
}
