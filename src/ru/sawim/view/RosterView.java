package ru.sawim.view;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.*;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.ProtocolsAdapter;
import ru.sawim.models.RosterAdapter;
import ru.sawim.widget.*;
import ru.sawim.widget.roster.RosterViewRoot;
import sawim.Options;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.forms.ManageContactListForm;
import sawim.roster.RosterHelper;
import sawim.roster.TreeNode;
import sawim.util.JLocale;


/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 01.04.13
 * Time: 19:58
 * To change this template use File | Settings | File Templates.
 */
public class RosterView extends Fragment implements ListView.OnItemClickListener, RosterHelper.OnUpdateRoster, Handler.Callback {

    public static final String TAG = RosterView.class.getSimpleName();

    private static final int UPDATE_BAR_PROTOCOLS = 0;
    private static final int UPDATE_PROGRESS_BAR = 1;
    private static final int UPDATE_ROSTER = 2;
    private static final int PUT_INTO_QUEUE = 3;

    private CustomDrawerLayout drawerLayout;
    private LinearLayout barLinearLayout;
    private RosterViewRoot rosterViewLayout;
    private static ProgressBar progressBar;
    private MyListView rosterListView;
    private MyListView protocolsListView;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;
    private Handler handler;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        SawimApplication.setCurrentActivity((ActionBarActivity) activity);
        handler = new Handler(this);
        barLinearLayout = new LinearLayout(activity);

        if (progressBar == null) {
            progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(100);
            progressBar.getProgressDrawable().setBounds(progressBar.getProgressDrawable().getBounds());
            LinearLayout.LayoutParams ProgressBarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ProgressBarLP.setMargins(30, 0, 30, 1);
            progressBar.setLayoutParams(ProgressBarLP);
        }

        drawerLayout = new CustomDrawerLayout(activity);
        protocolsListView = new MyListView(activity);
        DrawerLayout.LayoutParams nickListLP = new DrawerLayout.LayoutParams(Util.dipToPixels(getActivity(), 240), DrawerLayout.LayoutParams.MATCH_PARENT);
        DrawerLayout.LayoutParams drawerLayoutLP = new DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
        drawerLayout.setScrimColor(Scheme.isBlack() ? 0x55FFFFFF : 0x99000000);
        nickListLP.gravity = Gravity.START;
        drawerLayout.setLayoutParams(drawerLayoutLP);
        protocolsListView.setBackgroundResource(Util.getSystemBackground(getActivity()));
        protocolsListView.setLayoutParams(nickListLP);
        protocolsListView.setAdapter(new ProtocolsAdapter(activity));
        protocolsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (RosterHelper.getInstance().getCurrentItemProtocol() != position) {
                    RosterHelper.getInstance().setCurrentItemProtocol(position);
                    RosterHelper.getInstance().setCurrPage(RosterHelper.ALL_CONTACTS);
                    getRosterAdapter().setType(RosterHelper.ALL_CONTACTS);
                    SawimApplication.getActionBar().setSelectedNavigationItem(RosterHelper.ALL_CONTACTS);
                    update();
                    SawimApplication.getCurrentActivity().supportInvalidateOptionsMenu();
                }
                drawerLayout.closeDrawer(protocolsListView);
            }
        });
        drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {}

            @Override
            public void onDrawerOpened(View drawerView) {
                if (RosterHelper.getInstance().getProtocolCount() > 1 && !SawimApplication.isManyPane()) {
                    initBar(R.string.options_account);
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                if (RosterHelper.getInstance().getProtocolCount() > 1 && !SawimApplication.isManyPane()) {
                    initBar(R.string.options_account);
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_SETTLING) {
                    if (protocolsListView.getAdapter() != null) {
                        ((ProtocolsAdapter) protocolsListView.getAdapter()).notifyDataSetChanged();
                    }
                }
            }
        });

        drawerToggle = new ActionBarDrawerToggle(activity, drawerLayout, R.drawable.ic_drawer, 0, 0) {
            public void onDrawerClosed(View view) {
            }

            public void onDrawerOpened(View view) {
                if (protocolsListView.getAdapter() != null) {
                    ((ProtocolsAdapter) protocolsListView.getAdapter()).notifyDataSetChanged();
                }
            }
        };

        rosterListView = new MyListView(activity);
        RosterAdapter rosterAdapter = new RosterAdapter(activity);
        rosterAdapter.setType(RosterHelper.getInstance().getCurrPage());
        LinearLayout.LayoutParams rosterListViewLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rosterListView.setLayoutParams(rosterListViewLayoutParams);
        rosterListView.setAdapter(rosterAdapter);
        activity.registerForContextMenu(rosterListView);
        rosterListView.setOnCreateContextMenuListener(this);
        rosterListView.setOnItemClickListener(this);
        rosterViewLayout = new RosterViewRoot(SawimApplication.getCurrentActivity(), progressBar, rosterListView);
    }

    public ActionBarDrawerToggle getDrawerToggle() {
        return drawerToggle;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerToggle.syncState();

        if (drawerLayout.getParent() != null) {
            ((ViewGroup) drawerLayout.getParent()).removeView(drawerLayout);
        }
        if (rosterViewLayout.getParent() != null)
            ((ViewGroup) rosterViewLayout.getParent()).removeView(rosterViewLayout);
        if (protocolsListView.getParent() != null) {
            ((ViewGroup) protocolsListView.getParent()).removeView(protocolsListView);
        }
        drawerLayout.addView(rosterViewLayout);
        drawerLayout.addView(protocolsListView);
        return drawerLayout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        drawerLayout.setDrawerListener(null);
        drawerToggle.setDrawerIndicatorEnabled(false);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE_BAR_PROTOCOLS:
                final int protocolCount = RosterHelper.getInstance().getProtocolCount();
                if (protocolCount > 1) {
                    if (protocolsListView.getAdapter() != null) {
                        ((ProtocolsAdapter) protocolsListView.getAdapter()).notifyDataSetChanged();
                    }
                }
                break;
            case UPDATE_PROGRESS_BAR:
                final Protocol p = RosterHelper.getInstance().getCurrentProtocol();
                if (p != null) {
                    byte percent = p.getConnectingProgress();
                    if (100 != percent) {
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                        progressBar.setProgress(percent);
                    } else {
                        progressBar.setVisibility(ProgressBar.GONE);
                    }
                    if (100 == percent || 0 == percent) {
                        SawimApplication.getCurrentActivity().supportInvalidateOptionsMenu();
                    }
                }
                break;
            case UPDATE_ROSTER:
                RosterHelper.getInstance().updateOptions();
                if (rosterListView.getAdapter() != null) {
                    ((RosterAdapter) rosterListView.getAdapter()).refreshList();
                }
                if (drawerLayout != null && drawerLayout.isDrawerOpen(protocolsListView)) {
                    ((ProtocolsAdapter) protocolsListView.getAdapter()).notifyDataSetChanged();
                }
                break;
            case PUT_INTO_QUEUE:
                if (rosterListView.getAdapter() != null) {
                    ((RosterAdapter) rosterListView.getAdapter()).putIntoQueue((Group) msg.obj);
                }
                break;
        }
        return false;
    }

    @Override
    public void updateBarProtocols() {
        if (handler == null) return;
        handler.sendEmptyMessage(UPDATE_BAR_PROTOCOLS);
    }

    @Override
    public void updateProgressBar() {
        if (handler == null) return;
        handler.sendEmptyMessage(UPDATE_PROGRESS_BAR);
    }

    @Override
    public void updateRoster() {
        if (handler == null) return;
        handler.sendEmptyMessage(UPDATE_ROSTER);
    }

    @Override
    public void putIntoQueue(final Group g) {
        if (handler == null) return;
        handler.sendMessage(Message.obtain(handler, PUT_INTO_QUEUE, g));
    }

    public void update() {
        updateRoster();
        updateBarProtocols();
        updateProgressBar();
    }

    private void initBar(int title) {
        SawimApplication.getActionBar().setDisplayShowTitleEnabled(true);
        SawimApplication.getActionBar().setDisplayShowHomeEnabled(true);
        SawimApplication.getActionBar().setDisplayUseLogoEnabled(true);
        SawimApplication.getActionBar().setDisplayHomeAsUpEnabled(true);
        SawimApplication.getActionBar().setHomeButtonEnabled(true);
        SawimApplication.getActionBar().setDisplayShowCustomEnabled(false);
        SawimApplication.getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        SawimApplication.getCurrentActivity().setTitle(title);

        String[] data = new String[] {JLocale.getString(R.string.all_contacts), JLocale.getString(R.string.online_contacts), JLocale.getString(R.string.active_contacts) };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(SawimApplication.getCurrentActivity(),
                android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SawimApplication.getActionBar().setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                RosterHelper.getInstance().setCurrPage(itemPosition);
                getRosterAdapter().setType(itemPosition);
                update();
                SawimApplication.getCurrentActivity().supportInvalidateOptionsMenu();
                return false;
            }
        });

        if (SawimApplication.isManyPane()) {
            ChatView chatView = (ChatView) SawimApplication.getCurrentActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            barLinearLayout.removeAllViews();
            chatView.removeTitleBar();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            barLinearLayout.setLayoutParams(layoutParams);
            barLinearLayout.addView(chatView.getTitleBar());
            SawimApplication.getActionBar().setCustomView(barLinearLayout);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resume();
        if (!SawimApplication.isManyPane() && Scheme.isChangeTheme(Options.getInt(Options.OPTION_COLOR_SCHEME))) {
            ((SawimActivity)SawimApplication.getCurrentActivity()).recreateActivity();
        }
    }

    public void resume() {
        boolean drawerVisible = drawerLayout != null && drawerLayout.isDrawerOpen(protocolsListView);
        SawimApplication.setCurrentActivity((ActionBarActivity) getActivity());
        if (SawimApplication.getActionBar() == null)
            SawimApplication.setActionBar(SawimApplication.getCurrentActivity().getSupportActionBar());
        initBar(drawerVisible ? R.string.options_account : R.string.app_name);

        RosterHelper.getInstance().setCurrPage(RosterHelper.getInstance().getCurrPage());
        getRosterAdapter().setType(RosterHelper.getInstance().getCurrPage());
        SawimApplication.getActionBar().setSelectedNavigationItem(RosterHelper.getInstance().getCurrPage());

        if (RosterHelper.getInstance().getProtocolCount() > 0) {
            RosterHelper.getInstance().setCurrentContact(null);
            RosterHelper.getInstance().setOnUpdateRoster(this);
            if (SawimApplication.returnFromAcc) {
                SawimApplication.returnFromAcc = false;
                if (RosterHelper.getInstance().getCurrentProtocol().getContactItems().size() == 0
                        && !RosterHelper.getInstance().getCurrentProtocol().isConnecting())
                    Toast.makeText(SawimApplication.getCurrentActivity(), R.string.press_menu_for_connect, Toast.LENGTH_LONG).show();
                if (protocolsListView.getAdapter() != null) {
                    ((ProtocolsAdapter) protocolsListView.getAdapter()).notifyDataSetChanged();
                }
            }
            update();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        RosterHelper.getInstance().setOnUpdateRoster(null);
    }

    public RosterAdapter getRosterAdapter() {
        return ((RosterAdapter) rosterListView.getAdapter());
    }

    private void openChat(Protocol p, Contact c, boolean allowingStateLoss) {
        c.activate(p);
        if (!SawimApplication.isManyPane()) {
            ChatView chatView = new ChatView();
            chatView.initChat(p, c);
            FragmentTransaction transaction = SawimApplication.getCurrentActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, chatView, ChatView.TAG);
            transaction.addToBackStack(null);
            if (allowingStateLoss)
                transaction.commitAllowingStateLoss();
            else
                transaction.commit();
        } else {
            ChatView chatViewTablet = (ChatView) SawimApplication.getCurrentActivity().getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment);
            chatViewTablet.pause(chatViewTablet.getCurrentChat());
            if (c != null) {
                chatViewTablet.openChat(p, c);
                chatViewTablet.resume(chatViewTablet.getCurrentChat());
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
            Object o = rosterListView.getAdapter().getItem(position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                openChat(chat.getProtocol(), chat.getContact(), false);
                if (SawimApplication.isManyPane())
                    update();
            }
        } else {
            TreeNode item = (TreeNode) rosterListView.getAdapter().getItem(position);
            if (item.isContact()) {
                openChat(RosterHelper.getInstance().getCurrentProtocol(), ((Contact) item), false);
                if (SawimApplication.isManyPane())
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
        if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
            Object o = rosterListView.getAdapter().getItem(contextMenuInfo.position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                new ContactMenu(chat.getProtocol(), chat.getContact()).getContextMenu(menu);
            }
        } else {
            TreeNode node = (TreeNode) rosterListView.getAdapter().getItem(contextMenuInfo.position);
            Protocol p = RosterHelper.getInstance().getCurrentProtocol();
            if (node.isContact()) {
                new ContactMenu(p, (Contact) node).getContextMenu(menu);
                return;
            }
            if (node.isGroup()) {
                if (p.isConnected()) {
                    new ManageContactListForm(p, (Group) node).showMenu(SawimApplication.getCurrentActivity());
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (menuInfo == null)
            menuInfo = contextMenuInfo;
        if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
            Object o = rosterListView.getAdapter().getItem(menuInfo.position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                contactMenuItemSelected(chat.getContact(), item);
            }
        } else {
            TreeNode node = (TreeNode) rosterListView.getAdapter().getItem(menuInfo.position);
            if (node == null) return false;
            if (node.isContact()) {
                contactMenuItemSelected((Contact) node, item);
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    private void contactMenuItemSelected(final Contact c, final android.view.MenuItem item) {
        Protocol p = RosterHelper.getInstance().getCurrentProtocol();
        if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS)
            p = c.getProtocol();
        new ContactMenu(p, c).doAction(item.getItemId());
    }
}