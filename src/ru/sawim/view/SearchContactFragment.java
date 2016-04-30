package ru.sawim.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.SearchView;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import protocol.Contact;
import protocol.ContactMenu;
import protocol.Group;
import protocol.Protocol;
import protocol.StatusInfo;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.listener.OnUpdateRoster;
import ru.sawim.models.RosterAdapter;
import ru.sawim.roster.ProtocolBranch;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeNode;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.roster.RosterViewRoot;

/**
 * Created by gerc on 31.12.2015.
 */
public class SearchContactFragment extends SawimFragment
        implements OnUpdateRoster, Handler.Callback,
        MenuItemCompat.OnActionExpandListener, View.OnClickListener, MyListView.OnItemClickListener {

    public static final String TAG = SearchContactFragment.class.getSimpleName();

    private static final int UPDATE_PROGRESS_BAR = 0;
    private static final int UPDATE_ROSTER = 1;
    private static final int PUT_INTO_QUEUE = 2;

    RosterViewRoot rosterViewLayout;
    AppCompatButton connectBtn;
    Handler handler;
    MenuItem searchMenuItem;

    public SearchContactFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        handler = new Handler(this);

        MyListView listView = (MyListView) LayoutInflater.from(context).inflate(R.layout.recycler_view, null);
        RosterAdapter rosterAdapter = new RosterAdapter();
        rosterAdapter.setType(RosterHelper.ALL_CONTACTS);
        listView.setAdapter(rosterAdapter);
        registerForContextMenu(listView);
        listView.setOnItemClickListener(this);
        FrameLayout.LayoutParams listViewLP = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        listView.setLayoutParams(listViewLP);

        ProgressBar progressBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.getProgressDrawable().setBounds(progressBar.getProgressDrawable().getBounds());
        FrameLayout.LayoutParams progressBarLP = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBarLP.setMargins(30, 0, 30, 1);
        progressBarLP.gravity = Gravity.TOP;
        progressBar.setLayoutParams(progressBarLP);
        progressBar.setVisibility(View.GONE);

        rosterViewLayout = new RosterViewRoot(getActivity(), progressBar, listView);

        connectBtn = new AppCompatButton(getActivity());
        connectBtn.setText(R.string.connect);
        FrameLayout.LayoutParams connectBtnLP = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        connectBtnLP.gravity = Gravity.CENTER;
        connectBtn.setLayoutParams(connectBtnLP);
        rosterViewLayout.addView(connectBtn);
        connectBtn.setOnClickListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rosterViewLayout.getParent() != null)
            ((ViewGroup) rosterViewLayout.getParent()).removeView(rosterViewLayout);

        ActionBar actionBar = ((BaseActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(null);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }

        return rosterViewLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().supportInvalidateOptionsMenu();
        setHasOptionsMenu(true);

        ActionBar actionBar = ((BaseActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getListView().setOnItemClickListener(null);
        unregisterForContextMenu(getListView());
        getListView().setAdapter(null);
        handler = null;
        rosterViewLayout = null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE_PROGRESS_BAR:
                updateProgressBarSync();
                break;
            case UPDATE_ROSTER:
                updateRosterSync();
                break;
            case PUT_INTO_QUEUE:
                if (getRosterAdapter() != null) {
                    getRosterAdapter().putIntoQueue((Group) msg.obj);
                }
                break;
        }
        return false;
    }

    private int oldProgressBarPercent;
    private void updateProgressBarSync() {
        final Protocol p = RosterHelper.getInstance().getProtocol(0);
        if (RosterHelper.getInstance().getProtocolCount() == 1 && p != null) {
            byte percent = p.getConnectingProgress();
            if (oldProgressBarPercent != percent) {
                oldProgressBarPercent = percent;
                connectBtn.setVisibility(View.GONE);
                BaseActivity activity = (BaseActivity) getActivity();
                if (100 != percent) {
                    rosterViewLayout.getProgressBar().setVisibility(ProgressBar.VISIBLE);
                    rosterViewLayout.getProgressBar().setProgress(percent);
                }
                if (100 == percent || 0 == percent) {
                    rosterViewLayout.getProgressBar().setVisibility(ProgressBar.GONE);
                    activity.supportInvalidateOptionsMenu();
                }
            }
        }
    }

    private void updateRosterSync() {
        RosterHelper.getInstance().updateOptions();
        if (getRosterAdapter() != null) {
            getRosterAdapter().refreshList();
            if (getRosterAdapter().getItemCount() == 0) {
                connectBtn.setVisibility(View.VISIBLE);
            } else {
                connectBtn.setVisibility(View.GONE);
            }
        }
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
        updateRosterSync();
        updateProgressBarSync();
    }

    @Override
    public void onPause() {
        super.onPause();
        RosterHelper.getInstance().setOnUpdateRoster(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (SawimApplication.isManyPane())
            ((SawimActivity) getActivity()).recreateActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        getRosterAdapter().setType(RosterHelper.ALL_CONTACTS);
        RosterHelper.getInstance().setOnUpdateRoster(this);
        update();
        getActivity().supportInvalidateOptionsMenu();
        if (Scheme.isChangeTheme(Scheme.getSavedTheme())) {
            ((SawimActivity) getActivity()).recreateActivity();
        }
    }

    @Override
    public boolean hasBack() {
        return true;
    }

    @Override
    public void onPrepareOptionsMenu_(Menu menu) {
        getActivity().getMenuInflater().inflate(R.menu.menu_search_contact, menu);

        searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setOnQueryTextListener(new OnQueryTextListener(getRosterAdapter()));
        searchView.setQueryHint(JLocale.getString(R.string.search_user));
        searchView.setIconifiedByDefault(false);

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, this);
    }

    @Override
    public void onItemClick(View view, int position) {
        TreeNode treeNode = getRosterAdapter().getItem(position);
        if (treeNode.getType() == TreeNode.CONTACT) {
            openChat(((Contact) treeNode).getProtocol(), ((Contact) treeNode), null);
        }
        if (treeNode.getType() == TreeNode.PROTOCOL) {
            ProtocolBranch group = (ProtocolBranch) treeNode;
            RosterHelper roster = RosterHelper.getInstance();
            final int count = roster.getProtocolCount();
            int currProtocol = 0;
            for (int i = 0; i < count; ++i) {
                Protocol p = roster.getProtocol(i);
                if (p == null) return;
                ProtocolBranch root = p.getProtocolBranch(i);
                if (root.getGroupId() != group.getGroupId()) {
                    root.setExpandFlag(false);
                } else {
                    currProtocol = i;
                }
            }
            group.setExpandFlag(!group.isExpanded());
            update();
            getListView().smoothScrollToPosition(currProtocol);
        } else if (treeNode.getType() == TreeNode.GROUP) {
            Group group = RosterHelper.getInstance().getGroupWithContacts((Group) treeNode);
            group.setExpandFlag(!group.isExpanded());
            update();
        }
    }

    private static class OnQueryTextListener implements SearchView.OnQueryTextListener {

        RosterAdapter adapter;

        OnQueryTextListener(RosterAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            String query = newText.toLowerCase();
            adapter.filterData(query);
            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }
    };

    @Override
    public void onDestroyOptionsMenu() {
        if (searchMenuItem != null) {
            SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
            MenuItemCompat.setOnActionExpandListener(searchMenuItem, null);
            searchView.setOnQueryTextListener(null);
            searchMenuItem = null;
        }
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        // Do something when collapsed
        getRosterAdapter().filterData(null);
        return true; // Return true to collapse action view
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        // Do something when expanded
        return true; // Return true to expand action view
    }

    private void openChat(Protocol p, Contact c, String sharingText) {
        getFragmentManager().popBackStack();
        c.activate((BaseActivity) getActivity(), p);
        if (SawimApplication.isManyPane()) {
            ChatView chatViewTablet = (ChatView) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatViewTablet.pause(chatViewTablet.getCurrentChat());
            chatViewTablet.openChat(p, c);
            chatViewTablet.setSharingText(sharingText);
            chatViewTablet.resume(chatViewTablet.getCurrentChat());
            update();
        } else {
            ChatView chatView = ChatView.newInstance(p.getUserId(), c.getUserId());
            chatView.setSharingText(sharingText);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, chatView, ChatView.TAG);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == connectBtn) {
            connectBtn.setVisibility(View.GONE);
            Protocol p = RosterHelper.getInstance().getProtocol(0);
            if (p == null) {
                getFragmentManager().beginTransaction().addToBackStack(null)
                        .replace(R.id.fragment_container, new StartWindowView(), StartWindowView.TAG).commit();
            } else {
                SawimApplication.getInstance().setStatus(p, (p.isConnected() || p.isConnecting())
                        ? StatusInfo.STATUS_OFFLINE : StatusInfo.STATUS_ONLINE, "");
            }
            return;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MyListView.RecyclerContextMenuInfo contextMenuInfo = (MyListView.RecyclerContextMenuInfo) menuInfo;
        TreeNode treeNode = getRosterAdapter().getItem(contextMenuInfo.position);
        if (treeNode.getType() == TreeNode.PROTOCOL) {
            RosterHelper.getInstance().showProtocolMenu((BaseActivity) getActivity(), ((ProtocolBranch) treeNode).getProtocol());
        } else if (treeNode.getType() == TreeNode.GROUP) {
            Protocol p = RosterHelper.getInstance().getProtocol((Group) treeNode);
            if (p.isConnected()) {
                new ManageContactListForm(p, (Group) treeNode).showMenu((BaseActivity) getActivity());
            }
        } else if (treeNode.getType() == TreeNode.CONTACT) {
            new ContactMenu(((Contact) treeNode).getProtocol(), (Contact) treeNode).getContextMenu(menu);
        }
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        MyListView.RecyclerContextMenuInfo menuInfo = (MyListView.RecyclerContextMenuInfo) item.getMenuInfo();
        TreeNode treeNode = getRosterAdapter().getItem(menuInfo.position);
        if (treeNode == null) return false;
        if (treeNode.getType() == TreeNode.CONTACT) {
            Contact contact = (Contact) treeNode;
            contactMenuItemSelected(contact, item);
        }
        return super.onContextItemSelected(item);
    }

    private void contactMenuItemSelected(final Contact c, final android.view.MenuItem item) {
        new ContactMenu(c.getProtocol(), c).doAction((BaseActivity) getActivity(), item.getItemId());
    }

    public RosterAdapter getRosterAdapter() {
        if (rosterViewLayout == null || rosterViewLayout.getMyListView() == null) return null;
        return (RosterAdapter) getListView().getAdapter();
    }

    public MyListView getListView() {
        return rosterViewLayout.getMyListView();
    }
}
