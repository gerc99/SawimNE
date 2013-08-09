package ru.sawim.view;

import DrawControls.icons.Icon;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import ru.sawim.SawimApplication;
import sawim.roster.TreeNode;
import android.content.Intent;
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
import ru.sawim.activities.AccountsListActivity;
import ru.sawim.activities.ChatActivity;
import ru.sawim.models.CustomPagerAdapter;
import ru.sawim.models.RosterAdapter;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.roster.Roster;
import sawim.comm.Util;
import sawim.forms.ManageContactListForm;

import java.util.ArrayList;
import java.util.HashMap;
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

    private static final String TAG = "RosterView";
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
    private final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    private HashMap<Integer, ImageButton> protocolIconHash = new HashMap<Integer, ImageButton>();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FragmentActivity currentActivity = getActivity();
        roster = Roster.getInstance();
        if (roster == null) {
            startActivity(new Intent(currentActivity, AccountsListActivity.class));
            return;
        } else {
            int protocolCount = roster.getProtocolCount();
            if (protocolCount == 0) {
                startActivity(new Intent(currentActivity, AccountsListActivity.class));
                return;
            } else if (protocolCount == 1 && roster.getCurrentProtocol().getContactItems().size() == 0 && !roster.getCurrentProtocol().isConnecting()) {
                Toast.makeText(getActivity(), R.string.press_menu_for_connect, Toast.LENGTH_LONG).show();
            }
        }
        roster.setOnUpdateRoster(this);
        roster.updateOptions();

        adaptersPages.clear();
        ListView allListView = new ListView(currentActivity);
        ListView onlineListView = new ListView(currentActivity);
        ListView activeListView = new ListView(currentActivity);
    //    LayoutInflater inf = LayoutInflater.from(currentActivity);
        allRosterAdapter = new RosterAdapter(getActivity(), roster, items, RosterAdapter.ALL_CONTACTS);
        onlineRosterAdapter = new RosterAdapter(getActivity(), roster, items, RosterAdapter.ONLINE_CONTACTS);
        activeRosterAdapter = new RosterAdapter(getActivity(), roster, items, RosterAdapter.ACTIVE_CONTACTS);
        adaptersPages.add(allRosterAdapter);
        adaptersPages.add(onlineRosterAdapter);
        adaptersPages.add(activeRosterAdapter);
        allListView.setCacheColorHint(0x00000000);
        onlineListView.setCacheColorHint(0x00000000);
        activeListView.setCacheColorHint(0x00000000);
        allListView.setScrollingCacheEnabled(false);
        onlineListView.setScrollingCacheEnabled(false);
        activeListView.setScrollingCacheEnabled(false);
        allListView.setAnimationCacheEnabled(false);
        onlineListView.setAnimationCacheEnabled(false);
        activeListView.setAnimationCacheEnabled(false);
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.roster_view, null);
        LinearLayout rosterViewLayout = (LinearLayout) v.findViewById(R.id.roster_view);
        rosterViewLayout.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        progressBar = (ProgressBar) v.findViewById(R.id.myprogressbar);
        horizontalScrollView = (HorizontalScrollView) v.findViewById(R.id.horizontalScrollView);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {Scheme.getColor(Scheme.THEME_CAP_BACKGROUND),Scheme.getColor(Scheme.THEME_BACKGROUND)});
        gd.setCornerRadius(0f);
        horizontalScrollView.setBackgroundDrawable(gd);
        //horizontalScrollView.setBackgroundColor(Scheme.getColorWithAlpha(Scheme.THEME_CAP_BACKGROUND));
        protocolBarLayout = (LinearLayout) horizontalScrollView.findViewById(R.id.protocol_bar);
        viewPager = (ViewPager) v.findViewById(R.id.view_pager);
        PagerTitleStrip indicator = (PagerTitleStrip) viewPager.findViewById(R.id.pager_title_strip);
        indicator.setTextColor(Scheme.getColor(Scheme.THEME_GROUP));
        indicator.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        return v;
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
                //            int prevIndex = getCurrItem();
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
        if (null != node) {
            currentNode = node;
        }
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
        final int protCount = roster.getProtocolCount();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (protCount > 1) {
                    protocolBarLayout.removeAllViews();
                    for (int i = 0; i < protCount; ++i) {
                        Protocol protocol = roster.getProtocol(i);
                        ImageButton imageBarButtons = protocolIconHash.get(i);
                        if (imageBarButtons == null) {
                            imageBarButtons = new ImageButton(getActivity());
                            imageBarButtons.setOnClickListener(RosterView.this);
                            imageBarButtons.setOnLongClickListener(RosterView.this);
                            protocolIconHash.put(i, imageBarButtons);
                            imageBarButtons.setId(i);
                            lp.gravity = Gravity.CENTER;
                            imageBarButtons.setLayoutParams(lp);
                        }
                        imageBarButtons.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_CAP_BACKGROUND), PorterDuff.Mode.MULTIPLY);
                        if (i == roster.getCurrentItemProtocol())
                            imageBarButtons.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_BACKGROUND), PorterDuff.Mode.SCREEN);
                        imageBarButtons.setImageDrawable(protocol.getCurrentStatusIcon().getImage());
                        Icon messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
                        if (null != messageIcon)
                            imageBarButtons.setImageDrawable(messageIcon.getImage());
                        protocolBarLayout.addView(imageBarButtons, i);
                    }
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
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (roster == null) return;
        roster.setOnUpdateRoster(null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        ChatView viewer = (ChatView) getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment);
        TreeNode item = adaptersPages.get(viewPager.getCurrentItem()).getItem(position);
        if (item.isContact()) {
            Protocol p = roster.getCurrProtocol();
            Contact c = ((Contact) item);
            if (viewPager.getCurrentItem() == RosterAdapter.ACTIVE_CONTACTS)
                p = c.getProtocol();
            c.activate(p);
            if (!isInLayout()) return;
            if (viewer == null || !viewer.isInLayout()) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("protocol_id", p.getUserId());
                intent.putExtra("contact_id", c.getUserId());
                getActivity().startActivity(intent);
            } else {
                Chat chat = viewer.getCurrentChat();
                viewer.pause(chat);
                viewer.resetSpinner();
                if (c != null) {
                    viewer.openChat(p, c);
                    viewer.resume(viewer.getCurrentChat());
                }
            }
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
        }
        if (node.isGroup()) {
            if (p.isConnected()) {
                new ManageContactListForm(p, (Group) node).showMenu(getActivity());
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
                    new ContactMenu(p, c).doAction(getActivity(), item.getItemId());
                }
            });
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        Toast toast = Toast.makeText(getActivity(), roster.getProtocol(view.getId()).getUserId(), Toast.LENGTH_LONG);
        toast.setDuration(50);
        toast.show();
        roster.setCurrentItemProtocol(view.getId());
        update();
    }

    @Override
    public boolean onLongClick(View view) {
        new StatusesView(roster.getProtocol(view.getId()), StatusesView.ADAPTER_STATUS).show(getActivity().getSupportFragmentManager(), "change-status");
        return false;
    }
}