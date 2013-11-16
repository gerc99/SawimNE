package ru.sawim.view;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.*;
import android.widget.*;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.models.ChatsAdapter;
import ru.sawim.models.CustomPagerAdapter;
import ru.sawim.models.RosterAdapter;
import ru.sawim.widget.IconTabPageIndicator;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.roster.RosterViewRoot;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.forms.ManageContactListForm;
import sawim.roster.Roster;
import sawim.roster.TreeNode;

import java.util.ArrayList;
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

    private static boolean isTablet;
    private LinearLayout barLinearLayout;
    private IconTabPageIndicator horizontalScrollView;
    private RosterViewRoot rosterViewLayout;
    private ProgressBar progressBar;
    private ViewPager viewPager;
    private ArrayList<BaseAdapter> adaptersPages;
    private CustomPagerAdapter pagerAdapter;
    private Roster roster;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentActivity activity = getActivity();
        roster = Roster.getInstance();
        adaptersPages = new ArrayList<BaseAdapter>();
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
        General.currentActivity = (ActionBarActivity) activity;
        isTablet = activity.findViewById(R.id.fragment_container) == null;
        barLinearLayout = new LinearLayout(activity);
        horizontalScrollView = new IconTabPageIndicator(activity);

        progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        LinearLayout.LayoutParams ProgressBarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ProgressBarLP.setMargins(30, 0, 30, 1);
        progressBar.setLayoutParams(ProgressBarLP);

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
        PagerTitleStrip indicator = new PagerTitleStrip(activity);
        ViewPager.LayoutParams viewPagerLayoutParams = new ViewPager.LayoutParams();
        viewPagerLayoutParams.height = ViewPager.LayoutParams.WRAP_CONTENT;
        viewPagerLayoutParams.width = ViewPager.LayoutParams.FILL_PARENT;
        viewPagerLayoutParams.gravity = Gravity.TOP;
        viewPager.setLayoutParams(viewPagerLayoutParams);
        viewPager.addView(indicator, viewPagerLayoutParams);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        isTablet = getActivity().findViewById(R.id.fragment_container) == null;
        if (rosterViewLayout == null)
            rosterViewLayout = new RosterViewRoot(getActivity(), progressBar, viewPager);
        else
            ((ViewGroup) rosterViewLayout.getParent()).removeView(rosterViewLayout);

        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(roster.getCurrPage());
        return rosterViewLayout;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        barLinearLayout = null;
        horizontalScrollView = null;
        rosterViewLayout = null;
        progressBar = null;
        viewPager = null;
        adaptersPages = null;
        pagerAdapter = null;
        roster = null;
        contextMenuInfo = null;
    }

    @Override
    public void putIntoQueue(final Group g) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (adaptersPages != null && adaptersPages.size() > 0) {
                    if (roster.getCurrPage() != Roster.ACTIVE_CONTACTS) {
                        ((RosterAdapter) adaptersPages.get(roster.getCurrPage())).putIntoQueue(g);
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
                        ((ChatsAdapter) adaptersPages.get(roster.getCurrPage())).refreshList();
                    } else {
                        ((RosterAdapter) adaptersPages.get(roster.getCurrPage())).buildFlatItems();
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
        General.currentActivity.getSupportActionBar().setDisplayShowTitleEnabled(!isShowTabs);
        General.currentActivity.getSupportActionBar().setDisplayShowHomeEnabled(!isShowTabs);
        General.currentActivity.getSupportActionBar().setDisplayUseLogoEnabled(!isShowTabs);
        General.currentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(!isShowTabs);
        General.currentActivity.getSupportActionBar().setDisplayShowCustomEnabled(isShowTabs);
        getActivity().setTitle(R.string.app_name);
        if (isTablet) {
            ChatView chatView = (ChatView) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            LinearLayout.LayoutParams horizontalScrollViewLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            horizontalScrollViewLP.weight = 2;
            horizontalScrollView.setLayoutParams(horizontalScrollViewLP);
            barLinearLayout.removeAllViews();
            barLinearLayout.setLayoutParams(layoutParams);
            barLinearLayout.addView(horizontalScrollView);
            chatView.removeTitleBar();
            barLinearLayout.addView(chatView.getTitleBar());
            General.currentActivity.getSupportActionBar().setCustomView(barLinearLayout);
        } else
            General.currentActivity.getSupportActionBar().setCustomView(horizontalScrollView);
    }

    public void addProtocolsTabs() {
        final int protocolCount = roster.getProtocolCount();
        horizontalScrollView.removeAllTabs();
        horizontalScrollView.setOnTabSelectedListener(null);
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
        General.currentActivity = (ActionBarActivity) getActivity();
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
        if (!isTablet)
            getActivity().supportInvalidateOptionsMenu();
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
            ChatsAdapter chatsAdapter = ((ChatsAdapter) adaptersPages.get(roster.getCurrPage()));
            Object o = chatsAdapter.getItem(position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                openChat(chat.getProtocol(), chat.getContact(), false);
                if (getActivity().getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    update();
            }
        } else {
            TreeNode item = ((RosterAdapter) adaptersPages.get(roster.getCurrPage())).getItem(position);
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
            ChatsAdapter chatsAdapter = ((ChatsAdapter) adaptersPages.get(roster.getCurrPage()));
            Object o = chatsAdapter.getItem(contextMenuInfo.position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                new ContactMenu(chat.getProtocol(), chat.getContact()).getContextMenu(menu);
                return;
            }
        } else {
            TreeNode node = ((RosterAdapter) ((ListView) v).getAdapter()).getItem(contextMenuInfo.position);
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
            ChatsAdapter chatsAdapter = ((ChatsAdapter) adaptersPages.get(roster.getCurrPage()));
            Object o = chatsAdapter.getItem(menuInfo.position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                contactMenuItemSelected(chat.getContact(), item);
            }
        } else {
            TreeNode node = ((RosterAdapter) adaptersPages.get(roster.getCurrPage())).getItem(menuInfo.position);
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