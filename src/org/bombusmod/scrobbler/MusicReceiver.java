/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bombusmod.scrobbler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import ru.sawim.SawimService;

/**
 *
 * @author modi & Ivansuper
 */
public class MusicReceiver extends BroadcastReceiver {

    private SawimService service;

    public MusicReceiver(SawimService svc) {
        service = svc;
    }

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        String artist = arg1.getStringExtra("artist");
        String track = arg1.getStringExtra("track");

        if (artist == null && track == null) {
        } else if (artist == null) {
            artist = "Unknown";
        } else if (track == null) {
            track = "Unknown";
        }
        publishTune(artist, track);
    }

    protected void publishTune(String artist, String track) {

    }

    public IntentFilter getIntentFilter() {
        //audio scrobbler
        IntentFilter filter = new IntentFilter();
        //Google Android player
        filter.addAction("com.android.music.playstatechanged");
        filter.addAction("com.android.music.playbackcomplete");
        filter.addAction("com.android.music.metachanged");
        //HTC Music
        filter.addAction("com.htc.music.playstatechanged");
        filter.addAction("com.htc.music.playbackcomplete");
        filter.addAction("com.htc.music.metachanged");
        //MIUI Player
        filter.addAction("com.miui.player.playstatechanged");
        filter.addAction("com.miui.player.playbackcomplete");
        filter.addAction("com.miui.player.metachanged");
        //Real
        filter.addAction("com.real.IMP.playstatechanged");
        filter.addAction("com.real.IMP.playbackcomplete");
        filter.addAction("com.real.IMP.metachanged");
        //SEMC Music Player
        filter.addAction("com.sonyericsson.music.playbackcontrol.ACTION_TRACK_STARTED");
        filter.addAction("com.sonyericsson.music.playbackcontrol.ACTION_PAUSED");
        filter.addAction("com.sonyericsson.music.TRACK_COMPLETED");
        filter.addAction("com.sonyericsson.music.metachanged");
        filter.addAction("com.sonyericsson.music.playbackcomplete");
        filter.addAction("com.sonyericsson.music.playstatechanged");
        //rdio
        filter.addAction("com.rdio.android.metachanged");
        filter.addAction("com.rdio.android.playstatechanged");
        //Samsung Music Player
        filter.addAction("com.samsung.sec.android.MusicPlayer.playstatechanged");
        filter.addAction("com.samsung.sec.android.MusicPlayer.playbackcomplete");
        filter.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        filter.addAction("com.sec.android.app.music.playstatechanged");
        filter.addAction("com.sec.android.app.music.playbackcomplete");
        filter.addAction("com.sec.android.app.music.metachanged");
        //Winamp
        filter.addAction("com.nullsoft.winamp.playstatechanged");
        //Amazon
        filter.addAction("com.amazon.mp3.playstatechanged");
        //Rhapsody
        filter.addAction("com.rhapsody.playstatechanged");
        //PowerAmp
        filter.addAction("com.maxmpz.audioplayer.playstatechanged");
        //will be added any....
        //scrobblers detect for players (poweramp for example)
        //Last.fm
        filter.addAction("fm.last.android.metachanged");
        filter.addAction("fm.last.android.playbackpaused");
        filter.addAction("fm.last.android.playbackcomplete");
        //A simple last.fm scrobbler
        filter.addAction("com.adam.aslfms.notify.playstatechanged");
        //Scrobble Droid
        filter.addAction("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
        return filter;
    }
}