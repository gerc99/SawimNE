package ru.sawim.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import protocol.Contact;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimNotification;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.gcm.Preferences;
import ru.sawim.gcm.RegistrationIntentService;
import ru.sawim.listener.OnAccountsLoaded;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;
import ru.sawim.view.*;
import ru.sawim.view.menu.MyMenu;
import ru.sawim.view.menu.MyMenuItem;

public class SawimActivity extends BaseActivity implements OnAccountsLoaded {

    public static final String LOG_TAG = SawimActivity.class.getSimpleName();

    public static final String NOTIFY = "ru.sawim.notify";
    public static final String NOTIFY_REPLY = "ru.sawim.notify.reply";
    public static final String NOTIFY_CAPTCHA = "ru.sawim.notify.captcha";
    public static final String NOTIFY_UPLOAD = "ru.sawim.notify.upload";
    public static final String ACTION_SHOW_LOGIN_WINDOW = "ru.sawim.show_login_window";
    public static final String EXTRA_MESSAGE_FROM_ID = "ru.sawim.notify.message_from_id";

    private boolean isOpenNewChat = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        BaseActivity.setTheme(this);
        super.onCreate(savedInstanceState);

        setContentView(SawimApplication.isManyPane() ? R.layout.main_twopane : R.layout.main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null && !SawimApplication.isManyPane()) {
            RosterView rosterView = new RosterView();
            rosterView.setMode(RosterView.MODE_DEFAULT);
            rosterView.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, rosterView, RosterView.TAG).commit();
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getString(Preferences.TOKEN, "").isEmpty()) {
            if (SawimApplication.checkPlayServices) {
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (getRosterView() != null &&
                (getRosterView().getMode() == RosterView.MODE_SHARE_TEXT
                        || getRosterView().getMode() == RosterView.MODE_SHARE)) return;
        setIntent(intent);
    }

    private void handleIntent() {
        if (getIntent() == null || getIntent().getAction() == null) return;
        if (getIntent().getAction().startsWith(Intent.ACTION_SEND)) {
            FragmentManager fm = getSupportFragmentManager();
            for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                fm.popBackStack();
            }
            RosterView rosterView = getRosterView();
            if (rosterView != null)
                rosterView.setMode(getIntent().getType().equals("text/plain") ? RosterView.MODE_SHARE_TEXT : RosterView.MODE_SHARE);
            return;
        }
        if (ACTION_SHOW_LOGIN_WINDOW.equals(getIntent().getAction())) {
            StartWindowView newFragment = new StartWindowView();
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
            transaction.commit();
        }
        String userId = getIntent().getStringExtra(EXTRA_MESSAGE_FROM_ID);
        if (NOTIFY.equals(getIntent().getAction())) {
            Chat current = ChatHistory.instance.getChat(userId);
            if (current != null)
                isOpenNewChat = openChat(current.getProtocol(), current.getContact());
        }
        if (NOTIFY_REPLY.equals(getIntent().getAction())) {
            Chat current = ChatHistory.instance.getChat(userId);
            if (current != null)
                isOpenNewChat = openChat(current.getProtocol(), current.getContact());
        }
        if (NOTIFY_CAPTCHA.equals(getIntent().getAction())) {
            FormView.showWindows(this, getIntent().getStringExtra(NOTIFY_CAPTCHA));
        }
        if (NOTIFY_UPLOAD.equals(getIntent().getAction())) {
            RosterHelper.getInstance().getFileTransfer(getIntent().getIntExtra(NOTIFY_UPLOAD, -1)).showDialog(this);
        }
        setIntent(null);
    }

    public boolean openChat(Protocol p, Contact c) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ChatView chatView = (ChatView) fragmentManager.findFragmentById(R.id.chat_fragment);
        if (chatView == null) {
            Fragment rosterView = fragmentManager.findFragmentByTag(RosterView.TAG);
            chatView = (ChatView) fragmentManager.findFragmentByTag(ChatView.TAG);
            if (fragmentManager.getFragments() == null || rosterView == null || chatView == null || rosterView.isVisible()) {
                if (p == null || c == null) return false;
                chatView = ChatView.newInstance(p.getUserId(), c.getUserId());
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, chatView, ChatView.TAG);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
                return true;
            }
            if (RosterHelper.getInstance().getCurrentContact() != c
                    && chatView.isScrollEnd()) {
                if (c != null) {
                    chatView.openChat(p, c);
                    return true;
                }
            }
        } else {
            Protocol protocol = null;
            Contact contact = null;
            Chat oldChat = ChatHistory.instance.getChat(RosterHelper.getInstance().getCurrentContact());
            if (p != null && c != null) {
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
    public void onAccountsLoaded() {
        Log.e(LOG_TAG, "onAccountsLoaded");
        RosterHelper.getInstance().autoConnect();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StartWindowView startWindowView = (StartWindowView) getSupportFragmentManager().findFragmentByTag(StartWindowView.TAG);
                if (RosterHelper.getInstance().getProtocolCount() == 0) {
                    if (Options.getAccountCount() == 0) {
                        if (SawimApplication.isManyPane()) {
                            setContentView(R.layout.main);
                        }
                        if (startWindowView == null) {
                            StartWindowView newFragment = new StartWindowView();
                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
                            if (!SawimApplication.isManyPane()) {
                                transaction.addToBackStack(null);
                            }
                            transaction.commit();
                            supportInvalidateOptionsMenu();
                        }
                    } else {
                        startActivity(new Intent(getBaseContext(), AccountsListActivity.class));
                    }
                } else {
                    //Toast.makeText(SawimApplication.getContext(), R.string.press_menu_for_connect, Toast.LENGTH_LONG).show();
                    if (startWindowView != null)
                        getSupportFragmentManager().popBackStack();
                }
            }
        });
        RosterHelper.getInstance().setOnAccountsLoaded(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        SawimApplication.maximize();
        handleIntent();
        if (RosterHelper.getInstance().isAccountsLoaded) {
            onAccountsLoaded();
        } else {
            RosterHelper.getInstance().setOnAccountsLoaded(this);
        }
        if (!isOpenNewChat && SawimApplication.isManyPane()) openChat(null, null);
        if (SawimApplication.checkPlayServices) {
            RosterHelper.getInstance().autoConnect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SawimApplication.minimize();
        RosterHelper.getInstance().setOnAccountsLoaded(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (SawimApplication.checkPlayServices) {
            SawimApplication.getInstance().quit(false);
            SawimApplication.getInstance().stopService();
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SawimFragment rosterView = getRosterView();
        SawimFragment chatView = getChatView();
        SawimFragment formView = (SawimFragment) fragmentManager.findFragmentByTag(FormView.TAG);
        SawimFragment virtualListView = (SawimFragment) fragmentManager.findFragmentByTag(VirtualListView.TAG);
        if (chatView != null && chatView.isVisible()) {
            if (chatView.hasBack())
                super.onBackPressed();
        } else if (rosterView != null && rosterView.isVisible()) {
            if (rosterView.hasBack())
                back();
        } else if (virtualListView != null) {
            if (virtualListView.hasBack())
                back();
        } else if (formView != null) {
            if (formView.hasBack())
                back();
        } else super.onBackPressed();
    }

    private void back() {
        if (SawimApplication.isManyPane())
            recreateActivity();
        else
            super.onBackPressed();
    }

    public void closeActivity() {
        finish();
        Intent intent = new Intent(this, SawimActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (SawimApplication.isTablet()) recreateActivity();
        if (getChatView() != null && getChatView().getDrawerToggle() != null)
            getChatView().getDrawerToggle().onConfigurationChanged(newConfig);
    }

    private RosterView getRosterView() {
        RosterView rosterView = (RosterView) getSupportFragmentManager().findFragmentByTag(RosterView.TAG);
        if (rosterView == null)
            rosterView = (RosterView) getSupportFragmentManager().findFragmentById(R.id.roster_fragment);
        return rosterView;
    }

    private ChatView getChatView() {
        ChatView chatView = (ChatView) getSupportFragmentManager().findFragmentByTag(ChatView.TAG);
        if (chatView == null) chatView = (ChatView) getSupportFragmentManager().findFragmentById(R.id.chat_fragment);
        return chatView;
    }

    private static final int MENU_OPTIONS = 7;
    private static final int MENU_QUIT = 14;
    private static final int MENU_DEBUG_LOG = 22;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        ChatView chatView = getChatView();
        RosterView rosterView = getRosterView();
        SearchConferenceFragment searchConferenceFragment = (SearchConferenceFragment) getSupportFragmentManager().findFragmentByTag(SearchConferenceFragment.TAG);
        SearchContactFragment searchContactFragment = (SearchContactFragment) getSupportFragmentManager().findFragmentByTag(SearchContactFragment.TAG);
        StartWindowView startWindowView = (StartWindowView) getSupportFragmentManager().findFragmentByTag(StartWindowView.TAG);
        VirtualListView virtualListView = (VirtualListView) getSupportFragmentManager().findFragmentByTag(VirtualListView.TAG);
        if (virtualListView != null && virtualListView.isAdded()) {
            virtualListView.onPrepareOptionsMenu_(menu);
            return true;
        } else if (chatView != null && chatView.isAdded() && !SawimApplication.isManyPane()) {
            chatView.onPrepareOptionsMenu_(menu);
            return true;
        } else if ((rosterView != null && rosterView.isAdded())
                || (startWindowView != null && startWindowView.isAdded())) {
            Protocol p = RosterHelper.getInstance().getProtocol(0);
            if (RosterHelper.getInstance().getProtocolCount() == 1 && p != null) {
                MyMenu myMenu = RosterHelper.getInstance().getMenu(p);
                for (int i = 0; i < myMenu.getCount(); ++i) {
                    MyMenuItem myMenuItem = myMenu.getItem(i);
                    menu.add(Menu.NONE, myMenuItem.idItem, Menu.NONE, myMenuItem.nameItem);
                }
            }
            menu.add(Menu.NONE, MENU_OPTIONS, Menu.NONE, R.string.options);
            if (!SawimApplication.checkPlayServices) {
                menu.add(Menu.NONE, MENU_QUIT, Menu.NONE, R.string.quit);
            }
        } else if (searchContactFragment != null && searchContactFragment.isAdded()) {
            searchContactFragment.onPrepareOptionsMenu_(menu);
        } else if (searchConferenceFragment != null && searchConferenceFragment.isAdded()) {
            searchConferenceFragment.onPrepareOptionsMenu_(menu);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ChatView chatView = getChatView();
        VirtualListView virtualListView = (VirtualListView) getSupportFragmentManager().findFragmentByTag(VirtualListView.TAG);
        if (!SawimApplication.isManyPane()
                && chatView != null && chatView.getDrawerToggle() != null
                && chatView.getDrawerToggle().onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == android.R.id.home) back();

        if (virtualListView != null) {
            virtualListView.onOptionsItemSelected_(item);
            return true;
        } else if (!SawimApplication.isManyPane() && chatView != null) {
            chatView.onOptionsItemSelected_(item);
            return true;
        }
        if (RosterHelper.getInstance().protocolMenuItemSelected(this, RosterHelper.getInstance().getProtocol(0), item.getItemId()))
            return true;
        switch (item.getItemId()) {
            case MENU_OPTIONS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    startActivity(new Intent(this, MainPreferenceActivityNew.class));
                } else {
                    startActivity(new Intent(this, MainPreferenceActivity.class));
                }
                break;
            case MENU_DEBUG_LOG:
                DebugLog.instance.activate(this);
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
