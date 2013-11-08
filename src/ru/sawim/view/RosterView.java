package ru.sawim.view;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;
import ru.sawim.SawimApplication;
import ru.sawim.activities.AccountsListActivity;
import ru.sawim.widget.IconTabPageIndicator;
import sawim.Options;
import sawim.OptionsForm;
import sawim.forms.ManageContactListForm;
import sawim.forms.SmsForm;
import sawim.modules.DebugLog;
import sawim.modules.MagicEye;
import sawim.modules.Notify;
import protocol.StatusInfo;
import protocol.icq.Icq;
import protocol.jabber.Jabber;
import protocol.mrim.Mrim;
import ru.sawim.General;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.ChatsAdapter;
import ru.sawim.widget.MyListView;
import sawim.chat.Chat;
import sawim.roster.TreeNode;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.models.CustomPagerAdapter;
import ru.sawim.models.RosterAdapter;
import sawim.chat.ChatHistory;
import sawim.roster.Roster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 01.04.13
 * Time: 19:58
 * To change this template use File | Settings | File Templates.
 */
public class RosterView extends Fragment implements ListView.OnItemClickListener, Roster.OnUpdateRoster {

    public static final String TAG = "RosterView";

    private IconTabPageIndicator horizontalScrollView;
    private RosterViewRoot rosterViewLayout;
    private ProgressBar progressBar;
    private ViewPager viewPager;
    private ViewPager.LayoutParams viewPagerLayoutParams;
    private PagerTitleStrip indicator;
    private ArrayList<BaseAdapter> adaptersPages = new ArrayList<BaseAdapter>();
    private CustomPagerAdapter pagerAdapter;
    private Roster roster;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentActivity activity = getActivity();
        roster = Roster.getInstance();
        adaptersPages.clear();
        MyListView allListView = new MyListView(activity);
        MyListView onlineListView = new MyListView(activity);
        MyListView activeListView = new MyListView(activity);
        RosterAdapter allRosterAdapter = new RosterAdapter(getActivity(), roster, Roster.ALL_CONTACTS);
        RosterAdapter onlineRosterAdapter = new RosterAdapter(getActivity(), roster, Roster.ONLINE_CONTACTS);
        ChatsAdapter activeRosterAdapter = new ChatsAdapter(getActivity());
        adaptersPages.add(allRosterAdapter);
        adaptersPages.add(onlineRosterAdapter);
        adaptersPages.add(activeRosterAdapter);
        allListView.setAdapter(allRosterAdapter);
        onlineListView.setAdapter(onlineRosterAdapter);
        activeListView.setAdapter(activeRosterAdapter);
        allListView.setTag(activity.getResources().getString(R.string.all_contacts));
        onlineListView.setTag(activity.getResources().getString(R.string.online_contacts));
        activeListView.setTag(activity.getResources().getString(R.string.active_contacts));
        activity.registerForContextMenu(allListView);
        activity.registerForContextMenu(onlineListView);
        activity.registerForContextMenu(activeListView);
        allListView.setOnCreateContextMenuListener(this);
        onlineListView.setOnCreateContextMenuListener(this);
        activeListView.setOnCreateContextMenuListener(this);
        allListView.setOnItemClickListener(this);
        onlineListView.setOnItemClickListener(this);
        activeListView.setOnItemClickListener(this);

        List<View> pages = new ArrayList<View>();
        pages.add(allListView);
        pages.add(onlineListView);
        pages.add(activeListView);
        pagerAdapter = new CustomPagerAdapter(pages);

        setHasOptionsMenu(true);
        addProtocolsTabs();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        General.currentActivity = (FragmentActivity) activity;
        horizontalScrollView = new IconTabPageIndicator(activity);

        progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);

        viewPager = new ViewPager(activity);
        viewPager.setAnimationCacheEnabled(false);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(final int pos) {
                roster.setCurrPage(pos);
                updateRoster();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        indicator = new PagerTitleStrip(activity);
        viewPagerLayoutParams = new ViewPager.LayoutParams();
        viewPagerLayoutParams.height = ViewPager.LayoutParams.WRAP_CONTENT;
        viewPagerLayoutParams.width = ViewPager.LayoutParams.FILL_PARENT;
        viewPagerLayoutParams.gravity = Gravity.TOP;
        viewPager.addView(indicator, viewPagerLayoutParams);
    }

    private class RosterViewRoot extends LinearLayout {

        public RosterViewRoot(Context context) {
            super(context);

            LinearLayout.LayoutParams ProgressBarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ProgressBarLP.setMargins(30, 0, 30, 1);
            addViewInLayout(progressBar, 0, ProgressBarLP, true);
            addViewInLayout(viewPager, 1, viewPagerLayoutParams, true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rosterViewLayout == null)
            rosterViewLayout = new RosterViewRoot(getActivity());
        else
            ((ViewGroup)rosterViewLayout.getParent()).removeView(rosterViewLayout);
        rosterViewLayout.setOrientation(LinearLayout.VERTICAL);
        rosterViewLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        if (!Scheme.isSystemBackground())
            rosterViewLayout.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));

        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(roster.getCurrPage());
        return rosterViewLayout;
    }

    @Override
    public void putIntoQueue(final Group g) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (adaptersPages != null && adaptersPages.size() > 0) {
                    if (roster.getCurrPage() != Roster.ACTIVE_CONTACTS) {
                        ((RosterAdapter)adaptersPages.get(roster.getCurrPage())).putIntoQueue(g);
                    }
                }
            }
        });
    }

    @Override
    public void updateRoster() {
        roster.updateOptions();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (adaptersPages != null && adaptersPages.size() > 0) {
                    if (roster.getCurrPage() == Roster.ACTIVE_CONTACTS) {
                        ((ChatsAdapter)adaptersPages.get(roster.getCurrPage())).refreshList();
                    } else {
                        ((RosterAdapter)adaptersPages.get(roster.getCurrPage())).buildFlatItems();
                    }
                }
            }
        });
    }

    @Override
    public void updateProgressBar() {
        final Protocol p = roster.getCurrentProtocol();
        if (p == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (p.isConnecting()) {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    byte percent = p.getConnectingProgress();
                    Rect bounds = progressBar.getProgressDrawable().getBounds();
                    progressBar.getProgressDrawable().setBounds(bounds);
                    progressBar.setProgress(percent);
                } else {
                    progressBar.setVisibility(ProgressBar.GONE);
                }
            }
        });
    }

    public void update() {
        updateRoster();
        updateBarProtocols();
        updateProgressBar();
    }

    private void initBar() {
        boolean isShowTabs = roster.getProtocolCount() > 1;
        SawimActivity.actionBar.setDisplayShowTitleEnabled(!isShowTabs);
        SawimActivity.actionBar.setDisplayShowHomeEnabled(!isShowTabs);
        SawimActivity.actionBar.setDisplayUseLogoEnabled(!isShowTabs);
        SawimActivity.actionBar.setDisplayHomeAsUpEnabled(!isShowTabs);
        SawimActivity.actionBar.setDisplayShowCustomEnabled(isShowTabs);
        getActivity().setTitle(R.string.app_name);
        SawimActivity.actionBar.setCustomView(horizontalScrollView);
    }

    public void addProtocolsTabs() {
        final int protocolCount = roster.getProtocolCount();
        horizontalScrollView.removeAllTabs();
        if (protocolCount > 1) {
            horizontalScrollView.setOnTabSelectedListener(new IconTabPageIndicator.OnTabSelectedListener() {
                @Override
                public void onTabSelected(int position) {
                    roster.setCurrentItemProtocol(position);
                    update();
                    Toast toast = Toast.makeText(getActivity(), roster.getProtocol(position).getUserId(), Toast.LENGTH_LONG);
                    toast.setDuration(100);
                    toast.show();
                    getActivity().supportInvalidateOptionsMenu();
                }
            });
            for (int i = 0; i < protocolCount; ++i) {
                Protocol protocol = roster.getProtocol(i);
                Drawable icon = protocol.getCurrentStatusIcon().getImage();
                Icon messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
                if (null != messageIcon)
                    icon = messageIcon.getImage();
                horizontalScrollView.addTab(i, icon);
            }
            horizontalScrollView.setCurrentItem(roster.getCurrentItemProtocol());
        }
    }

    @Override
    public void updateBarProtocols() {
        final int protocolCount = roster.getProtocolCount();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (protocolCount > 1) {
                    for (int i = 0; i < protocolCount; ++i) {
                        Protocol protocol = roster.getProtocol(i);
                        Drawable icon = protocol.getCurrentStatusIcon().getImage();
                        Icon messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
                        if (null != messageIcon)
                            icon = messageIcon.getImage();
                        horizontalScrollView.updateTabIcon(i, icon);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        General.currentActivity = getActivity();
        initBar();
        if (roster.getProtocolCount() == 0) return;
        roster.setCurrentContact(null);
        roster.setOnUpdateRoster(this);
        if (General.returnFromAcc) {
            General.returnFromAcc = false;
            if (roster.getCurrentProtocol().getContactItems().size() == 0 && !roster.getCurrentProtocol().isConnecting())
                Toast.makeText(getActivity(), R.string.press_menu_for_connect, Toast.LENGTH_LONG).show();
            addProtocolsTabs();
        }
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        roster.setOnUpdateRoster(null);
    }

    public void openChat(Protocol p, Contact c, boolean allowingStateLoss) {
        c.activate(p);
        ChatView chatView = (ChatView) getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment);
        if (chatView == null) {
            chatView = new ChatView();
            chatView.initChat(p, c);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, chatView, ChatView.TAG);
            transaction.addToBackStack(null);
            if (allowingStateLoss)
                transaction.commitAllowingStateLoss();
            else
                transaction.commit();
        } else {
            chatView.pause(chatView.getCurrentChat());
            if (c != null) {
                chatView.openChat(p, c);
                chatView.resume(chatView.getCurrentChat());
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (roster.getCurrPage() == Roster.ACTIVE_CONTACTS) {
            ChatsAdapter chatsAdapter = ((ChatsAdapter)adaptersPages.get(roster.getCurrPage()));
            Object o = chatsAdapter.getItem(position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                openChat(chat.getProtocol(), chat.getContact(), false);
                if (getActivity().getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    update();
            }
        } else {
            TreeNode item = ((RosterAdapter)adaptersPages.get(roster.getCurrPage())).getItem(position);
            if (item.isContact()) {
                openChat(roster.getCurrentProtocol(), ((Contact) item), false);
                if (getActivity().getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    update();
            } else if (item.isGroup()) {
                Group group = (Group) item;
                group.setExpandFlag(!group.isExpanded());
                updateRoster();
            }
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (roster.getCurrPage() == Roster.ACTIVE_CONTACTS) {
            ChatsAdapter chatsAdapter = ((ChatsAdapter)adaptersPages.get(roster.getCurrPage()));
            Object o = chatsAdapter.getItem(contextMenuInfo.position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                new ContactMenu(chat.getProtocol(), chat.getContact()).getContextMenu(menu);
                return;
            }
        } else {
            TreeNode node = ((RosterAdapter)((ListView) v).getAdapter()).getItem(contextMenuInfo.position);
            Protocol p = roster.getCurrentProtocol();
            if (node.isContact()) {
                new ContactMenu(p, (Contact) node).getContextMenu(menu);
                return;
            }
            if (node.isGroup()) {
                if (p.isConnected()) {
                    new ManageContactListForm(p, (Group) node).showMenu(getActivity());
                    return;
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (menuInfo == null)
            menuInfo = contextMenuInfo;
        if (roster.getCurrPage() == Roster.ACTIVE_CONTACTS) {
            ChatsAdapter chatsAdapter = ((ChatsAdapter)adaptersPages.get(roster.getCurrPage()));
            Object o = chatsAdapter.getItem(menuInfo.position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                contactMenuItemSelected(chat.getContact(), item);
            }
        } else {
            TreeNode node = ((RosterAdapter)adaptersPages.get(roster.getCurrPage())).getItem(menuInfo.position);
            if (node == null) return false;
            if (node.isContact()) {
                contactMenuItemSelected((Contact) node, item);
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    private void contactMenuItemSelected(final Contact c, final android.view.MenuItem item) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Protocol p = roster.getCurrentProtocol();
                if (roster.getCurrPage() == Roster.ACTIVE_CONTACTS)
                    p = c.getProtocol();
                new ContactMenu(p, c).doAction(item.getItemId());
            }
        });
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
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
                    if (((Jabber)p).hasS2S()) {
                        menu.add(Menu.NONE, MENU_DISCO, Menu.NONE, R.string.service_discovery);
                    }
                }
                menu.add(Menu.NONE, MENU_GROUPS, Menu.NONE, R.string.manage_contact_list);
                if (p instanceof Icq) {
                    menu.add(Menu.NONE, MENU_MYSELF, Menu.NONE, R.string.myself);
                } else {
                    SubMenu moreMenu = menu.addSubMenu(Menu.NONE, MENU_MORE, Menu.NONE, R.string.more);
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
        menu.add(Menu.NONE, MENU_MAGIC_EYE, Menu.NONE, R.string.magic_eye);
        SubMenu optionsMenu = menu.addSubMenu(Menu.NONE, MENU_OPTIONS, Menu.NONE, R.string.options);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ACCOUNT, Menu.NONE, R.string.options_account);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_INTERFACE, Menu.NONE, R.string.options_interface);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_SIGNALING, Menu.NONE, R.string.options_signaling);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ANTISPAM, Menu.NONE, R.string.antispam);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ABSENCE, Menu.NONE, R.string.absence);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ANSWERER, Menu.NONE, R.string.answerer);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ABOUT, Menu.NONE, R.string.about_program);

        menu.add(Menu.NONE, MENU_DEBUG_LOG, Menu.NONE, R.string.debug);
        menu.add(Menu.NONE, MENU_QUIT, Menu.NONE, R.string.quit);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Protocol p = Roster.getInstance().getCurrentProtocol();
        switch (item.getItemId()) {
            case MENU_CONNECT:
                p.setStatus((p.isConnected() || p.isConnecting())
                        ? StatusInfo.STATUS_OFFLINE : StatusInfo.STATUS_ONLINE, "");
                Thread.yield();
                getActivity().supportInvalidateOptionsMenu();
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
                ((Jabber)p).getServiceDiscovery().showIt();
                break;
            case MENU_NOTES:
                ((Jabber)p).getMirandaNotes().showIt();
                break;
            case MENU_GROUPS:
                new ManageContactListForm(p).showMenu(General.currentActivity);
                break;
            case MENU_MYSELF:
                p.showUserInfo(p.createTempContact(p.getUserId(), p.getNick()));
                break;
            case MENU_MICROBLOG:
                ((Mrim)p).getMicroBlog().activate();
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