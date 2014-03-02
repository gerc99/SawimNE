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
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import protocol.Contact;
import protocol.Protocol;
import protocol.icq.Icq;
import protocol.mrim.Mrim;
import protocol.xmpp.Xmpp;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.view.*;
import ru.sawim.view.preference.MainPreferenceView;
import ru.sawim.widget.SwipeGestureListener;
import sawim.ExternalApi;
import sawim.Options;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.forms.ManageContactListForm;
import sawim.forms.SmsForm;
import sawim.modules.DebugLog;
import sawim.modules.Notify;
import sawim.roster.RosterHelper;

public class SawimActivity extends ActionBarActivity {

    public static final String LOG_TAG = SawimActivity.class.getSimpleName();
    public static final String NOTIFY = "ru.sawim.notify";
    public static final String NOTIFY_REPLY = "ru.sawim.notify.reply";
    private boolean isOpenNewChat = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        ExternalApi.instance.setActivity(this);
        SawimApplication.setCurrentActivity(this);
        setContentView(SawimApplication.isManyPane() ? R.layout.main_twopane : R.layout.main);

        if (savedInstanceState == null && !SawimApplication.isManyPane()) {
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
        SawimApplication.getActionBar().setDisplayHomeAsUpEnabled(false);
        SawimApplication.getActionBar().setDisplayShowTitleEnabled(true);
        SawimApplication.getActionBar().setDisplayUseLogoEnabled(true);
        SawimApplication.getActionBar().setDisplayShowHomeEnabled(true);
        SawimApplication.getActionBar().setDisplayShowCustomEnabled(false);
        SawimApplication.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        if (((SawimActivity)SawimApplication.getCurrentActivity()).getRosterView() != null)
            ((SawimActivity)SawimApplication.getCurrentActivity()).getRosterView().getDrawerToggle().setDrawerIndicatorEnabled(false);
        SawimApplication.getCurrentActivity().setTitle(R.string.app_name);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void handleIntent() {
        if (getIntent() == null) return;
        if (NOTIFY.equals(getIntent().getAction())) {
            Chat current = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
            if (current != null)
                isOpenNewChat = openChat(current.getProtocol(), current.getContact(), true);
        }
        if (NOTIFY_REPLY.equals(getIntent().getAction())) {
            Chat current = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
            if (current != null)
                isOpenNewChat = openChat(current.getProtocol(), current.getContact(), true);
        }
        setIntent(null);
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
            Chat oldChat = ChatHistory.instance.getChatById(ChatView.getLastChat());
            if (p != null && c != null && chatView.isLastPosition()) {
                protocol = p;
                contact = c;
            } else if (oldChat != null) {
                protocol = oldChat.getProtocol();
                contact = oldChat.getContact();
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
        SawimApplication.setCurrentActivity(this);
        if (SawimApplication.getActionBar() == null)
            SawimApplication.setActionBar(getSupportActionBar());
        SawimApplication.maximize();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (RosterHelper.getInstance().getProtocolCount() == 0) {
            if (getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment) != null)
                setContentView(R.layout.intercalation_layout);
            StartWindowView newFragment = new StartWindowView();
            transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
            transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();
        } else {
            StartWindowView startWindowView = (StartWindowView) getSupportFragmentManager().findFragmentByTag(StartWindowView.TAG);
            if (startWindowView != null)
                getSupportFragmentManager().popBackStack();
        }
        handleIntent();
        if (!isOpenNewChat && SawimApplication.isManyPane()) openChat(null, null, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        SawimApplication.minimize();
    }

    @Override
    public void onBackPressed() {
        SawimFragment chatView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        SawimFragment formView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(FormView.TAG);
        SawimFragment preferenceFormView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(MainPreferenceView.TAG);
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
            if (preferenceFormView.hasBack()) {
                if (SawimApplication.isManyPane()) {
                    recreateActivity();
                } else {
                    super.onBackPressed();
                }
            }
        } else moveTaskToBack(true);
        //if (SawimApplication.isManyPane())
        //    ((RosterView) getSupportFragmentManager().findFragmentById(R.id.roster_fragment)).resume();
    }

    private void back() {
        if (SawimApplication.isManyPane())
            recreateActivity();
        else
            super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ChatView view = (ChatView) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        if (view != null) {
            if (ExternalApi.instance.onActivityResult(requestCode, resultCode, data))
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void recreateActivity() {
        finish();
        SawimApplication.setActionBar(null);
        startActivity(new Intent(this, SawimActivity.class));
    }

    int oldOrientation;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (SawimApplication.isTablet()) recreateActivity();
        if (oldOrientation != newConfig.orientation) {
            oldOrientation = newConfig.orientation;
            if (SawimApplication.getInstance().getConfigurationChanged() != null)
                SawimApplication.getInstance().getConfigurationChanged().onConfigurationChanged();
        }

        getRosterView().getDrawerToggle().onConfigurationChanged(newConfig);
    }

    private RosterView getRosterView() {
        RosterView rosterView = (RosterView) getSupportFragmentManager().findFragmentByTag(RosterView.TAG);
        if (rosterView == null) rosterView = (RosterView) getSupportFragmentManager().findFragmentById(R.id.roster_fragment);
        return rosterView;
    }

    private static final int MENU_CONNECT = 0;
    private static final int MENU_STATUS = 1;
    private static final int MENU_XSTATUS = 2;
    private static final int MENU_PRIVATE_STATUS = 3;
    private static final int MENU_SEND_SMS = 5;
    private static final int MENU_SOUND = 6;
    private static final int MENU_OPTIONS = 7;
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
        SawimFragment mainPreferenceView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(MainPreferenceView.TAG);
        SawimFragment preferenceView = (SawimFragment) getSupportFragmentManager().findFragmentByTag(MainPreferenceView.TAG);
        menu.clear();
        Log.e(LOG_TAG, "" + (tabletChatView != null && tabletChatView.isOpenMenu()));
        if (virtualListView != null) {
            virtualListView.onPrepareOptionsMenu_(menu);
            return true;
        } else if (preferenceView != null || mainPreferenceView != null) {
            return false;
        } else if (chatView != null) {
            chatView.onPrepareOptionsMenu_(menu);
            return true;
        } else if (tabletChatView != null && tabletChatView.isOpenMenu()) {
            tabletChatView.setOpenMenu(false);
            supportInvalidateOptionsMenu();
            tabletChatView.onPrepareOptionsMenu_(menu);
            return true;
        }
        Protocol p = RosterHelper.getInstance().getCurrentProtocol();
        if (p != null) {
            menu.add(Menu.NONE, MENU_CONNECT, Menu.NONE, R.string.connect)
                    .setTitle((p.isConnected() || p.isConnecting()) ? R.string.disconnect : R.string.connect);
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
        menu.add(Menu.NONE, MENU_OPTIONS, Menu.NONE, R.string.options);
        menu.add(Menu.NONE, MENU_QUIT, Menu.NONE, R.string.quit);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (getRosterView().getDrawerToggle().onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == android.R.id.home) back();

        ChatView chatView = (ChatView) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        ChatView tabletChatView = (ChatView) getSupportFragmentManager().findFragmentById(R.id.chat_fragment);
        VirtualListView virtualListView = (VirtualListView) getSupportFragmentManager().findFragmentByTag(VirtualListView.TAG);
        if (virtualListView != null) {
            virtualListView.onOptionsItemSelected_(item);
            return true;
        } else if (tabletChatView != null && tabletChatView.isOpenMenu()) {
            tabletChatView.onOptionsItemSelected_(item);
            return true;
        } else if (chatView != null) {
            chatView.onOptionsItemSelected_(item);
            return true;
        }
        Protocol p = RosterHelper.getInstance().getCurrentProtocol();
        switch (item.getItemId()) {
            case MENU_CONNECT:
                SawimApplication.getInstance().setStatus();
                break;
            case MENU_STATUS:
                new StatusesView(p, StatusesView.ADAPTER_STATUS).show(getSupportFragmentManager(), "change-status");
                break;
            case MENU_XSTATUS:
                new XStatusesView().show(getSupportFragmentManager(), "change-xstatus");
                break;
            case MENU_PRIVATE_STATUS:
                new StatusesView(p, StatusesView.ADAPTER_PRIVATESTATUS).show(getSupportFragmentManager(), "change-private-status");
                break;
            case MENU_SEND_SMS:
                new SmsForm(null, null).show();
                break;
            case MENU_SOUND:
                Notify.getSound().changeSoundMode(false);
                break;
            case MENU_OPTIONS:
                MainPreferenceView.show();
                break;
            case MENU_DISCO:
                ((Xmpp) p).getServiceDiscovery().showIt();
                break;
            case MENU_NOTES:
                ((Xmpp) p).getMirandaNotes().showIt();
                break;
            case MENU_GROUPS:
                new ManageContactListForm(p).showMenu(this);
                break;
            case MENU_MYSELF:
                p.showUserInfo(p.createTempContact(p.getUserId(), p.getNick()));
                break;
            case MENU_MICROBLOG:
                ((Mrim) p).getMicroBlog().activate();
                break;
            case MENU_DEBUG_LOG:
                DebugLog.instance.activate();
                break;
            case MENU_QUIT:
                SawimApplication.getInstance().quit(false);
                SawimApplication.getInstance().stopService();
                finish();
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}