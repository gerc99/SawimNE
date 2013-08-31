package ru.sawim.view;

import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import ru.sawim.SawimApplication;
import ru.sawim.activities.SawimActivity;
import sawim.roster.TreeNode;
import android.graphics.PorterDuff;
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
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.roster.Roster;
import sawim.comm.Util;
import sawim.forms.ManageContactListForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 01.04.13
 * Time: 19:58
 * To change this template use File | Settings | File Templates.
 */
public class RosterView extends Fragment implements View.OnClickListener, ListView.OnItemClickListener, Roster.OnUpdateRoster, View.OnLongClickListener {

    public static final String TAG = "RosterView";
    private HorizontalScrollView horizontalScrollView;
    private LinearLayout protocolBarLayout;
    private ProgressBar progressBar;
    private ViewPager viewPager;
    private ArrayList<RosterAdapter> adaptersPages = new ArrayList<RosterAdapter>();
    private List<View> pages = new ArrayList<View>();
    private CustomPagerAdapter pagerAdapter;
    private RosterAdapter allRosterAdapter;
    private RosterAdapter onlineRosterAdapter;
    private RosterAdapter activeRosterAdapter;
    private Roster roster;
    private Vector updateQueue = new Vector();
    private List<TreeNode> items = new ArrayList<TreeNode>();
    private TreeNode currentNode = null;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (roster == null) {
            showStartWindow();
            return;
        } else {
            int protocolCount = roster.getProtocolCount();
            if (protocolCount == 0) {
                showStartWindow();
                return;
            } else if (protocolCount == 1 && roster.getCurrentProtocol().getContactItems().size() == 0 && !roster.getCurrentProtocol().isConnecting()) {
                Toast.makeText(getActivity(), R.string.press_menu_for_connect, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentActivity currentActivity = getActivity();
        roster = Roster.getInstance();

        adaptersPages.clear();
        MyListView allListView = new MyListView(currentActivity);
        MyListView onlineListView = new MyListView(currentActivity);
        MyListView activeListView = new MyListView(currentActivity);
        allRosterAdapter = new RosterAdapter(getActivity(), roster, items, RosterAdapter.ALL_CONTACTS);
        onlineRosterAdapter = new RosterAdapter(getActivity(), roster, items, RosterAdapter.ONLINE_CONTACTS);
        activeRosterAdapter = new RosterAdapter(getActivity(), roster, items, RosterAdapter.ACTIVE_CONTACTS);
        adaptersPages.add(allRosterAdapter);
        adaptersPages.add(onlineRosterAdapter);
        adaptersPages.add(activeRosterAdapter);
        allListView.setAdapter(allRosterAdapter);
        onlineListView.setAdapter(onlineRosterAdapter);
        activeListView.setAdapter(activeRosterAdapter);
        allListView.setTag(currentActivity.getResources().getString(R.string.all_contacts));
        onlineListView.setTag(currentActivity.getResources().getString(R.string.online_contacts));
        activeListView.setTag(currentActivity.getResources().getString(R.string.active_contacts));
        currentActivity.registerForContextMenu(allListView);
        currentActivity.registerForContextMenu(onlineListView);
        currentActivity.registerForContextMenu(activeListView);
        allListView.setOnCreateContextMenuListener(this);
        onlineListView.setOnCreateContextMenuListener(this);
        activeListView.setOnCreateContextMenuListener(this);
        allListView.setOnItemClickListener(this);
        onlineListView.setOnItemClickListener(this);
        activeListView.setOnItemClickListener(this);

        pages.add(allListView);
        pages.add(onlineListView);
        pages.add(activeListView);

        pagerAdapter = new CustomPagerAdapter(pages);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();
        LinearLayout rosterViewLayout = new LinearLayout(context);
        rosterViewLayout.setOrientation(LinearLayout.VERTICAL);
        rosterViewLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        rosterViewLayout.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));

        horizontalScrollView = new HorizontalScrollView(context);
        LinearLayout.LayoutParams horizontalScrollViewLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 70);
        horizontalScrollViewLP.gravity = Gravity.CENTER_VERTICAL;
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {Scheme.getColor(Scheme.THEME_CAP_BACKGROUND),Scheme.getColor(Scheme.THEME_BACKGROUND)});
        gd.setCornerRadius(0f);
        horizontalScrollView.setBackgroundDrawable(gd);
        rosterViewLayout.addView(horizontalScrollView, horizontalScrollViewLP);

        LinearLayout.LayoutParams protocolBarLayoutLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 70);
        protocolBarLayout = new LinearLayout(context);
        protocolBarLayout.setOrientation(LinearLayout.HORIZONTAL);
        protocolBarLayout.setLayoutParams(protocolBarLayoutLP);

        LinearLayout.LayoutParams ProgressBarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ProgressBarLP.setMargins(30, 0, 30, 1);
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setLayoutParams(ProgressBarLP);
        rosterViewLayout.addView(progressBar);

        viewPager = new ViewPager(context);
        PagerTitleStrip indicator = new PagerTitleStrip(context);
        indicator.setTextColor(Scheme.getColor(Scheme.THEME_GROUP));
        indicator.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(roster.getCurrPage());
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
        ViewPager.LayoutParams viewPagerLayoutParams = new ViewPager.LayoutParams();
        viewPagerLayoutParams.height = ViewPager.LayoutParams.WRAP_CONTENT;
        viewPagerLayoutParams.width = ViewPager.LayoutParams.MATCH_PARENT;
        viewPagerLayoutParams.gravity = Gravity.TOP;
        viewPager.addView(indicator, viewPagerLayoutParams);
        rosterViewLayout.addView(viewPager);
        return rosterViewLayout;
    }

    private static void showStartWindow() {
        if (SawimActivity.getInstance().getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            SawimActivity.getInstance().setContentView(R.layout.intercalation_layout);
        StartWindowView newFragment = new StartWindowView();
        FragmentTransaction transaction = SawimActivity.getInstance().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void putIntoQueue(Group g) {
        if (-1 == Util.getIndex(updateQueue, g)) {
            updateQueue.addElement(g);
        }
    }

    /*private void setCurrentItemIndex(final int currentIndex) {
        ((ListView) pages.get(viewPager.getCurrentItem())).setSelection(currentIndex);
    }

    private int getCurrItem() {
         if ((viewPager.getCurrentItem() < pages.size())
          return ((ListView) pages.get(viewPager.getCurrentItem())).getFirstVisiblePosition();
    }

    private TreeNode getCurrentNode() {
        return getSafeNode(getCurrItem());
    }

    public TreeNode getSafeNode(int index) {
        if ((index < items.size()) && (index >= 0))
            return items.get(index);
        return null;
    }*/

    private void setExpandFlag(Group node, boolean value) {
        //setCurrentNode(getCurrentNode());
        node.setExpandFlag(value);
    }

    @Override
    public void updateRoster() {
        SawimApplication.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                roster.updateOptions();
                while (!updateQueue.isEmpty()) {
                    Group group = (Group) updateQueue.firstElement();
                    updateQueue.removeElementAt(0);
                    roster.updateGroup(group);
                }
                //    try {
                TreeNode current = currentNode;
                currentNode = null;
                //int prevIndex = getCurrItem();
                if (null != current) {
                    if ((current.isContact()) && roster.useGroups) {
                        Contact c = (Contact) current;
                        Protocol p = roster.getCurrentProtocol();
                        if (null != p) {
                            Group group = p.getGroupById(c.getGroupId());
                            if (null == group) {
                                group = p.getNotInListGroup();
                            }
                            group.setExpandFlag(true);
                        }
                    }
                } else {
                    //current = getSafeNode(prevIndex);
                }
                items.clear();
                if (adaptersPages != null && adaptersPages.size() > 0)
                    adaptersPages.get(viewPager.getCurrentItem()).buildFlatItems(items);
            /* (null != current) {
                int currentIndex = Util.getIndex(items, current);
                if ((prevIndex != currentIndex) && (-1 != currentIndex)) {
                    setCurrentItemIndex(currentIndex);
                }
            }
            if (items.size() <= getCurrItem()) {
                setCurrentItemIndex(0);
            }*/
                //    } catch (Exception e) {
                //        DebugLog.panic("update ", e);
                //    }
            }
        });
    }

    @Override
    public void setCurrentNode(TreeNode node) {
        if (null != node)
            currentNode = node;
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

    private void update() {
        updateBarProtocols();
        updateProgressBar();
        updateRoster();
    }

    @Override
    public void updateBarProtocols() {
        final int protocolCount = roster.getProtocolCount();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (protocolCount > 1) {
                    horizontalScrollView.removeAllViews();
                    protocolBarLayout.removeAllViews();
                    for (int i = 0; i < protocolCount; ++i) {
                        Protocol protocol = roster.getProtocol(i);
                        ImageButton imageBarButtons = new ImageButton(getActivity());
                        imageBarButtons.setOnClickListener(RosterView.this);
                        imageBarButtons.setOnLongClickListener(RosterView.this);
                        imageBarButtons.setId(i);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                        lp.gravity = Gravity.CENTER;
                        imageBarButtons.setLayoutParams(lp);
                        imageBarButtons.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_CAP_BACKGROUND), PorterDuff.Mode.MULTIPLY);

                        if (i == roster.getCurrentItemProtocol())
                            imageBarButtons.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_BACKGROUND), PorterDuff.Mode.SCREEN);
                        imageBarButtons.setImageDrawable(protocol.getCurrentStatusIcon().getImage());
                        Icon messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
                        if (null != messageIcon)
                            imageBarButtons.setImageDrawable(messageIcon.getImage());
                        protocolBarLayout.addView(imageBarButtons, i);
                    }
                    horizontalScrollView.addView(protocolBarLayout);
                } else {
                    horizontalScrollView.setVisibility(LinearLayout.GONE);
                    protocolBarLayout.setVisibility(LinearLayout.GONE);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (roster == null) return;
        if (roster.getProtocolCount() == 0) return;
        roster.setOnUpdateRoster(this);
        roster.updateOptions();
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (roster == null) return;
        roster.setOnUpdateRoster(null);
    }

    public void openChat(Protocol p, Contact c, boolean allowingStateLoss) {
        c.activate(p);
        ChatView chatView = (ChatView) getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment);
        if (chatView == null) {
            ChatView newFragment = new ChatView();
            Bundle args = new Bundle();
            args.putString(ChatView.PROTOCOL_ID, p.getUserId());
            args.putString(ChatView.CONTACT_ID, c.getUserId());
            newFragment.setArguments(args);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, newFragment, ChatView.TAG);
            transaction.addToBackStack(null);
            if (allowingStateLoss)
                transaction.commitAllowingStateLoss();
            else
                transaction.commit();
        } else {
            Chat chat = chatView.getCurrentChat();
            chatView.pause(chat);
            chatView.resetSpinner();
            if (c != null) {
                chatView.openChat(p, c);
                chatView.resume(chatView.getCurrentChat());
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        TreeNode item = adaptersPages.get(viewPager.getCurrentItem()).getItem(position);
        if (item.isContact()) {
            Protocol p = roster.getCurrProtocol();
            Contact c = ((Contact) item);
            if (viewPager.getCurrentItem() == RosterAdapter.ACTIVE_CONTACTS)
                p = c.getProtocol();
            openChat(p, c, false);
        } else if (item.isGroup()) {
            Group group = (Group) item;
            setExpandFlag(group, !group.isExpanded());
        }
        updateRoster();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        TreeNode node = ((RosterAdapter)((ListView) v).getAdapter()).getItem(contextMenuInfo.position);
        Protocol p = roster.getCurrentProtocol();
        if (viewPager.getCurrentItem() == RosterAdapter.ACTIVE_CONTACTS)
            p = ((Contact) node).getProtocol();
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

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (menuInfo == null)
            menuInfo = contextMenuInfo;
        final TreeNode node = adaptersPages.get(viewPager.getCurrentItem()).getItem(menuInfo.position);
        if (node == null) return false;
        if (node.isContact()) {
            final Contact c = (Contact) node;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Protocol p = roster.getCurrentProtocol();
                    if (viewPager.getCurrentItem() == RosterAdapter.ACTIVE_CONTACTS)
                        p = ((Contact) node).getProtocol();
                    new ContactMenu(p, c).doAction(item.getItemId());
                }
            });
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        roster.setCurrentItemProtocol(view.getId());
        update();
        Toast toast = Toast.makeText(getActivity(), roster.getProtocol(view.getId()).getUserId(), Toast.LENGTH_LONG);
        toast.setDuration(100);
        toast.show();
    }

    @Override
    public boolean onLongClick(View view) {
        new StatusesView(roster.getProtocol(view.getId()), StatusesView.ADAPTER_STATUS).show(getActivity().getSupportFragmentManager(), "change-status");
        return false;
    }
}