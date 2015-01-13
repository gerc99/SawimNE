package ru.sawim.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;
import ru.sawim.view.*;
import ru.sawim.view.menu.MyMenu;
import ru.sawim.view.menu.MyMenuItem;

import java.util.ArrayList;
import java.util.List;

public class SawimActivity extends BaseActivity {

    public static final String LOG_TAG = SawimActivity.class.getSimpleName();
    public static final String NOTIFY = "ru.sawim.notify";
    public static final String NOTIFY_REPLY = "ru.sawim.notify.reply";
    public static final String NOTIFY_CAPTCHA = "ru.sawim.notify.captcha";
    public static final String NOTIFY_UPLOAD = "ru.sawim.notify.upload";
    public static final String SHOW_FRAGMENT = "ru.sawim.show_fragment";

    private static List<Fragment> backgroundFragmentsStack = new ArrayList<>();
    private boolean isOpenNewChat = false;
    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);

        setContentView(SawimApplication.isManyPane() ? R.layout.main_twopane : R.layout.main);
        if (savedInstanceState == null && !SawimApplication.isManyPane()) {
            RosterView rosterView = new RosterView();
            rosterView.setMode(RosterView.MODE_DEFAULT);
            rosterView.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, rosterView, RosterView.TAG).commit();
        }
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(SHOW_FRAGMENT)) {
                    showLastFragmentFromBackStack();
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter(SHOW_FRAGMENT));
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
        if (NOTIFY_CAPTCHA.equals(getIntent().getAction())) {
            FormView.showWindows(this, getIntent().getStringExtra(NOTIFY_CAPTCHA));
        }
        if (NOTIFY_UPLOAD.equals(getIntent().getAction())) {
            RosterHelper.getInstance().getFileTransfer(getIntent().getIntExtra(NOTIFY_UPLOAD, -1)).showDialog(this);
        }
        setIntent(null);
    }

    public boolean openChat(Protocol p, Contact c, boolean allowingStateLoss) {
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
                if (allowingStateLoss)
                    transaction.commitAllowingStateLoss();
                else
                    transaction.commit();
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

    private void showLastFragmentFromBackStack() {
        if (backgroundFragmentsStack.size() > 0) {
            Fragment fragment = backgroundFragmentsStack.get(backgroundFragmentsStack.size() - 1);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, fragment.getTag());
            backgroundFragmentsStack.remove(backgroundFragmentsStack.size() - 1);
        }
    }

    public static void addFragmentToStack(Fragment fragment) {
        if (SawimApplication.isPaused()) {
            backgroundFragmentsStack.add(fragment);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        SawimApplication.maximize();
        FragmentManager fragmentManager = getSupportFragmentManager();
        StartWindowView startWindowView = (StartWindowView) fragmentManager.findFragmentByTag(StartWindowView.TAG);
        if (RosterHelper.getInstance().getProtocolCount() == 0) {
            if (Options.getAccountCount() == 0) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                if (SawimApplication.isManyPane()) {
                    setContentView(R.layout.main);
                    if (startWindowView == null) {
                        StartWindowView newFragment = new StartWindowView();
                        transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
                        transaction.commit();
                        supportInvalidateOptionsMenu();
                    }
                }
            } else {
                startActivity(new Intent(this, AccountsListActivity.class));
            }
        } else {
            if (startWindowView != null)
                fragmentManager.popBackStack();
        }
        handleIntent();
        if (!isOpenNewChat && SawimApplication.isManyPane()) openChat(null, null, true);

        showLastFragmentFromBackStack();
    }

    @Override
    public void onPause() {
        super.onPause();
        SawimApplication.minimize();
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SawimFragment chatView = (SawimFragment) fragmentManager.findFragmentByTag(ChatView.TAG);
        SawimFragment formView = (SawimFragment) fragmentManager.findFragmentByTag(FormView.TAG);
        SawimFragment virtualListView = (SawimFragment) fragmentManager.findFragmentByTag(VirtualListView.TAG);
        if (chatView != null && chatView.isVisible()) {
            if (chatView.hasBack())
                super.onBackPressed();
        } else if (virtualListView != null) {
            if (virtualListView.hasBack())
                back();
        } else if (formView != null) {
            if (formView.hasBack())
                back();
        } else super.onBackPressed();
        //if (SawimApplication.isManyPane())
        //    rosterView.resume();
    }

    private void back() {
        if (SawimApplication.isManyPane())
            recreateActivity();
        else
            super.onBackPressed();
    }

    public void recreateActivity() {
        finish();
        startActivity(new Intent(this, SawimActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
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
            menu.add(Menu.NONE, MENU_QUIT, Menu.NONE, R.string.quit);
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
