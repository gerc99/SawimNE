/**
 *  MicroEmulator
 *  Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 *
 */

package ru.sawim.activities;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import protocol.Contact;
import ru.sawim.view.*;
import sawim.ExternalApi;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.roster.Roster;
import org.microemu.util.AndroidLoggerAppender;
import org.microemu.log.Logger;
import protocol.Protocol;
import ru.sawim.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class SawimActivity extends ActionBarActivity {

    public static final String LOG_TAG = "SawimActivity";
    public static String NOTIFY = "ru.sawim.notify";
    public static ActionBar actionBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        General.currentActivity = this;
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.main);
        ExternalApi.instance.setActivity(this);

        Logger.removeAllAppenders();
        Logger.setLocationEnabled(false);
        Logger.addAppender(new AndroidLoggerAppender());

        System.setOut(new PrintStream(new OutputStream() {
            StringBuffer line = new StringBuffer();

            @Override
            public void write(int oneByte) throws IOException {
                if (((char) oneByte) == '\n') {
                    Logger.debug(line.toString());
                    if (line.length() > 0) {
                        line.delete(0, line.length() - 1);
                    }
                } else {
                    line.append((char) oneByte);
                }
            }
        }));
        System.setErr(new PrintStream(new OutputStream() {
            StringBuffer line = new StringBuffer();

            @Override
            public void write(int oneByte) throws IOException {
                if (((char) oneByte) == '\n') {
                    Logger.debug(line.toString());
                    if (line.length() > 0) {
                        line.delete(0, line.length() - 1);
                    }
                } else {
                    line.append((char) oneByte);
                }
            }
        }));
        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) return;
            RosterView rosterView = new RosterView();
            rosterView.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, rosterView, RosterView.TAG).commit();
        }
    }

    public static void resetBar() {
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowCustomEnabled(false);
        General.currentActivity.setTitle(R.string.app_name);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (NOTIFY.equals(intent.getAction())) {
            Chat current = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
            if (current != null)
                openChat(current.getProtocol(), current.getContact(), true);
        }
    }

    private ChatView createChatView(Protocol p, Contact c, FragmentManager fragmentManager, boolean addToBackStack, boolean allowingStateLoss) {
        ChatView chatView = new ChatView();
        chatView.initChat(p, c);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, chatView, ChatView.TAG);
        if (addToBackStack)
            transaction.addToBackStack(null);
        if (allowingStateLoss)
            transaction.commitAllowingStateLoss();
        else
            transaction.commit();
        return chatView;
    }

    public void openChat(Protocol p, Contact c, boolean allowingStateLoss) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        c.activate(p);
        if (findViewById(R.id.fragment_container) != null) {
            Fragment rosterView = getSupportFragmentManager().findFragmentByTag(RosterView.TAG);
            ChatView chatView = (ChatView) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
            if (fragmentManager.getFragments() == null || rosterView == null || chatView == null) {
                createChatView(p, c, fragmentManager, true, allowingStateLoss);
                return;
            }
            if (rosterView.isVisible()) {
                createChatView(p, c, fragmentManager, true, allowingStateLoss);
            } else if (chatView.isVisible()) {
                if (c != null) {
                    chatView.openChat(p, c);
                    chatView.resume(p.getChat(c));
                }
            }
        } else {
            ChatView chatView = (ChatView) fragmentManager.findFragmentById(R.id.chat_fragment);
            if (chatView != null) {
                chatView.pause(chatView.getCurrentChat());
                if (c != null) {
                    chatView.openChat(p, c);
                    chatView.resume(chatView.getCurrentChat());
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        General.currentActivity = this;
        General.maximize();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (Roster.getInstance().getProtocolCount() == 0) {
            StartWindowView newFragment = new StartWindowView();
            if (getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment) != null) {
                getSupportFragmentManager().findFragmentById(R.id.chat_fragment).getView().setVisibility(View.GONE);
                transaction.replace(R.id.roster_fragment, newFragment, StartWindowView.TAG);
            } else {
                transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
            }
            transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();
        } else {
            StartWindowView startWindowView = (StartWindowView) getSupportFragmentManager().findFragmentByTag(StartWindowView.TAG);
            if (startWindowView != null)
                getSupportFragmentManager().popBackStack();
        }
        handleIntent(getIntent());
    }

    @Override
    public void onPause() {
        super.onPause();
        General.minimize();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        SawimFragment chatView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        SawimFragment formView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(FormView.TAG);
        SawimFragment virtualListView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(VirtualListView.TAG);
        if (chatView != null) {
            if (chatView.hasBack())
                super.onBackPressed();
        } else if (virtualListView != null) {
            if (virtualListView.hasBack())
                back();
        } else if (formView != null) {
            if (formView.hasBack())
                back();
        } else moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ChatView view = (ChatView) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        if (view != null) {
            if (ExternalApi.instance.onActivityResult(requestCode, resultCode, data)) {
                super.onActivityResult(requestCode, resultCode, data);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void back() {
        super.onBackPressed();
        if (General.currentActivity.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            General.currentActivity.getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment).getView().setVisibility(View.VISIBLE);
    }

    public void recreateActivity() {
        finish();
        startActivity(new Intent(this, SawimActivity.class));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            back();
        }
        return super.onOptionsItemSelected(item);
    }
}