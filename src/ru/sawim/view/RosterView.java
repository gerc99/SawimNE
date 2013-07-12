package ru.sawim.view;

import DrawControls.icons.Icon;
import DrawControls.tree.TreeNode;
import DrawControls.tree.VirtualContactList;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.*;
import android.widget.*;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.activities.AccountsListActivity;
import ru.sawim.activities.ChatActivity;
import ru.sawim.models.CustomPagerAdapter;
import ru.sawim.models.RosterAdapter;
import sawim.Options;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.cl.ContactList;
import sawim.comm.Util;
import sawim.forms.ManageContactListForm;
import sawim.modules.DebugLog;

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
public class RosterView extends Fragment implements View.OnClickListener, ListView.OnItemClickListener, VirtualContactList.OnUpdateRoster, View.OnLongClickListener {

    private LinearLayout rosterViewLayout;
    private LinearLayout topLinearLayout;
    private LinearLayout rosterBarLayout;
    private ProgressBar progressBar;
    private ViewPager viewPager;
    private PagerTitleStrip indicator;
    private List<View> pages = new ArrayList<View>();
    private CustomPagerAdapter pagerAdapter;
    private ArrayList<BaseAdapter> adaptersPages = new ArrayList<BaseAdapter>();
    private RosterAdapter allRosterAdapter;
    private RosterAdapter onlineRosterAdapter;
    private RosterAdapter chatsRosterAdapter;
    private VirtualContactList owner;
    private Vector updateQueue = new Vector();
    private List<TreeNode> items = new ArrayList<TreeNode>();
    private TreeNode currentNode = null;
    private ContactList general;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FragmentActivity currentActivity = getActivity();
        general = ContactList.getInstance();
        owner = general.getManager();
        if (owner == null) return;
        owner.setOnUpdateRoster(this);
        if (owner.getProtocolCount() == 0) {
            startActivity(new Intent(currentActivity, AccountsListActivity.class));
            return;
        }
        owner.updateOptions(owner, owner.getCurrProtocol());
        adaptersPages.clear();
        ListView allListView = new ListView(currentActivity);
        ListView onlineListView = new ListView(currentActivity);
        ListView chatsListView = new ListView(currentActivity);

        rosterViewLayout.setBackgroundColor(General.getColor(Scheme.THEME_BACKGROUND));
        indicator.setTextColor(General.getColor(Scheme.THEME_GROUP));
        indicator.setBackgroundColor(General.getColorWithAlpha(Scheme.THEME_BACKGROUND));
        rosterBarLayout.setBackgroundColor(General.getColorWithAlpha(Scheme.THEME_CAP_BACKGROUND));

        LayoutInflater inf = LayoutInflater.from(currentActivity);
        allRosterAdapter = new RosterAdapter(inf, owner, items, RosterAdapter.ALL_CONTACTS);
        onlineRosterAdapter = new RosterAdapter(inf, owner, items, RosterAdapter.ONLINE_CONTACTS);
        chatsRosterAdapter = new RosterAdapter(inf, owner, ChatHistory.instance.chats(), RosterAdapter.OPEN_CHATS);

        adaptersPages.add(allRosterAdapter);
        adaptersPages.add(onlineRosterAdapter);
        adaptersPages.add(chatsRosterAdapter);

        allListView.setCacheColorHint(0x00000000);
        onlineListView.setCacheColorHint(0x00000000);
        chatsListView.setCacheColorHint(0x00000000);

        allListView.setAdapter(allRosterAdapter);
        onlineListView.setAdapter(onlineRosterAdapter);
        chatsListView.setAdapter(chatsRosterAdapter);

        allListView.setTag(currentActivity.getResources().getString(R.string.all_contacts));
        onlineListView.setTag(currentActivity.getResources().getString(R.string.online_contacts));
        chatsListView.setTag(currentActivity.getResources().getString(R.string.chats));

        pages.add(allListView);
        pages.add(onlineListView);
        pages.add(chatsListView);

        pagerAdapter = new CustomPagerAdapter(pages);
        viewPager.setAdapter(pagerAdapter);

        currentActivity.registerForContextMenu(allListView);
        currentActivity.registerForContextMenu(onlineListView);
        currentActivity.registerForContextMenu(chatsListView);

        allListView.setOnCreateContextMenuListener(this);
        onlineListView.setOnCreateContextMenuListener(this);
        chatsListView.setOnCreateContextMenuListener(this);

        allListView.setOnItemClickListener(this);
        onlineListView.setOnItemClickListener(this);
        chatsListView.setOnItemClickListener(this);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(final int pos) {
                owner.setCurrPage(pos);
                updateRoster();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void putIntoQueue(Group g) {
        if (-1 == Util.getIndex(updateQueue, g)) {
            updateQueue.addElement(g);
        }
    }

    private void setCurrentItemIndex(final int currentIndex) {
        ((ListView) pages.get(viewPager.getCurrentItem())).setSelection(currentIndex);
    }

    private int getCurrItem() {
        return ((ListView) pages.get(viewPager.getCurrentItem())).getFirstVisiblePosition();
    }

    private TreeNode getCurrentNode() {
        return getSafeNode(getCurrItem());
    }

    @Override
    public void setCurrentNode(TreeNode node) {
        if (null != node) {
            currentNode = node;
        }
    }

    public TreeNode getSafeNode(int index) {
        if ((index < items.size()) && (index >= 0)) {
            return items.get(index);
        }
        return null;
    }

    private void setExpandFlag(Group node, boolean value) {
        setCurrentNode(getCurrentNode());
        node.setExpandFlag(value);
    }

    private void rebuildRoster(int pos) {
        owner.getProtocol(owner.getCurrProtocol()).sort();
        while (!updateQueue.isEmpty()) {
            Group group = (Group) updateQueue.firstElement();
            updateQueue.removeElementAt(0);
            owner.updateGroup(group);
        }
        try {
            owner.updateOptions(owner, owner.getCurrProtocol());

            TreeNode current = currentNode;
            currentNode = null;
            int prevIndex = getCurrItem();
            if (null != current) {
                if ((current instanceof Contact) && Options.getBoolean(Options.OPTION_USER_GROUPS)) {
                    Contact c = (Contact) current;
                    Protocol p = owner.getCurrentProtocol();
                    if (null != p) {
                        Group group = p.getGroupById(c.getGroupId());
                        if (null == group) {
                            group = p.getNotInListGroup();
                        }
                        group.setExpandFlag(true);
                    }
                }
            } else {
                current = getSafeNode(prevIndex);
            }
            items.clear();
            owner.buildFlatItems(owner.getCurrProtocol(), items);
            updatePage(pos);
            if (null != current) {
                int currentIndex = Util.getIndex(items, current);
                if ((prevIndex != currentIndex) && (-1 != currentIndex)) {
                    setCurrentItemIndex(currentIndex);
                }
            }
            if (items.size() <= getCurrItem()) {
                setCurrentItemIndex(0);
            }
        } catch (Exception e) {
            DebugLog.panic("update ", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (owner == null) return;
        if (owner.getProtocolCount() == 0) return;
        owner.setOnUpdateRoster(this);
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (owner == null) return;
        owner.setOnUpdateRoster(null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        ChatView viewer = (ChatView) getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment);
        TreeNode item = ((RosterAdapter)adaptersPages.get(viewPager.getCurrentItem())).getItem(position);
        if (item.isContact()) {
            Protocol p;
            Contact c = ((Contact) item);
            if (viewPager.getCurrentItem() == RosterAdapter.OPEN_CHATS)
                p = c.getProtocol();
            else
                p = general.getCurrProtocol();
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
                viewer.destroy(chat);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.roster_view, null);
        rosterViewLayout = (LinearLayout) v.findViewById(R.id.roster_view);
        rosterBarLayout = (LinearLayout) v.findViewById(R.id.roster_bar);
        progressBar = (ProgressBar) v.findViewById(R.id.myprogressbar);
        HorizontalScrollView horizontalScrollView = (HorizontalScrollView) v.findViewById(R.id.horizontalScrollView);
        topLinearLayout = (LinearLayout) horizontalScrollView.findViewById(R.id.topLinearLayout);
        viewPager = (ViewPager) v.findViewById(R.id.view_pager);
        indicator = (PagerTitleStrip) viewPager.findViewById(R.id.pagerTitleStrip);
        return v;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        TreeNode node = ((RosterAdapter)((ListView) v).getAdapter()).getItem(contextMenuInfo.position);
        Protocol p = general.getCurrProtocol();
        if (node.isContact()) {
            new ContactMenu(((Contact) node).getProtocol(), (Contact) node).getContextMenu(menu);
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
        Object node = adaptersPages.get(viewPager.getCurrentItem()).getItem(menuInfo.position);
        if (node instanceof Contact) {
            final Contact c = (Contact) node;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new ContactMenu(c.getProtocol(), c).doAction(getActivity(), item.getItemId());
                }
            });
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        owner.setCurrProtocol(view.getId());
        update();
    }

    @Override
    public boolean onLongClick(View view) {
        new StatusesView(owner.getProtocol(view.getId()), StatusesView.ADAPTER_STATUS).show(getActivity().getSupportFragmentManager(), "change-status");
        return false;
    }

    private void updateProgressBar() {
        Protocol p = general.getCurrProtocol();
        if (p != null)
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

    private void update() {
        updateBarProtocols();
        updateRoster();
    }

    @Override
    public void updateBarProtocols() {
        final int protCount = owner.getProtocolCount();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateProgressBar();

                if (protCount > 1) {
                    topLinearLayout.removeAllViews();
                    for (int j = 0; j < protCount; ++j) {
                        Protocol protocol = owner.getProtocol(j);
                        ImageButton imageBarButtons = new ImageButton(getActivity());
                        if (j == owner.getCurrProtocol())
                            imageBarButtons.setBackgroundColor(General.getColorWithAlpha(Scheme.THEME_BACKGROUND));
                        imageBarButtons.setImageBitmap(protocol.getCurrentStatusIcon().getImage());
                        imageBarButtons.setId(j);
                        imageBarButtons.setOnClickListener(RosterView.this);
                        imageBarButtons.setOnLongClickListener(RosterView.this);
                        Icon messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
                        if (null != messageIcon)
                            imageBarButtons.setImageBitmap(messageIcon.getImage());
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                        lp.gravity = Gravity.CENTER;
                        imageBarButtons.setLayoutParams(lp);
                        topLinearLayout.addView(imageBarButtons, j);
                    }
                } else {
                    rosterBarLayout.setVisibility(LinearLayout.GONE);
                    topLinearLayout.setVisibility(LinearLayout.GONE);
                }
            }
        });
    }

    private void updatePage(final int currPage) {
        if (adaptersPages != null && adaptersPages.size() > 0)
            adaptersPages.get(currPage).notifyDataSetChanged();
    }

    @Override
    public void updateRoster() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (viewPager.getCurrentItem() == RosterAdapter.OPEN_CHATS) {
                    Util.sort(ChatHistory.instance.chats());
                    adaptersPages.get(RosterAdapter.OPEN_CHATS).notifyDataSetChanged();
                } else {
                    rebuildRoster(viewPager.getCurrentItem());
                }
            }
        });
    }
}