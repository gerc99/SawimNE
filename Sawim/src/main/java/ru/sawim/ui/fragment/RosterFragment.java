package ru.sawim.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.*;
import android.widget.*;
import protocol.*;
import ru.sawim.*;
import ru.sawim.ui.activity.BaseActivity;
import ru.sawim.ui.activity.SawimActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.listener.OnUpdateRoster;
import ru.sawim.ui.adapter.RosterAdapter;
import ru.sawim.modules.FileTransfer;
import ru.sawim.roster.*;
import ru.sawim.ui.widget.MyImageButton;
import ru.sawim.ui.widget.MyListView;
import ru.sawim.ui.widget.Util;
import ru.sawim.ui.widget.roster.RosterViewRoot;


/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 01.04.13
 * Time: 19:58
 * To change this template use File | Settings | File Templates.
 */
public class RosterFragment extends SawimFragment implements View.OnClickListener, OnUpdateRoster, Handler.Callback, MyListView.OnItemClickListener {

    public static final String TAG = RosterFragment.class.getSimpleName();

    private static final int UPDATE_PROGRESS_BAR = 0;
    private static final int UPDATE_ROSTER = 1;
    private static final int PUT_INTO_QUEUE = 2;

    public static final int MODE_DEFAULT = 0;
    public static final int MODE_SHARE = 1;
    public static final int MODE_SHARE_TEXT = 2;
    private int mode;

    private TextView textViewBar;
    private RosterViewRoot rosterViewLayout;
    private Handler handler;
    private MyImageButton chatsImage;

    public static RosterFragment newInstance() {
        RosterFragment fragment = new RosterFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        handler = new Handler(this);

        chatsImage = new MyImageButton(activity);
        chatsImage.setOnClickListener(this);

        textViewBar = new TextView(activity);
        textViewBar.setText(R.string.app_name);
        textViewBar.setTextSize(19);
        textViewBar.setTypeface(Typeface.DEFAULT_BOLD);
        textViewBar.setTextColor(Color.WHITE);
        textViewBar.setGravity(Gravity.CENTER_VERTICAL);

        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.getProgressDrawable().setBounds(progressBar.getProgressDrawable().getBounds());
        FrameLayout.LayoutParams progressBarLP = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBarLP.setMargins(30, 0, 30, 1);
        progressBarLP.gravity = Gravity.TOP;
        progressBar.setLayoutParams(progressBarLP);
        progressBar.setVisibility(View.GONE);

        MyListView listView = (MyListView) LayoutInflater.from(context).inflate(R.layout.recycler_view, null);
        RosterAdapter rosterAdapter = new RosterAdapter();
        listView.setAdapter(rosterAdapter);
        registerForContextMenu(listView);
        listView.setOnItemClickListener(this);
        FrameLayout.LayoutParams listViewLP = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        listView.setLayoutParams(listViewLP);

        View emptyView = LayoutInflater.from(context).inflate(R.layout.roster_empty_view, null);
        Button findConfButton = (Button) emptyView.findViewById(R.id.find_conference_button);
        Button manageClButton = (Button) emptyView.findViewById(R.id.manage_contact_list_button);
        findConfButton.setOnClickListener(this);
        manageClButton.setOnClickListener(this);
        listView.setEmptyView(emptyView);

        FloatingActionButton fab = new FloatingActionButton(getActivity());
        fab.setImageResource(R.drawable.ic_pencil_white_24dp);
        fab.setOnClickListener(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.bottomMargin = Util.dipToPixels(SawimApplication.getContext(), 15);
        lp.rightMargin = Util.dipToPixels(SawimApplication.getContext(), 15);
        fab.setLayoutParams(lp);

        rosterViewLayout = new RosterViewRoot(activity, progressBar, listView, fab);
        rosterViewLayout.addView(emptyView);

        rosterAdapter.refreshList();
        initSwipe(listView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rosterViewLayout.getParent() != null)
            ((ViewGroup) rosterViewLayout.getParent()).removeView(rosterViewLayout);
        return rosterViewLayout;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        RosterHelper.getInstance().setOnAccountsLoaded(null);
        unregisterForContextMenu(getListView());
        getListView().setOnItemClickListener(null);
        getListView().setAdapter(null);
        rosterViewLayout.getFab().setOnClickListener(null);
        chatsImage.setOnClickListener(null);
        handler = null;
        rosterViewLayout = null;
        chatsImage = null;
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
        final Protocol p = RosterHelper.getInstance().getProtocol();
        if (p != null) {
            byte percent = p.getConnectingProgress();
            if (oldProgressBarPercent != percent) {
                oldProgressBarPercent = percent;
                BaseActivity activity = (BaseActivity) getActivity();
                if (100 == percent) {
                    if (getRosterAdapter().getItemCount() == 0) {
                        getListView().getEmptyView().setVisibility(View.VISIBLE);
                    }
                } else {
                    getListView().getEmptyView().setVisibility(View.GONE);
                    rosterViewLayout.getProgressBar().setVisibility(View.VISIBLE);
                    rosterViewLayout.getProgressBar().setProgress(percent);
                }
                if (100 == percent || 0 == percent) {
                    rosterViewLayout.getProgressBar().setVisibility(View.GONE);
                    activity.supportInvalidateOptionsMenu();
                }
            }
        }
    }

    private void updateRosterSync() {
        updateChatImage();
        RosterHelper.getInstance().updateOptions();
        if (getRosterAdapter() != null) {
            getRosterAdapter().refreshList();
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

    private void updateChatImage() {
        if (chatsImage == null) return;
        Drawable icMess = ChatHistory.instance.getLastUnreadMessageIcon();
        if (icMess == null) {
            chatsImage.setVisibility(View.GONE);
        } else {
            if (icMess == SawimResources.PERSONAL_MESSAGE_ICON) {
                icMess = icMess.getConstantState().newDrawable();
                icMess.setColorFilter(Scheme.getColor(R.attr.bar_personal_unread_message), PorterDuff.Mode.MULTIPLY);
            } else {
                icMess = icMess.getConstantState().newDrawable();
                icMess.setColorFilter(Scheme.getColor(R.attr.bar_unread_message), PorterDuff.Mode.MULTIPLY);
            }
            chatsImage.setVisibility(View.VISIBLE);
            chatsImage.setImageDrawable(icMess);
        }
    }

    private void initBar() {
        ActionBar actionBar = ((BaseActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        Toolbar.LayoutParams barLinearLayoutLP = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams rosterBarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams chatsImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        chatsImageLP.gravity = Gravity.END;
        chatsImageLP.weight = 4;
        rosterBarLP.weight = 1;
        textViewBar.setLayoutParams(rosterBarLP);
        LinearLayout barLinearLayout = new LinearLayout(getActivity());
        if (SawimApplication.isManyPane()) {
            LinearLayout rosterBarLayout = new LinearLayout(getActivity());
            LinearLayout.LayoutParams rosterBarLayoutLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            rosterBarLayoutLP.weight = 2;
            rosterBarLayout.setLayoutParams(rosterBarLayoutLP);
            rosterBarLayout.removeAllViews();
            if (textViewBar.getParent() != null)
                ((ViewGroup) textViewBar.getParent()).removeView(textViewBar);
            rosterBarLayout.addView(textViewBar);
            if (chatsImage.getParent() != null)
                ((ViewGroup) chatsImage.getParent()).removeView(chatsImage);
            rosterBarLayout.addView(chatsImage);
            barLinearLayout.addView(rosterBarLayout);
            ChatFragment chatFragment = (ChatFragment) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatFragment.removeTitleBar();
            barLinearLayout.addView(chatFragment.getTitleBar());
            actionBar.setCustomView(barLinearLayout);
        } else {
            if (textViewBar.getParent() != null)
                ((ViewGroup) textViewBar.getParent()).removeView(textViewBar);
            barLinearLayout.addView(textViewBar);
            if (chatsImage.getParent() != null)
                ((ViewGroup) chatsImage.getParent()).removeView(chatsImage);
            barLinearLayout.addView(chatsImage);
            actionBar.setCustomView(barLinearLayout);
        }
        barLinearLayout.setLayoutParams(barLinearLayoutLP);
        chatsImage.setLayoutParams(chatsImageLP);
    }

    @Override
    public void onPause() {
        super.onPause();
        RosterHelper.getInstance().setOnUpdateRoster(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        initBar();
        getRosterAdapter().setType(RosterHelper.ACTIVE_CONTACTS);
        RosterHelper.getInstance().setOnUpdateRoster(RosterFragment.this);
        update();
        getActivity().supportInvalidateOptionsMenu();
        if (Scheme.isChangeTheme(Scheme.getSavedTheme())) {
            ((SawimActivity) getActivity()).recreateActivity();
        }
        if (Options.getAccountCount() > 0) {
            update();
            getActivity().supportInvalidateOptionsMenu();
            if (SawimApplication.returnFromAcc) {
                SawimApplication.returnFromAcc = false;
                //Toast.makeText(getActivity(), R.string.press_menu_for_connect, Toast.LENGTH_LONG).show();
                if (getRosterAdapter().getItemCount() == 0) {
                    if (SawimApplication.isManyPane())
                        getActivity().setContentView(R.layout.main);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new SearchContactFragment(), SearchContactFragment.TAG)
                            .addToBackStack(null)
                            .commit();
                }
            }
        } else {
            if (!SawimApplication.returnFromAcc && !SawimApplication.isManyPane()) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                StartWindowFragment newFragment = new StartWindowFragment();
                transaction.replace(R.id.fragment_container, newFragment, StartWindowFragment.TAG);
                transaction.addToBackStack(null);
                transaction.commit();
                getActivity().supportInvalidateOptionsMenu();
            }
        }
    }

    public RosterAdapter getRosterAdapter() {
        if (rosterViewLayout == null || rosterViewLayout.getMyListView() == null) return null;
        return (RosterAdapter) getListView().getAdapter();
    }

    public MyListView getListView() {
        return rosterViewLayout.getMyListView();
    }

    private void openChat(Contact c, String sharingText) {
        c.activate((BaseActivity) getActivity());
        if (SawimApplication.isManyPane()) {
            ChatFragment chatFragmentTablet = (ChatFragment) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatFragmentTablet.pause(chatFragmentTablet.getCurrentChat());
            chatFragmentTablet.openChat(c);
            chatFragmentTablet.setSharingText(sharingText);
            chatFragmentTablet.resume(chatFragmentTablet.getCurrentChat());
            update();
        } else {
            ChatFragment chatFragment = ChatFragment.newInstance();
            chatFragment.setSharingText(sharingText);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, chatFragment, ChatFragment.TAG);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void sharing(Protocol p, Contact c) {
        Intent intent = getActivity().getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        String type = intent.getType();
        if (type.equals("text/plain")) {
            String subjectText = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String sharingText = intent.getStringExtra(Intent.EXTRA_TEXT);
            openChat(c, subjectText == null ? sharingText : subjectText + "\n" + sharingText);
        } else {
            Uri data = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (data == null) return;
            FileTransfer fileTransfer = new FileTransfer(p, c);
            fileTransfer.onFileSelect((BaseActivity) getActivity(), data);
            Toast.makeText(getActivity(), R.string.sending_file, Toast.LENGTH_LONG).show();
        }
        setMode(MODE_DEFAULT);
        getActivity().setIntent(null);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MyListView.RecyclerContextMenuInfo contextMenuInfo = (MyListView.RecyclerContextMenuInfo) menuInfo;
        TreeNode treeNode = getRosterAdapter().getItem(contextMenuInfo.position);
        if (treeNode.getType() == TreeNode.PROTOCOL) {
            RosterHelper.getInstance().showProtocolMenu((BaseActivity) getActivity(), ((ProtocolBranch) treeNode).getProtocol());
        } else if (treeNode.getType() == TreeNode.GROUP) {
            Protocol p = RosterHelper.getInstance().getProtocol();
            if (p.isConnected()) {
                new ManageContactListForm(p, (Group) treeNode).showMenu((BaseActivity) getActivity());
            }
        } else if (treeNode.getType() == TreeNode.CONTACT) {
            new ContactMenu((Contact) treeNode).getContextMenu(menu);
        }
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        MyListView.RecyclerContextMenuInfo menuInfo = (MyListView.RecyclerContextMenuInfo) item.getMenuInfo();
        TreeNode treeNode = getRosterAdapter().getItem(menuInfo.position);
        if (treeNode == null) return false;
        if (treeNode.getType() == TreeNode.CONTACT) {
            contactMenuItemSelected((Contact) treeNode, item);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void contactMenuItemSelected(final Contact c, final android.view.MenuItem item) {
        new ContactMenu(c).doAction((BaseActivity) getActivity(), item.getItemId());
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    @Override
    public boolean hasBack() {
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.find_conference_button:
                if (SawimApplication.isManyPane())
                    getActivity().setContentView(R.layout.main);
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SearchConferenceFragment(), SearchConferenceFragment.TAG)
                        .addToBackStack(null)
                        .commit();
                return;

            case R.id.manage_contact_list_button:
                new ManageContactListForm(RosterHelper.getInstance().getProtocol(0)).showMenu((BaseActivity) getActivity());
                return;
        }
        if (chatsImage == v) {
            Chat current = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
            if (current == null) return;
            if (0 < current.getAuthRequestCounter()) {
                ChatFragment.showAuthorizationDialog((BaseActivity) getActivity(), current);
            } else {
                openChat(current.getContact(), null);
                update();
            }
            return;
        }
        if (rosterViewLayout.getFab() == v) {
            if (SawimApplication.isManyPane())
                getActivity().setContentView(R.layout.main);
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SearchContactFragment(), SearchContactFragment.TAG)
                    .addToBackStack(null)
                    .commit();
            return;
        }

    }
    @Override
    public void onItemClick(View view, int position) {
        TreeNode treeNode = getRosterAdapter().getItem(position);
        if (treeNode == null) return;
        if (getMode() == MODE_SHARE || getMode() == MODE_SHARE_TEXT) {
            if (treeNode.getType() == TreeNode.CONTACT) {
                sharing(((Contact) treeNode).getProtocol(), (Contact) treeNode);
            }
        } else {
            if (treeNode.getType() == TreeNode.CONTACT) {
                openChat(((Contact) treeNode), null);
            }
        }
        if (treeNode.getType() == TreeNode.GROUP) {
            Group group = RosterHelper.getInstance().getGroupWithContacts((Group) treeNode);
            group.setExpandFlag(!group.isExpanded());
            update();
        }
    }

    private void initSwipe(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                TreeNode treeNode = getRosterAdapter().getItem(position);
                if (treeNode.getType() == TreeNode.CONTACT) {
                    Protocol protocol = RosterHelper.getInstance().getProtocol();
                    Contact contact = (Contact) treeNode;
                    protocol.getChat(contact).getHistory().removeHistory();
                    ChatHistory.instance.unregisterChat(protocol.getChat(contact));
                    RosterHelper.getInstance().updateRoster();
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;

                    if(dX > 0){
                    } else {
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }
}