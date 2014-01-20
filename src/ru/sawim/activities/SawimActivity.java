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
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import org.microemu.log.Logger;
import org.microemu.util.AndroidLoggerAppender;
import protocol.Contact;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.icq.Icq;
import protocol.mrim.Mrim;
import protocol.xmpp.Xmpp;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.view.*;
import ru.sawim.view.preference.PreferenceFormView;
import sawim.ExternalApi;
import sawim.Options;
import sawim.OptionsForm;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.forms.ManageContactListForm;
import sawim.forms.SmsForm;
import sawim.modules.DebugLog;
import sawim.modules.Notify;
import sawim.roster.RosterHelper;

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
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        ExternalApi.instance.setActivity(this);
        General.actionBar = getSupportActionBar();
        General.currentActivity = this;
        setContentView(General.isManyPane() ? R.layout.main_twopane : R.layout.main);

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
        if (savedInstanceState == null && !General.isManyPane()) {
            RosterView rosterView = new RosterView();
            rosterView.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, rosterView, RosterView.TAG).commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);
    }

    public static void resetBar() {
        General.actionBar.setDisplayHomeAsUpEnabled(false);
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
        boolean isOpenNewChat = false;
        if (NOTIFY.equals(intent.getAction())) {
            Chat current = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
            if (current != null)
                isOpenNewChat = openChat(current.getProtocol(), current.getContact(), true);
        }
        if (!isOpenNewChat && General.isManyPane()) openChat(null, null, true);
    }

    public boolean openChat(Protocol p, Contact c, boolean allowingStateLoss) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ChatView chatView = (ChatView) fragmentManager.findFragmentById(R.id.chat_fragment);
        if (chatView == null) {
            Fragment rosterView = getSupportFragmentManager().findFragmentByTag(RosterView.TAG);
            chatView = (ChatView) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
            if (fragmentManager.getFragments() == null || rosterView == null || chatView == null || rosterView.isVisible()) {
                if (p == null || c == null) return false;
                chatView = new ChatView();
                c.activate(p);
                chatView.initChat(p, c);
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, chatView, ChatView.TAG);
                transaction.addToBackStack(null);
                if (allowingStateLoss)
                    transaction.commitAllowingStateLoss();
                else
                    transaction.commit();
                return true;
            }
            if (chatView.isVisible() && chatView.isLastPosition()) {
                if (c != null) {
                    c.activate(p);
                    chatView.openChat(p, c);
                    chatView.resume(p.getChat(c));
                    return true;
                }
            }
        } else {
            Protocol protocol = null;
            Contact contact = null;
            if (p != null && c != null && chatView.isLastPosition()) {
                protocol = p;
                contact = c;
            } else if (chatView.getCurrentChat() != null) {
                protocol = chatView.getCurrentChat().getProtocol();
                contact = chatView.getCurrentChat().getContact();
            }
            if (protocol != null && contact != null) {
                chatView.openChat(protocol, contact);
                chatView.resume(chatView.getCurrentChat());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (General.currentActivity == null)
            General.currentActivity = this;
        General.maximize();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (RosterHelper.getInstance().getProtocolCount() == 0) {
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
    public void onBackPressed() {
        SawimFragment chatView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        SawimFragment formView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(FormView.TAG);
        SawimFragment preferenceFormView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(PreferenceFormView.TAG);
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
        } else if (preferenceFormView != null) {
            if (preferenceFormView.hasBack())
                back();
        } else super.onBackPressed();
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
        if (General.isTablet()) recreateActivity();
        if (oldOrientation != newConfig.orientation) {
            oldOrientation = newConfig.orientation;
            if (General.getInstance().getConfigurationChanged() != null)
                General.getInstance().getConfigurationChanged().onConfigurationChanged();
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
    private static final int MENU_DISCO = 16;
    private static final int MENU_NOTES = 17;
    private static final int MENU_GROUPS = 18;
    private static final int MENU_MYSELF = 19;
    private static final int MENU_MICROBLOG = 20;//ManageContactListForm
    private static final int MENU_DEBUG_LOG = 22;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ChatView chatView = (ChatView) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        ChatView tabletChatView = (ChatView) getSupportFragmentManager().findFragmentById(R.id.chat_fragment);
        VirtualListView virtualListView = (VirtualListView) getSupportFragmentManager().findFragmentByTag(VirtualListView.TAG);
        menu.clear();
        if (chatView != null) {
            chatView.onPrepareOptionsMenu_(menu);
            return true;
        } else if (tabletChatView != null && tabletChatView.isOpenMenu()) {
            tabletChatView.onPrepareOptionsMenu_(menu);
            tabletChatView.setOpenMenu(false);
            return true;
        } else if (virtualListView != null) {
            virtualListView.onPrepareOptionsMenu_(menu);
            return true;
        }
        Protocol p = RosterHelper.getInstance().getCurrentProtocol();
        if (p != null) {
            menu.add(Menu.NONE, MENU_CONNECT, Menu.NONE, R.string.connect);
            menu.findItem(MENU_CONNECT).setTitle((p.isConnected() || p.isConnecting()) ? R.string.disconnect : R.string.connect);
            menu.add(Menu.NONE, MENU_STATUS, Menu.NONE, R.string.status);
            if (p.getXStatusInfo() != null)
                menu.add(Menu.NONE, MENU_XSTATUS, Menu.NONE, R.string.xstatus);
            if ((p instanceof Icq) || (p instanceof Mrim))
                menu.add(Menu.NONE, MENU_PRIVATE_STATUS, Menu.NONE, R.string.private_status);

            int count = RosterHelper.getInstance().getProtocolCount();
            for (int i = 0; i < count; ++i) {
                Protocol pr = RosterHelper.getInstance().getProtocol(i);
                if ((pr instanceof Mrim) && pr.isConnected()) {
                    menu.add(Menu.NONE, MENU_SEND_SMS, Menu.NONE, R.string.send_sms);
                }
            }
            if (p.isConnected()) {
                if (p instanceof Xmpp) {
                    if (((Xmpp) p).hasS2S()) {
                        menu.add(Menu.NONE, MENU_DISCO, Menu.NONE, R.string.service_discovery);
                    }
                }
                menu.add(Menu.NONE, MENU_GROUPS, Menu.NONE, R.string.manage_contact_list);
                if (p instanceof Icq) {
                    menu.add(Menu.NONE, MENU_MYSELF, Menu.NONE, R.string.myself);
                } else {
                    if (p instanceof Xmpp) {
                        menu.add(Menu.NONE, MENU_NOTES, Menu.NONE, R.string.notes);
                    }
                    if (p.hasVCardEditor())
                        menu.add(Menu.NONE, MENU_MYSELF, Menu.NONE, R.string.myself);
                    if (p instanceof Mrim)
                        menu.add(Menu.NONE, MENU_MICROBLOG, Menu.NONE, R.string.microblog);
                }
            }
        }
        menu.add(Menu.NONE, MENU_SOUND, Menu.NONE, Options.getBoolean(Options.OPTION_SILENT_MODE)
                ? R.string.sound_on : R.string.sound_off);
        SubMenu optionsMenu = menu.addSubMenu(Menu.NONE, MENU_OPTIONS, Menu.NONE, R.string.options);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ACCOUNT, Menu.NONE, R.string.options_account);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_INTERFACE, Menu.NONE, R.string.options_interface);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_SIGNALING, Menu.NONE, R.string.options_signaling);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ANTISPAM, Menu.NONE, R.string.antispam);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ANSWERER, Menu.NONE, R.string.answerer);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ABOUT, Menu.NONE, R.string.about_program);

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
            virtualListView.onOptionsItemSelected_(item);
            return true;
        } else if (tabletChatView != null && tabletChatView.isOpenMenu()) {
            tabletChatView.onOptionsItemSelected_(item);
            tabletChatView.setOpenMenu(false);
            return true;
        } else if (chatView != null) {
            chatView.onOptionsItemSelected_(item);
            return true;
        }
        Protocol p = RosterHelper.getInstance().getCurrentProtocol();
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
            case MENU_DISCO:
                ((Xmpp) p).getServiceDiscovery().showIt();
                break;
            case MENU_NOTES:
                ((Xmpp) p).getMirandaNotes().showIt();
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
                General.getInstance().quit(false);
                SawimApplication.getInstance().quit();
                General.currentActivity.finish();
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}