package ru.sawim.view;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import ru.sawim.General;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.ChatsAdapter;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;
import sawim.chat.Chat;
import sawim.modules.DebugLog;
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
import sawim.forms.ManageContactListForm;

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
        addProtocolsTabs();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.e(TAG, "onAttach");
        General.currentActivity = (FragmentActivity) activity;

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

            LinearLayout.LayoutParams horizontalScrollViewLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, Util.dipToPixels(getActivity(), 50));
            horizontalScrollViewLP.gravity = Gravity.TOP;

            LinearLayout.LayoutParams ProgressBarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ProgressBarLP.setMargins(30, 0, 30, 1);
            addViewInLayout(progressBar, 0, ProgressBarLP, true);
            addViewInLayout(viewPager, 1, viewPagerLayoutParams, true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(TAG, "onCreateView");

        if (rosterViewLayout == null)
            rosterViewLayout = new RosterViewRoot(getActivity());
        else
            ((ViewGroup)rosterViewLayout.getParent()).removeView(rosterViewLayout);
        rosterViewLayout.setOrientation(LinearLayout.VERTICAL);
        rosterViewLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        rosterViewLayout.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));

        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(roster.getCurrPage());

        indicator.setTextColor(Scheme.getColor(Scheme.THEME_GROUP));
        indicator.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
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
        SawimActivity.actionBar.setNavigationMode(isShowTabs ? ActionBar.NAVIGATION_MODE_TABS : ActionBar.NAVIGATION_MODE_STANDARD);
        SawimActivity.actionBar.setDisplayShowCustomEnabled(getActivity().findViewById(R.id.fragment_container) == null);
    }

    public void addProtocolsTabs() {
        int protocolCount = roster.getProtocolCount();
        SawimActivity.actionBar.removeAllTabs();
        if (protocolCount > 1) {
            for (int i = 0; i < protocolCount; ++i) {
                ActionBar.Tab tab = SawimActivity.actionBar.newTab();
                tab.setTabListener(new ActionBar.TabListener() {
                    @Override
                    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                        roster.setCurrentItemProtocol(tab.getPosition());
                        update();
                        Toast toast = Toast.makeText(getActivity(), roster.getProtocol(tab.getPosition()).getUserId(), Toast.LENGTH_LONG);
                        toast.setDuration(100);
                        toast.show();
                    }

                    @Override
                    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                    }

                    @Override
                    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                    }
                });
                SawimActivity.actionBar.addTab(tab);
            }
        }
    }
    @Override
    public void updateBarProtocols() {
        final int protocolCount = roster.getProtocolCount();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (protocolCount > 1) {
                    for (int i = 0; i < SawimActivity.actionBar.getTabCount(); ++i) {
                        Protocol protocol = roster.getProtocol(i);
                        if (protocol == null) continue;
                        ActionBar.Tab tab = SawimActivity.actionBar.getTabAt(i);
                        tab.setIcon(protocol.getCurrentStatusIcon().getImage());
                        Icon messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
                        if (null != messageIcon)
                            tab.setIcon(messageIcon.getImage());
                    }
                }
                ChatView chatView = (ChatView) getActivity().getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment);
                if (chatView != null)
                    chatView.updateChatIcon();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        General.currentActivity = getActivity();
        if (roster.getProtocolCount() == 0) return;
        roster.setCurrentContact(null);
        roster.setOnUpdateRoster(this);
        initBar();
        if (General.returnFromAcc)
            addProtocolsTabs();
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
}