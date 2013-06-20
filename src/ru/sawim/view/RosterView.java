package ru.sawim.view;

import DrawControls.icons.Icon;
import DrawControls.tree.TreeBranch;
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
import sawim.chat.ChatHistory;
import sawim.cl.ContactList;
import sawim.comm.Util;
import sawim.forms.ManageContactListForm;
import sawim.modules.DebugLog;
import sawim.ui.base.Scheme;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.activities.AccountsListActivity;
import ru.sawim.activities.ChatActivity;
import ru.sawim.models.ContactsAdapter;
import ru.sawim.models.CustomPagerAdapter;
import ru.sawim.models.RosterAdapter;

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
public class RosterView extends Fragment implements View.OnClickListener, ListView.OnItemClickListener, VirtualContactList.OnUpdateRoster {

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
    private ContactsAdapter onlineRosterAdapter;
    private ContactsAdapter chatsRosterAdapter;
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
        if (general.getCurrProtocol() == null) {
            startActivity(new Intent(currentActivity, AccountsListActivity.class));
            return;
        }
        adaptersPages.clear();
        ListView allListView = new ListView(currentActivity);
        ListView onlineListView = new ListView(currentActivity);
        ListView chatsListView = new ListView(currentActivity);

        rosterViewLayout.setBackgroundColor(General.getColor(Scheme.THEME_BACKGROUND));
        indicator.setTextColor(General.getColor(Scheme.THEME_GROUP));
        indicator.setBackgroundColor(General.getColorWithAlpha(Scheme.THEME_BACKGROUND));
        rosterBarLayout.setBackgroundColor(General.getColorWithAlpha(Scheme.THEME_CAP_BACKGROUND));

        LayoutInflater inf = LayoutInflater.from(currentActivity);
        allRosterAdapter = new RosterAdapter(inf, owner, items);
        onlineRosterAdapter = new ContactsAdapter(inf, owner, ContactsAdapter.ONLINE_CONTACTS);
        chatsRosterAdapter = new ContactsAdapter(inf, owner, ContactsAdapter.OPEN_CHATS);

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
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rebuildRoster(pos);
                    }
                });
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        Update();
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

    public int getCurrItem() {
        return ((ListView) pages.get(viewPager.getCurrentItem())).getFirstVisiblePosition();
    }

    private TreeNode getCurrentNode() {
        return getSafeNode(getCurrItem());
    }

    public TreeNode getSafeNode(int index) {
        if ((index < items.size()) && (index >= 0)) {
            return items.get(index);
        }
        return null;
    }

    public void setExpandFlag(TreeBranch node, boolean value) {
        setCurrentNode(getCurrentNode());
        node.setExpandFlag(value);
        updateRoster();
    }

    @Override
    public void setCurrentNode(TreeNode node) {
        if (null != node) {
            currentNode = node;
        }
    }

    private void rebuildRoster(int pos) {
        while (!updateQueue.isEmpty()) {
            Group group = (Group) updateQueue.firstElement();
            updateQueue.removeElementAt(0);
            owner.getModel().updateGroup(group);
        }
        try {
            owner.updateOption();

            TreeNode current = currentNode;
            currentNode = null;
            int prevIndex = getCurrItem();
            if (null != current) {
                owner.expandNodePath(current);
            } else {
                current = getSafeNode(prevIndex);
            }
            items.clear();
            owner.getModel().buildFlatItems(owner.getCurrProtocol(), items);
            updatePage(pos);
            if (null != current) {
                int currentIndex = Util.getIndex(items, current);
                if ((prevIndex != currentIndex) && (-1 != currentIndex)) {
                    setCurrentItemIndex(currentIndex);
                }
            }
            if (items.size() <= getCurrItem()) {
                //setCurrentItemIndex(0);
            }
        } catch (Exception e) {
            DebugLog.panic("update ", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (owner == null) return;
        Update();
        if (adaptersPages.size() > 0)
            viewPager.setCurrentItem(1);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        ChatView viewer = (ChatView) getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment);
        Object item = adaptersPages.get(viewPager.getCurrentItem()).getItem(position);
        if (item instanceof Contact) {
            Contact c = ((Contact) item);
            Protocol p = c.getProtocol();
            c.activate(p);
            if (!isInLayout()) return;
            if (viewer == null || !viewer.isInLayout()) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("protocol_id", p.getUserId());
                intent.putExtra("contact_id", c.getUserId());
                getActivity().startActivity(intent);
            } else {
                if (c != null) {
                    viewer.openChat(p, c);
                }
            }
        } else if (item instanceof Group) {
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
        Object node = ((ListView) v).getAdapter().getItem(contextMenuInfo.position);
        Protocol p = general.getCurrProtocol();
        if (node instanceof Contact) {
            new ContactMenu(((Contact) node).getProtocol(), (Contact) node).getContextMenu(menu);
        }
        if (node instanceof Group) {
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
                    new ContactMenu(c.getProtocol(), c).doAction(item.getItemId());
                }
            });
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        owner.setCurrProtocol(view.getId());
        Update();
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

    private void Update() {
        updateBarProtocols();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rebuildRoster(viewPager.getCurrentItem());
            }
        });
    }

    @Override
    public void updateBarProtocols() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateProgressBar();
                topLinearLayout.removeAllViews();
                for (int j = 0; j < owner.getModel().getProtocolCount(); j++) {
                    Protocol protocol = owner.getModel().getProtocol(j);
                    ImageButton imageBarButtons = new ImageButton(getActivity());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(58, LinearLayout.LayoutParams.MATCH_PARENT);
                    lp.gravity = Gravity.CENTER;
                    imageBarButtons.setLayoutParams(lp);
                    if (j == owner.getCurrProtocol())
                        imageBarButtons.setBackgroundColor(General.getColorWithAlpha(Scheme.THEME_BACKGROUND));
                    imageBarButtons.setImageBitmap(General.iconToBitmap(protocol.getCurrentStatusIcon()));
                    imageBarButtons.setOnClickListener(RosterView.this);
                    imageBarButtons.setId(j);
                    Icon messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
                    if (null != messageIcon)
                        imageBarButtons.setImageBitmap(General.iconToBitmap(messageIcon));
                    topLinearLayout.addView(imageBarButtons, j);
                }
            }
        });
    }

    private void updatePage(final int currPage) {
        if (currPage == ContactsAdapter.ALL_CONTACTS) {

        } else if (currPage == ContactsAdapter.ONLINE_CONTACTS) {
            onlineRosterAdapter.clear();
            Vector contacts = general.getCurrProtocol().getSortedContacts();
            for (int i = 0; i < contacts.size(); ++i) {
                Contact c = (Contact) contacts.get(i);
                if (c.isVisibleInContactList())
                    onlineRosterAdapter.setItems(c);
            }
        } else if (currPage == ContactsAdapter.OPEN_CHATS) {
            chatsRosterAdapter.clear();
            ChatHistory chats = ChatHistory.instance;
            for (int i = 0; i < chats.getTotal(); ++i) {
                chatsRosterAdapter.setItems(chats.contactAt(i));
            }
        }
        if (adaptersPages != null && adaptersPages.size() > 0)
            adaptersPages.get(currPage).notifyDataSetChanged();
    }

    @Override
    public void updateRoster() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                general.getManager().getModel().sort();
                rebuildRoster(viewPager.getCurrentItem());
            }
        });
    }
}