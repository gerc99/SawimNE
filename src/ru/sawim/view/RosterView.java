package ru.sawim.view;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.FragmentTransaction;
import ru.sawim.General;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;
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
public class RosterView extends Fragment implements View.OnClickListener, ListView.OnItemClickListener, Roster.OnUpdateRoster, View.OnLongClickListener {

    public static final String TAG = "RosterView";
    private RosterViewRoot rosterViewLayout;
    private HorizontalScrollView horizontalScrollView;
    private LinearLayout protocolBarLayout;
    private ProgressBar progressBar;
    private ViewPager viewPager;
    private ViewPager.LayoutParams viewPagerLayoutParams;
    private PagerTitleStrip indicator;
    private ArrayList<RosterAdapter> adaptersPages = new ArrayList<RosterAdapter>();
    private CustomPagerAdapter pagerAdapter;
    private Roster roster;

    private AdapterView.AdapterContextMenuInfo contextMenuInfo;
    private HashMap<Integer, ImageButton> protocolIconsHash = new HashMap<Integer, ImageButton>();

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
        RosterAdapter allRosterAdapter = new RosterAdapter(getActivity(), roster, RosterAdapter.ALL_CONTACTS);
        RosterAdapter onlineRosterAdapter = new RosterAdapter(getActivity(), roster, RosterAdapter.ONLINE_CONTACTS);
        RosterAdapter activeRosterAdapter = new RosterAdapter(getActivity(), roster, RosterAdapter.ACTIVE_CONTACTS);
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

        List<View> pages = new ArrayList<View>();
        pages.add(allListView);
        pages.add(onlineListView);
        pages.add(activeListView);
        pagerAdapter = new CustomPagerAdapter(pages);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        horizontalScrollView = new HorizontalScrollView(activity);
        LinearLayout.LayoutParams protocolBarLayoutLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, Util.dipToPixels(getActivity(), 50));
        protocolBarLayout = new LinearLayout(activity);
        protocolBarLayout.setOrientation(LinearLayout.HORIZONTAL);
        protocolBarLayout.setLayoutParams(protocolBarLayoutLP);
        horizontalScrollView.addView(protocolBarLayout);

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
            addViewInLayout(horizontalScrollView, 0, horizontalScrollViewLP, true);

            LinearLayout.LayoutParams ProgressBarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ProgressBarLP.setMargins(30, 0, 30, 1);
            addViewInLayout(progressBar, 1, ProgressBarLP, true);
            addViewInLayout(viewPager, 2, viewPagerLayoutParams, true);
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
        rosterViewLayout.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));

        horizontalScrollView.setBackgroundColor(Scheme.getColor(Scheme.THEME_CAP_BACKGROUND));

        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(roster.getCurrPage());

        indicator.setTextColor(Scheme.getColor(Scheme.THEME_GROUP));
        indicator.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));

        if (roster.getProtocolCount() > 1) {
            horizontalScrollView.setVisibility(LinearLayout.VISIBLE);
            protocolBarLayout.setVisibility(LinearLayout.VISIBLE);
        } else {
            horizontalScrollView.setVisibility(LinearLayout.GONE);
            protocolBarLayout.setVisibility(LinearLayout.GONE);
        }
        return rosterViewLayout;
    }

    private static void showStartWindow() {
        if (General.sawimActivity.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            General.sawimActivity.setContentView(R.layout.intercalation_layout);
        StartWindowView newFragment = new StartWindowView();
        FragmentTransaction transaction = General.sawimActivity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void putIntoQueue(final Group g) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (adaptersPages != null && adaptersPages.size() > 0) {
                    adaptersPages.get(viewPager.getCurrentItem()).putIntoQueue(g);
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
                    adaptersPages.get(viewPager.getCurrentItem()).buildFlatItems();
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

    private void update() {
        updateRoster();
        updateBarProtocols();
        updateProgressBar();
    }

    @Override
    public void updateBarProtocols() {
        final int protocolCount = roster.getProtocolCount();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (protocolCount > 1) {
                    protocolBarLayout.removeAllViews();
                    for (int i = 0; i < protocolCount; ++i) {
                        Protocol protocol = roster.getProtocol(i);
                        ImageButton imageBarButtons = protocolIconsHash.get(i);
                        if (imageBarButtons == null) {
                            imageBarButtons = new ImageButton(getActivity());
                            imageBarButtons.setOnClickListener(RosterView.this);
                            imageBarButtons.setOnLongClickListener(RosterView.this);
                            protocolIconsHash.put(i, imageBarButtons);
                            imageBarButtons.setId(i);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
                            lp.gravity = Gravity.CENTER;
                            imageBarButtons.setLayoutParams(lp);
                        }
                        imageBarButtons.setBackgroundDrawable(new ColorDrawable(0));
                        if (i == roster.getCurrentItemProtocol())
                            imageBarButtons.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
                        imageBarButtons.setImageDrawable(protocol.getCurrentStatusIcon().getImage());
                        Icon messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
                        if (null != messageIcon)
                            imageBarButtons.setImageDrawable(messageIcon.getImage());

                        protocolBarLayout.addView(imageBarButtons);
                        protocolBarLayout.addView(Util.getDivider(getActivity(), true, Scheme.getColor(Scheme.THEME_BACKGROUND)));
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (roster == null) return;
        if (roster.getProtocolCount() == 0) return;
        roster.setCurrentContact(null);
        roster.setOnUpdateRoster(this);
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
            chatView = new ChatView();
            Bundle args = new Bundle();
            args.putString(ChatView.PROTOCOL_ID, p.getUserId());
            args.putString(ChatView.CONTACT_ID, c.getUserId());
            chatView.setArguments(args);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, chatView, ChatView.TAG);
            transaction.addToBackStack(null);
            if (allowingStateLoss)
                transaction.commitAllowingStateLoss();
            else
                transaction.commit();
        } else {
            chatView.pause(chatView.getCurrentChat());
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
            if (getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment) != null)
                updateRoster();
        } else if (item.isGroup()) {
            Group group = (Group) item;
            group.setExpandFlag(!group.isExpanded());
            updateRoster();
        }
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