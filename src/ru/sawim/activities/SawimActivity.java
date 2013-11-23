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
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import org.microemu.log.Logger;
import org.microemu.util.AndroidLoggerAppender;
import protocol.Contact;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.icq.Icq;
import protocol.jabber.Jabber;
import protocol.mrim.Mrim;
import ru.sawim.*;
import ru.sawim.view.*;
import sawim.ExternalApi;
import sawim.Options;
import sawim.OptionsForm;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.forms.ManageContactListForm;
import sawim.forms.SmsForm;
import sawim.modules.DebugLog;
import sawim.modules.MagicEye;
import sawim.modules.Notify;
import sawim.roster.Roster;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class SawimActivity extends ActionBarActivity {

    public static final String LOG_TAG = "SawimActivity";
    public static String NOTIFY = "ru.sawim.notify";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        General.actionBar = getSupportActionBar();
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
        General.actionBar.setDisplayHomeAsUpEnabled(true);
        General.actionBar.setDisplayShowTitleEnabled(true);
        General.actionBar.setDisplayUseLogoEnabled(true);
        General.actionBar.setDisplayShowHomeEnabled(true);
        General.actionBar.setDisplayShowCustomEnabled(false);
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
        ChatView chatView = (ChatView) fragmentManager.findFragmentById(R.id.chat_fragment);
        c.activate(p);
        if (chatView == null) {
            Fragment rosterView = getSupportFragmentManager().findFragmentByTag(RosterView.TAG);
            chatView = (ChatView) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
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
            if (c != null) {
                chatView.openChat(p, c);
                chatView.resume(chatView.getCurrentChat());
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
            if (General.currentActivity.getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment) != null)
                General.currentActivity.setContentView(R.layout.intercalation_layout);
            StartWindowView newFragment = new StartWindowView();
            transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
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
    protected void onDestroy() {
        super.onDestroy();
        General.currentActivity = null;
    }

    @Override
    public void onBackPressed() {
        SawimFragment chatView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        SawimFragment formView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(FormView.TAG);
        SawimFragment virtualListView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(VirtualListView.TAG);
        if (chatView != null && chatView.isVisible()) {
            if (chatView.hasBack())
                super.onBackPressed();
        } else if (virtualListView != null) {
            if (virtualListView.hasBack())
                back();
        } else if (formView != null) {
            if (formView.hasBack())
                back();
        } else moveTaskToBack(true);
        if (getSupportFragmentManager().findFragmentById(R.id.roster_fragment) != null)
            ((RosterView) getSupportFragmentManager().findFragmentById(R.id.roster_fragment)).resume();
    }

    private void back() {
        if (General.currentActivity.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            recreateActivity();
        else
            super.onBackPressed();
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

    public void recreateActivity() {
        finish();
        startActivity(new Intent(this, SawimActivity.class));
    }

    int oldOrientation;
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (oldOrientation != newConfig.orientation) {
            oldOrientation = newConfig.orientation;
            if (General.getInstance().getConfigurationChanged() != null) {
                General.getInstance().getConfigurationChanged().onConfigurationChanged();
            }
        }
    }

    private static final int MENU_CONNECT = 0;
    private static final int MENU_STATUS = 1;
    private static final int MENU_XSTATUS = 2;
    private static final int MENU_PRIVATE_STATUS = 3;
    private static final int MENU_SEND_SMS = 4;
    private static final int MENU_SOUND = 5;
    private static final int MENU_OPTIONS = 6;
    private static final int MENU_QUIT = 14; //OptionsForm
    private static final int MENU_MORE = 15;
    private static final int MENU_DISCO = 16;
    private static final int MENU_NOTES = 17;
    private static final int MENU_GROUPS = 18;
    private static final int MENU_MYSELF = 19;
    private static final int MENU_MICROBLOG = 20;//ManageContactListForm
    private static final int MENU_MAGIC_EYE = 21;
    private static final int MENU_DEBUG_LOG = 22;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ChatView chatView = (ChatView) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        ChatView tabletChatView = (ChatView) getSupportFragmentManager().findFragmentById(R.id.chat_fragment);
        VirtualListView virtualListView = (VirtualListView) getSupportFragmentManager().findFragmentByTag(VirtualListView.TAG);
        FormView formView = (FormView) getSupportFragmentManager().findFragmentByTag(FormView.TAG);
        menu.clear();
        if (formView != null) {
            return false;
        } else if (chatView != null) {
            chatView.onPrepareOptionsMenu_(menu);
            return true;
        } else if (tabletChatView != null && tabletChatView.isOpenMenu) {
            tabletChatView.onPrepareOptionsMenu_(menu);
            tabletChatView.isOpenMenu = false;
            return true;
        } else if (virtualListView != null) {
            virtualListView.onPrepareOptionsMenu_(menu);
            return true;
        }
        Protocol p = Roster.getInstance().getCurrentProtocol();
        if (p != null) {
            menu.add(Menu.NONE, MENU_CONNECT, Menu.NONE, R.string.connect);
            menu.findItem(MENU_CONNECT).setTitle((p.isConnected() || p.isConnecting()) ? R.string.disconnect : R.string.connect);
            menu.add(Menu.NONE, MENU_STATUS, Menu.NONE, R.string.status);
            menu.add(Menu.NONE, MENU_XSTATUS, Menu.NONE, R.string.xstatus);
            if ((p instanceof Icq) || (p instanceof Mrim))
                menu.add(Menu.NONE, MENU_PRIVATE_STATUS, Menu.NONE, R.string.private_status);

            int count = Roster.getInstance().getProtocolCount();
            for (int i = 0; i < count; ++i) {
                Protocol pr = Roster.getInstance().getProtocol(i);
                if ((pr instanceof Mrim) && pr.isConnected()) {
                    menu.add(Menu.NONE, MENU_SEND_SMS, Menu.NONE, R.string.send_sms);
                }
            }
            if (p.isConnected()) {
                if (p instanceof Jabber) {
                    if (((Jabber) p).hasS2S()) {
                        menu.add(Menu.NONE, MENU_DISCO, Menu.NONE, R.string.service_discovery);
                    }
                }
                menu.add(Menu.NONE, MENU_GROUPS, Menu.NONE, R.string.manage_contact_list);
                if (p instanceof Icq) {
                    menu.add(Menu.NONE, MENU_MYSELF, Menu.NONE, R.string.myself);
                } else {
                    //SubMenu moreMenu = menu.addSubMenu(Menu.NONE, MENU_MORE, Menu.NONE, R.string.more);
                    Menu moreMenu = menu;
                    if (p instanceof Jabber) {
                        moreMenu.add(Menu.NONE, MENU_NOTES, Menu.NONE, R.string.notes);
                    }
                    if (p.hasVCardEditor())
                        moreMenu.add(Menu.NONE, MENU_MYSELF, Menu.NONE, R.string.myself);
                    if (p instanceof Mrim)
                        moreMenu.add(Menu.NONE, MENU_MICROBLOG, Menu.NONE, R.string.microblog);
                }
            }
        }
        menu.add(Menu.NONE, MENU_SOUND, Menu.NONE, Options.getBoolean(Options.OPTION_SILENT_MODE)
                ? R.string.sound_on : R.string.sound_off);
        //menu.add(Menu.NONE, MENU_MAGIC_EYE, Menu.NONE, R.string.magic_eye);
        SubMenu optionsMenu = menu.addSubMenu(Menu.NONE, MENU_OPTIONS, Menu.NONE, R.string.options);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ACCOUNT, Menu.NONE, R.string.options_account);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_INTERFACE, Menu.NONE, R.string.options_interface);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_SIGNALING, Menu.NONE, R.string.options_signaling);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ANTISPAM, Menu.NONE, R.string.antispam);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ABSENCE, Menu.NONE, R.string.absence);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ANSWERER, Menu.NONE, R.string.answerer);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ABOUT, Menu.NONE, R.string.about_program);

        //menu.add(Menu.NONE, MENU_DEBUG_LOG, Menu.NONE, R.string.debug);
        menu.add(Menu.NONE, MENU_QUIT, Menu.NONE, R.string.quit);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) back();

        ChatView chatView = (ChatView) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        ChatView tabletChatView = (ChatView) getSupportFragmentManager().findFragmentById(R.id.chat_fragment);
        VirtualListView virtualListView = (VirtualListView) getSupportFragmentManager().findFragmentByTag(VirtualListView.TAG);
        if (virtualListView != null) {
            return virtualListView.onOptionsItemSelected_(item);
        } else if (tabletChatView != null && tabletChatView.isOpenMenu) {
            tabletChatView.onOptionsItemSelected_(item);
            tabletChatView.isOpenMenu = false;
            return true;
        } else if (chatView != null) {
            return chatView.onOptionsItemSelected_(item);
        }
        Protocol p = Roster.getInstance().getCurrentProtocol();
        switch (item.getItemId()) {
            case MENU_CONNECT:
                p.setStatus((p.isConnected() || p.isConnecting())
                        ? StatusInfo.STATUS_OFFLINE : StatusInfo.STATUS_ONLINE, "");
                Thread.yield();
                supportInvalidateOptionsMenu();
                break;
            case MENU_STATUS:
                new StatusesView(p, StatusesView.ADAPTER_STATUS).show(General.currentActivity.getSupportFragmentManager(), "change-status");
                break;
            case MENU_XSTATUS:
                new XStatusesView().show(General.currentActivity.getSupportFragmentManager(), "change-xstatus");
                break;
            case MENU_PRIVATE_STATUS:
                new StatusesView(p, StatusesView.ADAPTER_PRIVATESTATUS).show(General.currentActivity.getSupportFragmentManager(), "change-private-status");
                break;
            case MENU_SEND_SMS:
                new SmsForm(null, null).show();
                break;
            case MENU_SOUND:
                Notify.getSound().changeSoundMode(false);
                break;
            case MENU_MAGIC_EYE:
                MagicEye.instance.activate();
                break;
            case MENU_DISCO:
                ((Jabber) p).getServiceDiscovery().showIt();
                break;
            case MENU_NOTES:
                ((Jabber) p).getMirandaNotes().showIt();
                break;
            case MENU_GROUPS:
                new ManageContactListForm(p).showMenu(General.currentActivity);
                break;
            case MENU_MYSELF:
                p.showUserInfo(p.createTempContact(p.getUserId(), p.getNick()));
                break;
            case MENU_MICROBLOG:
                ((Mrim) p).getMicroBlog().activate();
                break;

            case OptionsForm.OPTIONS_ACCOUNT:
                startActivity(new Intent(General.currentActivity, AccountsListActivity.class));
                break;
            case OptionsForm.OPTIONS_INTERFACE:
                new OptionsForm().select(item.getTitle(), OptionsForm.OPTIONS_INTERFACE);
                break;
            case OptionsForm.OPTIONS_SIGNALING:
                new OptionsForm().select(item.getTitle(), OptionsForm.OPTIONS_SIGNALING);
                break;
            case OptionsForm.OPTIONS_ANTISPAM:
                new OptionsForm().select(item.getTitle(), OptionsForm.OPTIONS_ANTISPAM);
                break;
            case OptionsForm.OPTIONS_ABSENCE:
                new OptionsForm().select(item.getTitle(), OptionsForm.OPTIONS_ABSENCE);
                break;
            case OptionsForm.OPTIONS_ANSWERER:
                new OptionsForm().select(item.getTitle(), OptionsForm.OPTIONS_ANSWERER);
                break;
            case OptionsForm.OPTIONS_ABOUT:
                new AboutProgramView().show(General.currentActivity.getSupportFragmentManager(), AboutProgramView.TAG);
                break;

            case MENU_DEBUG_LOG:
                DebugLog.instance.activate();
                break;
            case MENU_QUIT:
                General.getInstance().quit();
                SawimApplication.getInstance().quit();
                General.currentActivity.finish();
                System.exit(0);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}