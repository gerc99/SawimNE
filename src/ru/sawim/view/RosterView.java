package ru.sawim.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.*;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.widget.*;

import ru.sawim.activities.SawimActivity;
import ru.sawim.listener.OnAccountsLoaded;
import ru.sawim.listener.OnUpdateRoster;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.*;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.models.RosterAdapter;
import ru.sawim.modules.FileTransfer;
import ru.sawim.roster.ProtocolBranch;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeNode;
import ru.sawim.widget.MyImageButton;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;
import ru.sawim.widget.roster.RosterViewRoot;


/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 01.04.13
 * Time: 19:58
 * To change this template use File | Settings | File Templates.
 */
public class RosterView extends SawimFragment implements View.OnClickListener, OnUpdateRoster, Handler.Callback, OnAccountsLoaded {

    public static final String TAG = RosterView.class.getSimpleName();

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

        MyListView listView = (MyListView) LayoutInflater.from(getContext()).inflate(R.layout.recycler_view, null);
        RosterAdapter rosterAdapter = new RosterAdapter();
        listView.setAdapter(rosterAdapter);
        registerForContextMenu(listView);
        rosterAdapter.setOnItemClickListener(this);
        FrameLayout.LayoutParams listViewLP = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        listViewLP.gravity = Gravity.BOTTOM;
        listView.setLayoutParams(listViewLP);

        FloatingActionButton fab = new FloatingActionButton(getActivity());
        fab.setImageResource(R.drawable.ic_pencil_white_24dp);
        fab.setOnClickListener(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.bottomMargin = Util.dipToPixels(SawimApplication.getContext(), 15);
        lp.rightMargin = Util.dipToPixels(SawimApplication.getContext(), 15);
        fab.setLayoutParams(lp);

        rosterViewLayout = new RosterViewRoot(activity, progressBar, listView, fab);

        RosterHelper.getInstance().setOnAccountsLoaded(this);
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
        unregisterForContextMenu(rosterViewLayout.getMyListView());
        getRosterAdapter().setOnItemClickListener(null);
        rosterViewLayout.getMyListView().setAdapter(null);
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
        final Protocol p = RosterHelper.getInstance().getProtocol(0);
        if (RosterHelper.getInstance().getProtocolCount() == 1 && p != null) {
            byte percent = p.getConnectingProgress();
            if (oldProgressBarPercent != percent) {
                oldProgressBarPercent = percent;
                BaseActivity activity = (BaseActivity) getActivity();
                if (100 != percent) {
                    rosterViewLayout.getProgressBar().setVisibility(ProgressBar.VISIBLE);
                    rosterViewLayout.getProgressBar().setProgress(percent);
                } else {
                    rosterViewLayout.getProgressBar().setVisibility(ProgressBar.GONE);
                }
                if (100 == percent || 0 == percent) {
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
            icMess = icMess.mutate();
            if (icMess == SawimResources.PERSONAL_MESSAGE_ICON) {
                icMess.setColorFilter(Scheme.getColor(R.attr.bar_personal_unread_message), PorterDuff.Mode.MULTIPLY);
            } else {
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
            ChatView chatView = (ChatView) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatView.removeTitleBar();
            barLinearLayout.addView(chatView.getTitleBar());
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
        RosterHelper.getInstance().setOnUpdateRoster(RosterView.this);
        update();
        if (getRosterAdapter().getItemCount() == 0) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SearchContactFragment(), SearchContactFragment.TAG)
                    .addToBackStack(null)
                    .commit();
        }
        getActivity().supportInvalidateOptionsMenu();
        if (Scheme.isChangeTheme(Scheme.getSavedTheme())) {
            ((SawimActivity) getActivity()).recreateActivity();
        }
    }

    @Override
    public void onAccountsLoaded() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                //initBar();
                if (Options.getAccountCount() > 0) {
                    update();

                    if (getRosterAdapter().getItemCount() == 0) {
                        getFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new SearchContactFragment(), SearchContactFragment.TAG)
                                .addToBackStack(null)
                                .commit();
                    }

                    getActivity().supportInvalidateOptionsMenu();
                    if (SawimApplication.returnFromAcc) {
                        SawimApplication.returnFromAcc = false;
                        if (getRosterAdapter().getItemCount() == 0)
                            Toast.makeText(getActivity(), R.string.press_menu_for_connect, Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (!SawimApplication.isManyPane()) {
                        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                        FragmentTransaction transaction = fragmentManager.beginTransaction();
                        StartWindowView newFragment = new StartWindowView();
                        transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
                        transaction.addToBackStack(null);
                        transaction.commit();
                        getActivity().supportInvalidateOptionsMenu();
                    }
                }
            }
        });
    }

    public RosterAdapter getRosterAdapter() {
        if (rosterViewLayout.getMyListView() == null) return null;
        return (RosterAdapter) getListView().getAdapter();
    }

    public RecyclerView getListView() {
        return rosterViewLayout.getMyListView();
    }

    private void openChat(Protocol p, Contact c, String sharingText) {
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

    private void sharing(Protocol p, Contact c) {
        Intent intent = getActivity().getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        String type = intent.getType();
        if (type.equals("text/plain")) {
            String subjectText = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String sharingText = intent.getStringExtra(Intent.EXTRA_TEXT);
            openChat(p, c, subjectText == null ? sharingText : subjectText + "\n" + sharingText);
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
        if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
            if (treeNode.getType() == TreeNode.CONTACT) {
                Contact contact = (Contact) treeNode;
                new ContactMenu(contact.getProtocol(), contact).getContextMenu(menu);
            }
        } else {
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
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        MyListView.RecyclerContextMenuInfo menuInfo = (MyListView.RecyclerContextMenuInfo) item.getMenuInfo();
        TreeNode treeNode = getRosterAdapter().getItem(menuInfo.position);
        if (treeNode == null) return false;
        if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
            if (treeNode.getType() == TreeNode.CONTACT) {
                Contact contact = (Contact) treeNode;
                contactMenuItemSelected(contact, item);
            }
        } else {
            if (treeNode.getType() == TreeNode.CONTACT) {
                contactMenuItemSelected((Contact) treeNode, item);
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    private void contactMenuItemSelected(final Contact c, final android.view.MenuItem item) {
        new ContactMenu(c.getProtocol(), c).doAction((BaseActivity) getActivity(), item.getItemId());
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
        if (chatsImage == v) {
            Chat current = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
            if (current == null) return;
            if (0 < current.getAuthRequestCounter()) {
                ChatView.showAuthorizationDialog((BaseActivity) getActivity(), current);
            } else {
                openChat(current.getProtocol(), current.getContact(), null);
                update();
            }
            return;
        } else if (rosterViewLayout.getFab() == v) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SearchContactFragment(), SearchContactFragment.TAG)
                    .addToBackStack(null)
                    .commit();
            return;
        }
        int position = (int) v.getTag();
        TreeNode treeNode = getRosterAdapter().getItem(position);
        if (getMode() == MODE_SHARE || getMode() == MODE_SHARE_TEXT) {
            if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
                if (treeNode.getType() == TreeNode.CONTACT) {
                    Contact contact = (Contact) treeNode;
                    sharing(contact.getProtocol(), contact);
                }
            } else {
                if (treeNode.getType() == TreeNode.CONTACT) {
                    sharing(((Contact) treeNode).getProtocol(), (Contact) treeNode);
                }
            }
        } else {
            if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
                if (treeNode.getType() == TreeNode.CONTACT) {
                    Contact contact = (Contact) treeNode;
                    openChat(contact.getProtocol(), contact, null);
                }
            } else {
                if (treeNode.getType() == TreeNode.CONTACT) {
                    openChat(((Contact) treeNode).getProtocol(), ((Contact) treeNode), null);
                }
            }
        }
        if (RosterHelper.getInstance().getCurrPage() != RosterHelper.ACTIVE_CONTACTS) {
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
    }
}
