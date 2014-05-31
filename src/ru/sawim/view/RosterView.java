package ru.sawim.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.*;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.*;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.comm.JLocale;
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.models.RosterAdapter;
import ru.sawim.models.RosterModsAdapter;
import ru.sawim.modules.FileTransfer;
import ru.sawim.roster.ProtocolBranch;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeBranch;
import ru.sawim.roster.TreeNode;
import ru.sawim.widget.MyImageButton;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.MySpinner;
import ru.sawim.widget.Util;
import ru.sawim.widget.roster.RosterViewRoot;

import java.io.FileNotFoundException;
import java.io.InputStream;


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

    public static final int MODE_DEFAULT = 0;
    public static final int MODE_SHARE = 1;
    public static final int MODE_SHARE_TEXT = 2;
    private int mode;

    private LinearLayout rosterBarLayout;
    private LinearLayout barLinearLayout;
    private RosterViewRoot rosterViewLayout;
    private MyListView rosterListView;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;
    private Handler handler;
    private RosterModsAdapter rosterModsAdapter;
    private MySpinner rosterModsSpinner;
    private MyImageButton chatsImage;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        handler = new Handler(this);
        rosterBarLayout = new LinearLayout(activity);
        barLinearLayout = new LinearLayout(activity);
        chatsImage = new MyImageButton(activity);

        rosterListView = new MyListView(activity);
        RosterAdapter rosterAdapter = new RosterAdapter();
        rosterAdapter.setType(RosterHelper.getInstance().getCurrPage());
        LinearLayout.LayoutParams rosterListViewLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rosterListView.setLayoutParams(rosterListViewLayoutParams);
        rosterListView.setAdapter(rosterAdapter);
        activity.registerForContextMenu(rosterListView);
        rosterListView.setOnCreateContextMenuListener(this);
        rosterListView.setOnItemClickListener(this);
        rosterViewLayout = new RosterViewRoot(activity, rosterListView);

        rosterModsSpinner = new MySpinner(activity, null, Build.VERSION.SDK_INT < 11
                ? R.attr.actionDropDownStyle : android.R.attr.actionDropDownStyle);
        rosterModsAdapter = new RosterModsAdapter();
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
        contextMenuInfo = null;
        handler = null;
        rosterModsAdapter = null;
        rosterModsSpinner = null;
        rosterViewLayout = null;
        rosterListView = null;
        rosterBarLayout = null;
        barLinearLayout = null;
        chatsImage = null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE_BAR_PROTOCOLS:
                final int protocolCount = RosterHelper.getInstance().getProtocolCount();
                if (protocolCount > 1) {
                    if (rosterModsAdapter != null)
                        rosterModsAdapter.notifyDataSetChanged();
                }
                break;
            case UPDATE_PROGRESS_BAR:
                final Protocol p = RosterHelper.getInstance().getProtocol(0);
                if (RosterHelper.getInstance().getProtocolCount() == 1 && p != null) {
                    BaseActivity activity = (BaseActivity) getActivity();
                    byte percent = p.getConnectingProgress();
                    activity.setSupportProgress(percent * 100);
                    if (100 == percent || 0 == percent) {
                        activity.supportInvalidateOptionsMenu();
                    }
                }
                break;
            case UPDATE_ROSTER:
                RosterHelper.getInstance().updateOptions();
                if (getRosterAdapter() != null) {
                    getRosterAdapter().refreshList();
                }
                if (rosterModsAdapter != null)
                    rosterModsAdapter.notifyDataSetChanged();
                updateChatImage();
                break;
            case PUT_INTO_QUEUE:
                if (getRosterAdapter() != null) {
                    getRosterAdapter().putIntoQueue((Group) msg.obj);
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

    private void updateChatImage() {
        if (chatsImage == null) return;
        Drawable icMess = ChatHistory.instance.getUnreadMessageIcon();
        if (icMess == null) {
            chatsImage.setVisibility(View.GONE);
        } else {
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
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowCustomEnabled(true);
        if (RosterHelper.getInstance().getProtocolCount() > 0) {
            rosterModsSpinner.setAdapter(rosterModsAdapter);
            rosterModsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int itemPosition, long id) {
                    if (itemPosition != RosterHelper.getInstance().getCurrPage()) {
                        getRosterAdapter().setType(itemPosition);
                        RosterHelper.getInstance().setCurrPage(itemPosition);
                        update();
                        getActivity().supportInvalidateOptionsMenu();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        chatsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Chat current = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
                if (current == null) return;
                if (0 < current.getAuthRequestCounter()) {
                    new DialogFragment() {
                        @Override
                        public Dialog onCreateDialog(Bundle savedInstanceState) {
                            final Context context = getActivity();
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                            dialogBuilder.setInverseBackgroundForced(Util.isNeedToInverseDialogBackground());
                            dialogBuilder.setMessage(JLocale.getString(R.string.grant) + " " + current.getContact().getName() + "?");
                            dialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new ContactMenu(current.getProtocol(), current.getContact()).doAction((BaseActivity) getActivity(), ContactMenu.USER_MENU_GRANT_AUTH);
                                    getActivity().supportInvalidateOptionsMenu();
                                    updateRoster();
                                }
                            });
                            dialogBuilder.setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new ContactMenu(current.getProtocol(), current.getContact()).doAction((BaseActivity) getActivity(), ContactMenu.USER_MENU_DENY_AUTH);
                                    getActivity().supportInvalidateOptionsMenu();
                                    updateRoster();
                                }
                            });
                            Dialog dialog = dialogBuilder.create();
                            dialog.setCanceledOnTouchOutside(true);
                            return dialog;
                        }
                    }.show(getFragmentManager().beginTransaction(), "auth");
                } else {
                    openChat(current.getProtocol(), current.getContact(), null);
                    getActivity().supportInvalidateOptionsMenu();
                    updateRoster();
                }
            }
        });
        LinearLayout.LayoutParams barLinearLayoutLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams spinnerLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams chatsImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        chatsImageLP.gravity = Gravity.RIGHT;
        chatsImageLP.weight = 4;
        spinnerLP.weight = 1;
        barLinearLayout.removeAllViews();
        if (SawimApplication.isManyPane()) {
            LinearLayout.LayoutParams rosterBarLayoutLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            rosterBarLayoutLP.weight = 2;
            rosterBarLayout.setLayoutParams(rosterBarLayoutLP);
            rosterBarLayout.removeAllViews();
            if (RosterHelper.getInstance().getProtocolCount() > 0)
                rosterBarLayout.addView(rosterModsSpinner);
            rosterBarLayout.addView(chatsImage);
            barLinearLayout.addView(rosterBarLayout);
            ChatView chatView = (ChatView) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatView.removeTitleBar();
            barLinearLayout.addView(chatView.getTitleBar());
            actionBar.setCustomView(barLinearLayout);
        } else {
            if (RosterHelper.getInstance().getProtocolCount() > 0)
                barLinearLayout.addView(rosterModsSpinner);
            barLinearLayout.addView(chatsImage);
            actionBar.setCustomView(barLinearLayout);
        }
        barLinearLayout.setLayoutParams(barLinearLayoutLP);
        rosterModsSpinner.setLayoutParams(spinnerLP);
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
        resume();
        if (!SawimApplication.isManyPane() && Scheme.isChangeTheme(Scheme.getThemeId(Options.getString(Options.OPTION_COLOR_SCHEME)))) {
            ((SawimActivity) getActivity()).recreateActivity();
        }
    }

    public void resume() {
        initBar();
        getRosterAdapter().setType(RosterHelper.getInstance().getCurrPage());
        rosterModsSpinner.setSelection(RosterHelper.getInstance().getCurrPage());

        if (RosterHelper.getInstance().getProtocolCount() > 0) {
            if (!SawimApplication.isManyPane())
                RosterHelper.getInstance().setCurrentContact(null);
            RosterHelper.getInstance().setOnUpdateRoster(this);
            if (SawimApplication.returnFromAcc) {
                SawimApplication.returnFromAcc = false;
                if (getRosterAdapter().getCount() == 0)
                    Toast.makeText(getActivity(), R.string.press_menu_for_connect, Toast.LENGTH_LONG).show();
                if (rosterModsAdapter != null)
                    rosterModsAdapter.notifyDataSetChanged();
            }
            update();
            getActivity().supportInvalidateOptionsMenu();
        } else {
            if (!SawimApplication.isManyPane()) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                StartWindowView newFragment = new StartWindowView();
                transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
                transaction.commit();
                getActivity().supportInvalidateOptionsMenu();
            }
        }
    }

    public RosterAdapter getRosterAdapter() {
        return (RosterAdapter) rosterListView.getAdapter();
    }

    private void openChat(Protocol p, Contact c, String sharingText) {
        if (!SawimApplication.isManyPane()) {
            ChatView chatView = new ChatView();
            chatView.initChat(p, c);
            chatView.setSharingText(sharingText);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, chatView, ChatView.TAG);
            transaction.addToBackStack(null);
            transaction.commit();
        } else {
            ChatView chatViewTablet = (ChatView) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatViewTablet.pause(chatViewTablet.getCurrentChat());
            if (c != null) {
                chatViewTablet.openChat(p, c);
                chatViewTablet.setSharingText(sharingText);
                chatViewTablet.resume(chatViewTablet.getCurrentChat());
            }
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
            try {
                InputStream is = getActivity().getContentResolver().openInputStream(data);
                FileTransfer fileTransfer = new FileTransfer(p, c);
                fileTransfer.onFileSelect((BaseActivity) getActivity(), is, ExternalApi.getFileName(data, getActivity()));
            } catch (FileNotFoundException e) {
            }
            Toast.makeText(getActivity(), R.string.sending_file, Toast.LENGTH_LONG).show();
        }
        setMode(MODE_DEFAULT);
        getActivity().setIntent(null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (getMode() == MODE_SHARE || getMode() == MODE_SHARE_TEXT) {
            if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
                Object o = getRosterAdapter().getItem(position);
                if (o instanceof Chat) {
                    Chat chat = (Chat) o;
                    sharing(chat.getProtocol(), chat.getContact());
                }
            } else {
                TreeNode item = (TreeNode) getRosterAdapter().getItem(position);
                if (item.getType() == TreeNode.CONTACT) {
                    sharing(((Contact) item).getProtocol(), (Contact) item);
                }
            }
        } else {
            if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
                Object o = getRosterAdapter().getItem(position);
                if (o instanceof Chat) {
                    Chat chat = (Chat) o;
                    openChat(chat.getProtocol(), chat.getContact(), null);
                    if (SawimApplication.isManyPane())
                        update();
                }
            } else {
                TreeNode item = (TreeNode) getRosterAdapter().getItem(position);
                if (item.getType() == TreeNode.CONTACT) {
                    openChat(((Contact) item).getProtocol(), ((Contact) item), null);
                    if (SawimApplication.isManyPane())
                        update();
                }
            }
        }
        if (RosterHelper.getInstance().getCurrPage() != RosterHelper.ACTIVE_CONTACTS) {
            TreeNode item = (TreeNode) getRosterAdapter().getItem(position);
            if (item.getType() == TreeNode.PROTOCOL || item.getType() == TreeNode.GROUP) {
                TreeBranch group = (TreeBranch) item;
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
            if (node.getType() == TreeNode.PROTOCOL) {
                RosterHelper.getInstance().showProtocolMenu((BaseActivity) getActivity(), ((ProtocolBranch) node).getProtocol());
            } else if (node.getType() == TreeNode.GROUP) {
                Protocol p = RosterHelper.getInstance().getProtocol((Group) node);
                if (p.isConnected()) {
                    new ManageContactListForm(p, (Group) node).showMenu((BaseActivity) getActivity());
                }
            } else if (node.getType() == TreeNode.CONTACT) {
                new ContactMenu(((Contact) node).getProtocol(), (Contact) node).getContextMenu(menu);
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
            if (node.getType() == TreeNode.CONTACT) {
                contactMenuItemSelected((Contact) node, item);
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
}
