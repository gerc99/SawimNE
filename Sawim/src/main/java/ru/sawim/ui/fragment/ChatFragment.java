package ru.sawim.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import io.realm.Realm;
import io.realm.RealmResults;
import protocol.xmpp.*;
import ru.sawim.db.RealmDb;
import ru.sawim.io.RosterStorage;
import ru.sawim.listener.OnMoreMessagesLoaded;
import ru.sawim.listener.OnUpdateChat;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.StatusInfo;
import ru.sawim.*;
import ru.sawim.ui.activity.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.ui.adapter.MessagesAdapter;
import ru.sawim.ui.adapter.MucUsersAdapter;
import ru.sawim.ui.adapter.RosterAdapter;
import ru.sawim.roster.RosterHelper;
import ru.sawim.text.TextFormatter;
import ru.sawim.ui.dialog.ContactAuthDialogFragment;
import ru.sawim.ui.fragment.menu.JuickMenu;
import ru.sawim.ui.fragment.menu.MyMenu;
import ru.sawim.ui.fragment.menu.MyMenuItem;
import ru.sawim.ui.widget.FixedEditText;
import ru.sawim.ui.widget.HeaderDecoration;
import ru.sawim.ui.widget.MyDrawerLayout;
import ru.sawim.ui.widget.MyImageButton;
import ru.sawim.ui.widget.MyListView;
import ru.sawim.ui.widget.Util;
import ru.sawim.ui.widget.chat.ChatBarView;
import ru.sawim.ui.widget.chat.ChatInputBarView;
import ru.sawim.ui.widget.chat.ChatListsView;
import ru.sawim.ui.widget.chat.ChatViewRoot;
import ru.sawim.ui.widget.chat.SmileysPopup;
import ru.sawim.ui.widget.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.01.13
 * Time: 20:30
 * To change this template use File | Settings | File Templates.
 */
public class ChatFragment extends SawimFragment implements OnUpdateChat, Handler.Callback,
        ChatsDialogFragment.OnForceGoToChatListener,
        View.OnClickListener, MessagesAdapter.OnItemClickListener,
        MessagesAdapter.OnLoadItemsListener, BaseActivity.OnConfigurationChanged,
        TextView.OnEditorActionListener, View.OnKeyListener, View.OnLongClickListener {

    public static final String TAG = ChatFragment.class.getSimpleName();

    private static final int HISTORY_MESSAGES_LIMIT = 20;
    private static final String CITATION_DIVIDER = "\n---\n";

    private static final int ADD_MESSAGE = 0;
    private static final int UPDATE_MESSAGES = 1;
    private static final int UPDATE_MESSAGE = 2;
    private Handler handler;

    private Chat chat;
    private int oldChatHash;
    private boolean isOldChat;
    private int unreadMessageCount;
    private String sharingText;
    private boolean sendByEnter;
    private int newMessageCount = 0;

    private String oldSearchQuery = "";
    private boolean isSearchMode;
    private int searchMessagePositionsCount;
    private List<Integer> searchMessagePositions;
    Realm realmDb = RealmDb.realm();

    private StickyRecyclerHeadersDecoration stickyRecyclerHeadersDecoration;
    private MyListView nickList;
    private ChatViewRoot chatViewLayout;
    private SmileysPopup smileysPopup;
    private MucUsersFragment mucUsersFragment;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private ChatBarView chatBarLayout;
    private ChatsDialogFragment chatsDialogFragment;
    private int lastServerMessageCount;

    public static ChatFragment newInstance() {
        ChatFragment chatFragment = new ChatFragment();
        return chatFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        handler = new Handler(this);
        Activity activity = getActivity();
        MyImageButton chatsImage = new MyImageButton(activity);
        MyImageButton menuButton = new MyImageButton(activity);
        MyImageButton smileButton = new MyImageButton(activity);
        MyImageButton sendButton = new MyImageButton(activity);
        FixedEditText messageEditor = new FixedEditText(activity);
        chatBarLayout = new ChatBarView(activity, chatsImage);
        chatBarLayout.setOnClickListener(this);
        MyListView chatListView = (MyListView) LayoutInflater.from(context).inflate(R.layout.recycler_view, null);
        nickList = (MyListView) LayoutInflater.from(context).inflate(R.layout.recycler_view, null);
        ChatListsView chatListsView = new ChatListsView(activity, SawimApplication.isManyPane(), chatListView, nickList);
        ChatInputBarView chatInputBarView = new ChatInputBarView(activity, menuButton, smileButton, messageEditor, sendButton);
        chatViewLayout = new ChatViewRoot(activity, chatListsView, chatInputBarView);
        smileysPopup = new SmileysPopup((BaseActivity) activity, chatViewLayout);
        drawerLayout = new MyDrawerLayout(activity);
        Util.setDrawerLayoutSensitivity(drawerLayout);

        ((BaseActivity) activity).setConfigurationChanged(this);
    }

    public void removeTitleBar() {
        if (chatBarLayout != null && chatBarLayout.getParent() != null)
            ((ViewGroup) chatBarLayout.getParent()).removeView(chatBarLayout);
    }

    public ChatBarView getTitleBar() {
        return chatBarLayout;
    }

    public ActionBarDrawerToggle getDrawerToggle() {
        return drawerToggle;
    }

    private void resetBar() {
        if (!SawimApplication.isManyPane()) {
            ActionBar actionBar = ((BaseActivity) getActivity()).getSupportActionBar();
            drawerToggle.setDrawerIndicatorEnabled(true);
            removeTitleBar();
            actionBar.setDisplayShowTitleEnabled(false);

            if (chat != null) {
                Contact contact = chat.getContact();
                actionBar.setDisplayShowHomeEnabled(contact.isConference());
                actionBar.setDisplayUseLogoEnabled(contact.isConference());
                actionBar.setDisplayHomeAsUpEnabled(contact.isConference());
                actionBar.setHomeButtonEnabled(contact.isConference());
            }
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(chatBarLayout);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceStateLog) {
        final BaseActivity activity = (BaseActivity) getActivity();
        MyImageButton menuButton = chatViewLayout.getChatInputBarView().getMenuButton();
        MyImageButton smileButton = chatViewLayout.getChatInputBarView().getSmileButton();
        MyImageButton sendButton = chatViewLayout.getChatInputBarView().getSendButton();
        FixedEditText messageEditor = chatViewLayout.getChatInputBarView().getMessageEditor();
        updateChatIcon();
        if (drawerLayout != null && drawerLayout.getParent() != null) {
            ((ViewGroup) drawerLayout.getParent()).removeView(drawerLayout);
        }
        if (chatViewLayout.getParent() != null) {
            ((ViewGroup) chatViewLayout.getParent()).removeView(chatViewLayout);
        }

        chatBarLayout.update();
        chatViewLayout.update();
        chatViewLayout.getChatListsView().update();
        chatViewLayout.getChatInputBarView().setImageButtons(menuButton, smileButton, sendButton);
        if (!SawimApplication.isManyPane()) {
            DrawerLayout.LayoutParams nickListLP = new DrawerLayout.LayoutParams(Util.dipToPixels(activity, 240), DrawerLayout.LayoutParams.MATCH_PARENT);
            DrawerLayout.LayoutParams drawerLayoutLP = new DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT);
            drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            drawerLayout.setScrimColor(Scheme.isBlack() ? 0x55FFFFFF : 0x99000000);
            nickListLP.gravity = Gravity.START;
            drawerLayout.setLayoutParams(drawerLayoutLP);
            drawerLayout.addView(chatViewLayout);

            if (nickList.getParent() != null) {
                ((ViewGroup) nickList.getParent()).removeView(nickList);
            }
            drawerLayout.addView(nickList);
            nickList.setBackgroundResource(Util.getSystemBackground(activity));
            nickList.setLayoutParams(nickListLP);
            drawerToggle = new ActionBarDrawerToggle(activity, drawerLayout, 0, 0) {
                public void onDrawerClosed(View view) {
                }

                public void onDrawerOpened(View view) {
                    if (mucUsersFragment != null)
                        mucUsersFragment.update(getMucUsersAdapter());
                }
            };
            drawerLayout.setDrawerListener(drawerToggle);
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerToggle.syncState();
        }
        if (!SawimApplication.isManyPane() && chatBarLayout.getChatsImage() != null) {
            chatBarLayout.getChatsImage().setOnClickListener(this);
        }
        if (SawimApplication.isManyPane()) {
            menuButton.setVisibility(ImageButton.VISIBLE);
            menuButton.setOnClickListener(this);
        } else
            menuButton.setVisibility(ImageButton.GONE);
        smileButton.setOnClickListener(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            messageEditor.setTextColor(Scheme.getColor(R.attr.text));
        }
        sendByEnter = Options.getBoolean(JLocale.getString(R.string.pref_simple_input));
        if (sendByEnter) {
            messageEditor.setImeOptions(EditorInfo.IME_ACTION_SEND);
        } else {
            messageEditor.setImeOptions(EditorInfo.IME_ACTION_NONE);
        }
        messageEditor.setSingleLine(false);
        messageEditor.setMaxLines(4);
        messageEditor.setHorizontallyScrolling(false);
        messageEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageEditor.setHint(R.string.hint_message);
        messageEditor.addTextChangedListener(textWatcher);
        messageEditor.setOnClickListener(this);
        messageEditor.setOnEditorActionListener(this);
        messageEditor.setOnKeyListener(this);
        if (sendByEnter) {
            sendButton.setVisibility(ImageButton.GONE);
        } else {
            sendButton.setVisibility(ImageButton.VISIBLE);
            sendButton.setOnClickListener(this);
            sendButton.setOnLongClickListener(this);
        }
        destroySearchMode();
        return SawimApplication.isManyPane() ? chatViewLayout : drawerLayout;
    }

    @Override
    public void onClick(View v) {
        if (v == chatBarLayout.getChatsImage()) {
            final Chat newChat = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
            if (newChat == null) return;
            if (0 < newChat.getAuthRequestCounter()) {
                showAuthorizationDialog((BaseActivity) getActivity(), newChat);
            } else {
                forceGoToChat(newChat);
            }
        } else if (v == chatViewLayout.getChatInputBarView().getMenuButton()) {
            if (chat == null) return;
            final MyMenu menu = getMenu();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setCancelable(true);
            builder.setTitle(null);
            builder.setAdapter(menu, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onOptionsItemSelected(menu.getItem(which).idItem);
                }
            });
            builder.create().show();
        } else if (v == chatViewLayout.getChatInputBarView().getMessageEditor()) {
            smileysPopup.hide(chatViewLayout);
        } else if (v == chatViewLayout.getChatInputBarView().getSmileButton()) {
            smileysPopup.show((BaseActivity) getActivity(), chatViewLayout);
        } else if (v == chatBarLayout) {
            chatsDialogFragment = new ChatsDialogFragment();
            chatsDialogFragment.setForceGoToChatListener(this);
            chatsDialogFragment.show(getFragmentManager(), "force-go-to-chat");
        } else if (v == chatViewLayout.getChatInputBarView().getSendButton()) {
            send();
            closePane();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
            drawerLayout.setDrawerListener(null);
            drawerToggle = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((BaseActivity) getActivity()).setConfigurationChanged(null);
        if (chat != null) {
            if (getMessagesAdapter().getItemCount() == 0) {
                ChatHistory.instance.unregisterChat(chat);
            }
            getMessagesAdapter().setOnLoadItemsListener(null);
            getMessagesAdapter().setOnItemClickListener(null);
            chat = null;
        }
        chatBarLayout.setOnClickListener(null);
        MyImageButton menuButton = chatViewLayout.getChatInputBarView().getMenuButton();
        MyImageButton smileButton = chatViewLayout.getChatInputBarView().getSmileButton();
        MyImageButton sendButton = chatViewLayout.getChatInputBarView().getSendButton();
        FixedEditText messageEditor = chatViewLayout.getChatInputBarView().getMessageEditor();
        menuButton.setOnClickListener(null);
        smileButton.setOnClickListener(null);
        sendButton.setOnClickListener(null);
        sendButton.setOnLongClickListener(null);
        messageEditor.removeTextChangedListener(textWatcher);
        messageEditor.setOnClickListener(null);
        messageEditor.setOnEditorActionListener(null);
        messageEditor.setOnKeyListener(null);
        MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
        chatListView.clearOnScrollListeners();
        unregisterForContextMenu(chatListView);
        unregisterForContextMenu(nickList);
        nickList.addOnItemTouchListener(null);
        nickList.setOnTouchListener(null);

        realmDb.close();
    }

    public boolean hasBack() {
        if (isSearchMode) {
            destroySearchMode();
            return false;
        }
        return !closePane();
    }

    private boolean closePane() {
        if (!SawimApplication.isManyPane()) {
            if (drawerLayout.isDrawerOpen(nickList)) {
                drawerLayout.closeDrawer(nickList);
                return true;
            }
        }
        if (smileysPopup != null) {
            return smileysPopup.hide(chatViewLayout);
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (chat != null) {
            RosterHelper.getInstance().setCurrentContact(chat.getContact());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Contact contact = RosterHelper.getInstance().getCurrentContact();
        if (contact != null) {
            initChat(contact);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Contact currentContact = RosterHelper.getInstance().getCurrentContact();
        if (chat == null) {
            if (SawimApplication.isManyPane()) {
                chatViewLayout.showHint();
            } else {
                getActivity().getSupportFragmentManager().popBackStack();
            }
            if (currentContact != null)
                initChat(currentContact);
            return;
        } else {
            openChat(chat.getContact());
        }
        if (!SawimApplication.isManyPane()) {
            getActivity().supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (((Xmpp) RosterHelper.getInstance().getProtocol()).getConnection() != null) {
            ((Xmpp) RosterHelper.getInstance().getProtocol()).getConnection().getMessageArchiveManagement().clearQueries();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pause(chat);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!SawimApplication.isManyPane()) {
            resume(chat);
        } else {
            if (getActivity().getIntent() == null) {

            } else {
                getActivity().setIntent(null);
            }
        }
    }

    public void pause(Chat chat) {
        if (chat != null) {
            RecyclerView chatListView = chatViewLayout.getChatListsView().getChatListView();
            chat.savedMessage = getText().length() == 0 ? null : getText();

            chat.currentPosition = getMessagesAdapter().getItemCount();
            chat.firstVisiblePosition = ((LinearLayoutManager) chatListView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            if (isScrollEnd()) {
                unreadMessageCount = 0;
                chat.currentPosition = 0;
            }
            View item = chatListView.getChildAt(0);
            chat.offset = (item == null) ? 0 : Math.abs(isScrollEnd() ? item.getTop() : item.getBottom());

            chat.setVisibleChat(false);
            chat.resetUnreadMessages();
        }
        RosterHelper.getInstance().setOnUpdateChat(null);
    }

    public void resume(Chat chat) {
        resetBar();
        if (chat == null) return;
        chat.setVisibleChat(true);
        RosterHelper.getInstance().setOnUpdateChat(this);
        unreadMessageCount = chat.getAllUnreadMessageCount();
        if (sharingText != null) {
            if (null != chat.savedMessage) {
                chat.savedMessage += " " + sharingText;
            } else {
                chat.savedMessage = sharingText;
            }
        }
        FixedEditText messageEditor = chatViewLayout.getChatInputBarView().getMessageEditor();
        messageEditor.setText(chat.savedMessage);
        messageEditor.setSelection(messageEditor.getText().length());

        if (!SawimApplication.isManyPane()) {
            drawerLayout.setDrawerLockMode(chat.getContact().isConference() ?
                    DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        updateChat();

        isOldChat = oldChatHash == chat.hashCode();
        if (isOldChat) {
            addUnreadMessagesFromHistoryToList();
        } else {
            oldChatHash = chat.hashCode();
            loadStory(false, true);
        }

        chat.resetUnreadMessages();
    }

    @SuppressLint("NewApi")
    public boolean isScrollEnd() {
        MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
        return chatListView != null && !chatListView.canScrollVertically(0);
    }

    public void setSharingText(String text) {
        sharingText = text;
    }

    private void forceGoToChat(Chat current) {
        if (current == null) return;
        pause(chat);
        RecyclerView chatListView = chatViewLayout.getChatListsView().getChatListView();
        chatListView.stopScroll();
        openChat(current.getContact());
        resume(current);
        getActivity().supportInvalidateOptionsMenu();
        updateRoster();
    }

    private void initChat(Contact c) {
        c.activate((BaseActivity) getActivity());
        chat = RosterHelper.getInstance().getProtocol().getChat(c);
    }

    public void openChat(Contact c) {
        chatViewLayout.hideHint();
        initChat(c);

        boolean isNewChat = oldChatHash != chat.hashCode();
        if (isNewChat) {
            ChatHistory.instance.registerChat(chat);
            initLabel();
            initList();
            initMucUsers();
        }
    }

    public Chat getCurrentChat() {
        return chat;
    }

    private void initLabel() {
        chatBarLayout.updateLabelIcon((BitmapDrawable) RosterAdapter.getImageChat(chat, false));
        chatBarLayout.updateTextView(chat.getContact().getName());
    }

    private void initList() {
        final MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
        final MessagesAdapter adapter = new MessagesAdapter(realmDb, chat.getContact().getUserId());
        chatListView.setAdapter(adapter);
        if (stickyRecyclerHeadersDecoration == null) {
            stickyRecyclerHeadersDecoration = new StickyRecyclerHeadersDecoration(adapter);
        } else {
            chatListView.removeItemDecoration(stickyRecyclerHeadersDecoration);
            stickyRecyclerHeadersDecoration = null;
            stickyRecyclerHeadersDecoration = new StickyRecyclerHeadersDecoration(adapter);
        }
        chatListView.addItemDecoration(stickyRecyclerHeadersDecoration);
        chatListView.addItemDecoration(new HeaderDecoration(getContext(),
                chatListView,  R.layout.item_loading));

        setHasOptionsMenu(true);
        ((LinearLayoutManager) chatListView.getLayoutManager()).setStackFromEnd(true);

        registerForContextMenu(chatListView);
        adapter.setOnItemClickListener(this);
        adapter.setOnLoadItemsListener(this);
        chatListView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            //int oldFirstVisibleItem = -1;
            boolean isScroll;
            boolean isScrollEnd;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                isScroll = newState > 0;
                if (isScrollEnd()) {
                    chat.currentPosition = 0;
                    newMessageCount = 0;
                } else {
                    chat.currentPosition = -3;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                boolean isScrollEnd_ = isScrollEnd();
                if (isScrollEnd != isScrollEnd_) {
                    isScrollEnd = isScrollEnd_;
                    chatViewLayout.getChatListsView().setShowDividerForUnreadMessage(!isScrollEnd);
                }
            }
        });
        chatListView.setOnInterceptTouchListener(new MyListView.OnInterceptTouchListener() {
            private float x1,x2;
            static final int MIN_DISTANCE = 150;
            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        x2 = event.getX();
                        float deltaX = x2 - x1;
                        if (Math.abs(deltaX) > MIN_DISTANCE) {
                            if (deltaX < 0) {
                                adapter.showTimes();
                                return true;
                            }
                        }
                        break;
                }
                return false;
            }
        });
    }

    boolean canLoadByScroll;

    private void initMucUsers() {
        if (SawimApplication.isManyPane()) {
            nickList.setVisibility(View.VISIBLE);
        } else if (drawerLayout.isDrawerOpen(nickList)) {
            drawerLayout.closeDrawer(nickList);
        }
        boolean isConference = chat.getContact() instanceof XmppServiceContact && chat.getContact().isConference();
        if (isConference) {
            new RosterStorage().loadSubContacts((XmppContact) chat.getContact());
            mucUsersFragment = new MucUsersFragment();
            mucUsersFragment.show(this, nickList);
        } else {
            mucUsersFragment = null;
            if (SawimApplication.isManyPane()) {
                nickList.setVisibility(View.GONE);
            } else {
                if (drawerLayout.isDrawerOpen(nickList)) {
                    drawerLayout.closeDrawer(nickList);
                }
            }
        }
    }

    public void sort(List<MessData> messDataList) {
        Collections.sort(messDataList, new Comparator<MessData>() {
            @Override
            public int compare(MessData left, MessData right) {
                if (left.getTime() < right.getTime()) {
                    return -1;
                } else if (left.getTime() > right.getTime()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case ADD_MESSAGE:
                Contact c = (Contact) ((Object[]) msg.obj)[0];
                MessData mess = (MessData) ((Object[]) msg.obj)[1];
                if (chat != null && chat.getContact() == c) {
                    newMessageCount++;

                    setScroll();
                }
                break;
            case UPDATE_MESSAGES:
                Contact contact = (Contact) msg.obj;
                if (chat != null && chat.getContact() == contact) {
                    setScroll();
                }
                break;

            case UPDATE_MESSAGE:
                c = (Contact) ((Object[]) msg.obj)[0];
                String messId = (String) ((Object[]) msg.obj)[1];
                int state = (Integer) ((Object[]) msg.obj)[2];
                MessData messData = findMessData(messId);
                if (messData != null) {
                    messData.setIconIndex(state);
                }
                if (chat != null && chat.getContact() == c) {
                    setScroll();
                }
                break;
        }
        return false;
    }

    private MessData findMessData(String messId) {
        if (messId == null) return null;
        return getMessagesAdapter().getVisibleMessData(messId);
    }

    public MucUsersAdapter getMucUsersAdapter() {
        return (MucUsersAdapter) nickList.getAdapter();
    }

    private MessagesAdapter getMessagesAdapter() {
        if (chatViewLayout == null) return null;
        return (MessagesAdapter) chatViewLayout.getChatListsView().getChatListView().getAdapter();
    }

    private void setScroll() {
        boolean isScrollEnd = isScrollEnd();
        getMessagesAdapter().notifyDataSetChanged();
        if (isScrollEnd) {
            RecyclerView chatListView = chatViewLayout.getChatListsView().getChatListView();
            ((LinearLayoutManager) chatListView.getLayoutManager()).scrollToPositionWithOffset(
                    getMessagesAdapter().getItemCount() - newMessageCount, chatListView.getHeight() / 4);
        }
    }

    @Override
    public void addMessage(Contact contact, MessData mess) {
        if (chat != null && chat.getContact() == contact) {
            handler.sendMessage(Message.obtain(handler, ADD_MESSAGE, new Object[]{contact, mess}));
        }
    }

    @Override
    public void updateMessages(Contact contact) {
        if (chat != null && chat.getContact() == contact) {
            handler.sendMessage(Message.obtain(handler, UPDATE_MESSAGES, contact));
        }
    }

    @Override
    public void updateMessage(Contact contact, String id, int state) {
        if (chat != null && chat.getContact() == contact) {
            handler.sendMessage(Message.obtain(handler, UPDATE_MESSAGE, new Object[] {contact, id, state}));
        }
    }

    @Override
    public void updateChat() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateChatIcon();
                if (chatsDialogFragment != null && chatsDialogFragment.isVisible())
                    chatsDialogFragment.refreshList();
            }
        });
    }

    @Override
    public void updateMucList() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mucUsersFragment != null) {
                    if (SawimApplication.isManyPane()
                            || (drawerLayout != null && nickList != null && drawerLayout.isDrawerOpen(nickList))) {
                        mucUsersFragment.update(getMucUsersAdapter());
                    }
                }
            }
        });
    }

    private void setPositionAfterHistoryLoaded(boolean hasUnreadMessages, boolean isBottomScroll,
                                               boolean isFirstOpenChat, boolean isScroll, boolean isLoad, boolean isAdded, int oldCount) {
        if (getMessagesAdapter() == null) return;
        int newCount = getMessagesAdapter().getItemCount();
        if (isAdded && (isLoad || isScroll)) {
            int position = newCount - unreadMessageCount;
            getMessagesAdapter().setPosition(position);
            getMessagesAdapter().notifyDataSetChanged();
            RecyclerView chatListView = chatViewLayout.getChatListsView().getChatListView();
            LinearLayoutManager linearLayoutManager = ((LinearLayoutManager) chatListView.getLayoutManager());
            if (isScroll && !isLoad) {
                View v = chatListView.getChildAt(0);
                final int pxOffset = (v == null) ? 0 : v.getTop();
                linearLayoutManager.scrollToPositionWithOffset(newCount == oldCount ? 0
                        : newCount - oldCount + linearLayoutManager.findFirstCompletelyVisibleItemPosition(), pxOffset);
            } else {
                if (hasUnreadMessages) {
                    if (isFirstOpenChat || isBottomScroll) {
                        linearLayoutManager.scrollToPositionWithOffset(position, 30);
                    } else if (!isBottomScroll) {
                        linearLayoutManager.scrollToPositionWithOffset(chat.firstVisiblePosition, chat.offset);
                    }
                } else {
                    if (!isFirstOpenChat) {
                        if (isBottomScroll) {
                            linearLayoutManager.scrollToPositionWithOffset(chat.firstVisiblePosition, -chat.offset);
                        } else {
                            linearLayoutManager.scrollToPositionWithOffset(chat.firstVisiblePosition + 1, chat.offset);
                        }
                    }
                }
            }
        }
        if (chat != null) {
            if (chat.currentPosition > 0) {
                chat.currentPosition = -1;
            }
        }
        unreadMessageCount = 0;
    }

    @Override
    public void onLoadItems(int i) {
        if (i == 0) {
            canLoadByScroll = false;
            loadStory(true, false);
        }
    }

    int start = 0;
    long lastId;
    private void loadStory(final boolean isScroll, final boolean isLoad) {
        if (isOldChat) {
            isOldChat = false;
            return;
        }
        if (chat != null && getMessagesAdapter() != null) {
            final boolean hasUnreadMessages = unreadMessageCount > 0;
            final boolean isBottomScroll = chat.currentPosition == 0;
            final boolean isFirstOpenChat = chat.currentPosition == -2;
            int limit = HISTORY_MESSAGES_LIMIT;
            if (chat.currentPosition > 0) {
                limit = chat.currentPosition;
            } else if (unreadMessageCount > 0) {
                limit += unreadMessageCount;
            }
            final int oldCount = getMessagesAdapter().getItemCount();
            if (!isScroll && isLoad) {
                start = limit;
            } else if (isScroll && !isLoad) {
                start = HISTORY_MESSAGES_LIMIT;
            }
            RecyclerView chatListView = chatViewLayout.getChatListsView().getChatListView();
            LinearLayoutManager linearLayoutManager = ((LinearLayoutManager) chatListView.getLayoutManager());
            int firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
            //if (firstVisibleItem == RecyclerView.NO_POSITION) return;
            boolean isScrollable = chatListView.computeHorizontalScrollRange() > chatListView.getWidth() || chatListView.computeVerticalScrollRange() > chatListView.getHeight();
            if (getMessagesAdapter().getItems().isEmpty() || isScrollable || (!isScroll && getMessagesAdapter().getItems().isEmpty())) {
                if (!getMessagesAdapter().isLoader()) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getMessagesAdapter().showLoader();
                        }
                    });
                    ((Xmpp) RosterHelper.getInstance().getProtocol()).queryMessageArchiveManagement(chat.getContact(), new OnMoreMessagesLoaded() {
                        @Override
                        public void onLoaded(final int messagesCount) {
                            lastServerMessageCount = messagesCount;

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    getMessagesAdapter().hideLoader();
                                    int newCount = getMessagesAdapter().getItemCount();
                                    int position = newCount - messagesCount;
                                    getMessagesAdapter().setPosition(position);
                                    getMessagesAdapter().notifyDataSetChanged();
                                    RecyclerView chatListView = chatViewLayout.getChatListsView().getChatListView();
                                    LinearLayoutManager linearLayoutManager = ((LinearLayoutManager) chatListView.getLayoutManager());
                                    View v = chatListView.getChildAt(0);
                                    final int pxOffset = (v == null) ? 0 : v.getTop() + Util.dipToPixels(SawimApplication.getContext(), 20);
                                    linearLayoutManager.scrollToPositionWithOffset(newCount - oldCount, pxOffset);
                                    canLoadByScroll = true;
                                }
                            });
                        }
                    });
                }
            } else {
                getMessagesAdapter().hideLoader();
                loaded(start, hasUnreadMessages, isBottomScroll,
                        isFirstOpenChat, isScroll, isLoad, oldCount);
            }
        }
    }

    private void loaded(final int start, final boolean hasUnreadMessages, final boolean isBottomScroll, final boolean isFirstOpenChat,
                        final boolean isScroll, final boolean isLoad, final int oldCount) {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (lastId != getMessagesAdapter().getItemId(start)) {
                        lastId = getMessagesAdapter().getItemId(start);
                        getMessagesAdapter().notifyItemRangeInserted(0, start);
                        setPositionAfterHistoryLoaded(hasUnreadMessages, isBottomScroll,
                                isFirstOpenChat, isScroll, isLoad,
                                start != 0, oldCount);
                        canLoadByScroll = true;
                    }
                }
            });
        }
    }

    private void addUnreadMessagesFromHistoryToList() {
        if (unreadMessageCount == 0) return;
        if (isOldChat) {
            isOldChat = false;
        }
        final boolean isBottomScroll = chat.currentPosition == 0;
        final int oldCount = getMessagesAdapter().getItemCount();
        getMessagesAdapter().setPosition(oldCount);
        if (chat == null) return;
        getMessagesAdapter().notifyItemRangeInserted(getMessagesAdapter().getItemCount(), unreadMessageCount);
        if (isBottomScroll) {
            RecyclerView chatListView = chatViewLayout.getChatListsView().getChatListView();
            LinearLayoutManager linearLayoutManager = ((LinearLayoutManager) chatListView.getLayoutManager());
            linearLayoutManager.scrollToPositionWithOffset(getMessagesAdapter().getItemCount() - unreadMessageCount, chatListView.getHeight() / 4);
        }
        chat.currentPosition = -1;
        unreadMessageCount = 0;

    }

    private void updateChatIcon() {
        if (chat == null || chatBarLayout == null) return;
        Drawable icMess = ChatHistory.instance.getLastUnreadMessageIcon();
        BitmapDrawable confIcon = StatusInfo.STATUS_OFFLINE == chat.getContact().getStatusIndex()
                ? SawimResources.usersIcon : SawimResources.usersIconOn;
        if (SawimApplication.isManyPane()) {
            chatBarLayout.updateLabelIcon(chat.getContact().isConference() ? confIcon : (BitmapDrawable) RosterAdapter.getImageChat(chat, false));
        } else {
            if (!isSearchMode) {
                ((BaseActivity) getActivity()).getSupportActionBar().setIcon(confIcon);
            }
            if (icMess == null) {
                chatBarLayout.setVisibilityChatsImage(View.GONE);
            } else {
                chatBarLayout.setVisibilityChatsImage(View.VISIBLE);
                if (!SawimApplication.isManyPane()) {
                    if (icMess == SawimResources.PERSONAL_MESSAGE_ICON) {
                        icMess = icMess.getConstantState().newDrawable();
                        icMess.setColorFilter(Scheme.getColor(R.attr.bar_personal_unread_message), PorterDuff.Mode.MULTIPLY);
                    } else {
                        icMess = icMess.getConstantState().newDrawable();
                        icMess.setColorFilter(Scheme.getColor(R.attr.bar_unread_message), PorterDuff.Mode.MULTIPLY);
                    }
                    if (chatBarLayout.getChatsImage() != null)
                        chatBarLayout.getChatsImage().setImageDrawable(icMess);
                }
            }
            chatBarLayout.updateLabelIcon(chat.getContact().isConference() ? null : (BitmapDrawable) RosterAdapter.getImageChat(chat, false));
        }
    }

    private void updateRoster() {
        RosterFragment rosterFragment = (RosterFragment) getFragmentManager().findFragmentById(R.id.roster_fragment);
        if (rosterFragment != null)
            rosterFragment.update();
    }

    private boolean searchTextFromMessage(final boolean up) {
        final String query = getTitleBar().getSearchEditText().getText().toString().toLowerCase();
        if (query.isEmpty()) return false;
        if (searchMessagePositions == null) {
            searchMessagePositions = chat.getHistory().getSearchMessagesIds(query);
        }
        final boolean positionsIsNtEmpty = !searchMessagePositions.isEmpty();
        final int oldCount = getMessagesAdapter().getItemCount();
        if (positionsIsNtEmpty) {
            getMessagesAdapter().setQuery(query);
            if (up) {
                searchMessagePositionsCount++;
            } else {
                searchMessagePositionsCount--;
            }
            if (searchMessagePositionsCount < 0) {
                searchMessagePositionsCount = searchMessagePositions.size() - 1;
            } else if (searchMessagePositionsCount >= searchMessagePositions.size()) {
                searchMessagePositionsCount = 0;
            }
            final int position = searchMessagePositions.get(searchMessagePositionsCount);
            SawimApplication.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    if (chat == null) return;
                    RealmResults<ru.sawim.db.model.Message> messages = null;
                    if (position > oldCount) {
                        messages = chat.getHistory().getMessages(getMessagesAdapter().getItem(0).getDate());
                    }
                    if (getActivity() != null) {
                        final RealmResults<ru.sawim.db.model.Message> finalMessages = messages;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (finalMessages != null) {
                                    getMessagesAdapter().setItems(finalMessages);
                                }
                                getMessagesAdapter().notifyDataSetChanged();
                                RecyclerView chatListView = chatViewLayout.getChatListsView().getChatListView();
                                LinearLayoutManager linearLayoutManager = ((LinearLayoutManager) chatListView.getLayoutManager());
                                //linearLayoutManager.scrollToPosition(position);
                                linearLayoutManager.scrollToPositionWithOffset(position, chatListView.getHeight() / 4);
                            }
                        });
                    }
                }
            });
        } else {
            resetSearchMode(true);
        }
        return positionsIsNtEmpty;
    }

    private void resetSearchMode(boolean showToast) {
        searchMessagePositionsCount = 0;
        searchMessagePositions = null;
        if (getMessagesAdapter() != null) {
            getMessagesAdapter().setQuery(null);
            getMessagesAdapter().notifyDataSetChanged();
        }
        if (showToast)
            Toast.makeText(getActivity().getApplicationContext(), R.string.not_found, Toast.LENGTH_LONG).show();
    }

    private void enableSearchMode() {
        if (isSearchMode) return;
        isSearchMode = true;

        ActionBar actionBar = ((BaseActivity) getActivity()).getSupportActionBar();
        actionBar.setIcon(null);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
        }
        searchMenuItem.setVisible(false);
        getTitleBar().showSearch(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchTextFromMessage(true);
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchTextFromMessage(false);
            }
        });

        getMessagesAdapter().notifyDataSetChanged();

        getTitleBar().getSearchEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (/*actionId == EditorInfo.IME_ACTION_SEARCH || */event != null && (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_SEARCH || event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    Util.hideKeyboard(getActivity());
                    searchTextFromMessage(true);
                }
                return false;
            }
        });
    }

    private void destroySearchMode() {
        isSearchMode = false;
        resetSearchMode(false);
        getTitleBar().hideSearch();
        resetBar();
        updateChatIcon();
        if (getMessagesAdapter() != null) {
            getMessagesAdapter().notifyDataSetChanged();
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    private void destroyMultiCitationMode() {
        Clipboard.setClipBoardText(getActivity(), null);
        getMessagesAdapter().setMultiQuoteMode(false);
        for (int i = 0; i < getMessagesAdapter().getItemCount(); ++i) {
            MessData messData = getMessagesAdapter().getMessData(getMessagesAdapter().getItem(i));
            if (messData != null && messData.isMarked()) {
                messData.setMarked(false);
            }
        }
        getMessagesAdapter().notifyDataSetChanged();
        getActivity().supportInvalidateOptionsMenu();
    }

    private StringBuilder buildQuote() {
        StringBuilder multiQuoteBuffer = new StringBuilder();
        for (int i = 0; i < getMessagesAdapter().getItemCount(); ++i) {
            MessData mData = getMessagesAdapter().getMessData(getMessagesAdapter().getItem(i));
            CharSequence msg = mData.getText();
            if (mData.isMarked()) {
                if (mData.isMe())
                    msg = "*" + mData.getNick() + " " + msg;
                String msgSerialize = Clipboard.serialize(false, mData.isIncoming(), mData.getNick(), mData.getStrTime(), msg);
                multiQuoteBuffer.append(msgSerialize);
                multiQuoteBuffer.append(CITATION_DIVIDER);
            }
        }
        return multiQuoteBuffer;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (BaseActivity.getExternalApi().onActivityResult(requestCode, resultCode, data)) return;
        super.onActivityResult(requestCode, resultCode, data);
    }

    private MyMenu getMenu() {
        Contact contact = chat.getContact();
        final MyMenu menu = new MyMenu();
        boolean accessible = chat.getWritable() && (contact.isSingleUserContact() || contact.isOnline());
        menu.add(getString(getMessagesAdapter().isMultiQuoteMode() ?
                R.string.disable_multi_citation : R.string.include_multi_citation), ContactMenu.MENU_MULTI_CITATION);
        if (0 < chat.getAuthRequestCounter()) {
            menu.add(R.string.grant, ContactMenu.USER_MENU_GRANT_AUTH);
            menu.add(R.string.deny, ContactMenu.USER_MENU_DENY_AUTH);
        }
        if (!contact.isAuth()) {
            menu.add(R.string.requauth, ContactMenu.USER_MENU_REQU_AUTH);
        }
        if (accessible) {
            menu.add(R.string.ft_name, ContactMenu.USER_MENU_FILE_TRANS);
            menu.add(R.string.ft_cam, ContactMenu.USER_MENU_CAM_TRANS);
        }

        if (contact.isSingleUserContact()) {
            menu.add(R.string.user_statuses, ContactMenu.USER_MENU_STATUSES);
        } else {
            menu.add(R.string.conference_theme, ContactMenu.USER_MENU_STATUSES);
            if (contact.isOnline()) {
                menu.add(R.string.leave_chat, ContactMenu.CONFERENCE_DISCONNECT);
            }
        }
        return menu;
    }

    public void onPrepareOptionsMenu_(Menu menu) {
        if (chat == null) return;
        menu.clear();
        MyMenu myMenu = getMenu();
        for (int i = 0; i < myMenu.getCount(); ++i) {
            MyMenuItem myMenuItem = myMenu.getItem(i);
            menu.add(Menu.FIRST, myMenuItem.idItem, 2, myMenuItem.nameItem);
        }
        addSearchView(menu);
        super.onPrepareOptionsMenu(menu);
    }

    public void addSearchView(Menu menu) {
        if (!isSearchMode) {
            searchMenuItem = menu.add(Menu.FIRST, ContactMenu.CHAT_MENU_SEARCH, 2, "")
                    .setIcon(R.drawable.ic_search_white_24dp);
            MenuItemCompat.setShowAsAction(searchMenuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        }
    }

    MenuItem searchMenuItem;

    public void onOptionsItemSelected_(MenuItem item) {
        onOptionsItemSelected(item.getItemId());
    }

    private void onOptionsItemSelected(int id) {
        if (chat == null) return;
        final Contact contact = chat.getContact();
        switch (id) {
            case ContactMenu.CHAT_MENU_SEARCH:
                enableSearchMode();
                break;
            case ContactMenu.MENU_MULTI_CITATION:
                if (getMessagesAdapter().isMultiQuoteMode()) {
                    destroyMultiCitationMode();
                } else {
                    getMessagesAdapter().setMultiQuoteMode(true);
                    Toast.makeText(getActivity(), R.string.hint_multi_citation, Toast.LENGTH_LONG).show();
                    getMessagesAdapter().notifyDataSetChanged();
                    getActivity().supportInvalidateOptionsMenu();
                }
                break;

            case ContactMenu.USER_MENU_FILE_TRANS:
                BaseActivity.getExternalApi().setFragment(this);
                new ContactMenu(contact).doAction((BaseActivity) getActivity(), ContactMenu.USER_MENU_FILE_TRANS);
                break;
            case ContactMenu.USER_MENU_CAM_TRANS:
                BaseActivity.getExternalApi().setFragment(this);
                new ContactMenu(contact).doAction((BaseActivity) getActivity(), ContactMenu.USER_MENU_CAM_TRANS);
                break;

            case ContactMenu.CONFERENCE_DISCONNECT:
                ChatHistory.instance.unregisterChat(chat);
                chat.setVisibleChat(false);
                RosterHelper.getInstance().setOnUpdateChat(null);
                chat = null;
                new ContactMenu(contact).doAction((BaseActivity) getActivity(), ContactMenu.CONFERENCE_DISCONNECT);
                if (!SawimApplication.isManyPane()) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
                break;

            default:
                new ContactMenu(contact).doAction((BaseActivity) getActivity(), id);
                getActivity().supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final Contact contact = chat.getContact();
        menu.add(Menu.FIRST, ContactMenu.MENU_COPY_TEXT, 0, android.R.string.copy);
        menu.add(Menu.FIRST, ContactMenu.ACTION_QUOTE, 0, R.string.quote);
        if (mucUsersFragment != null) {
            menu.add(Menu.FIRST, ContactMenu.COMMAND_PRIVATE, 0, R.string.open_private);
            menu.add(Menu.FIRST, ContactMenu.COMMAND_INFO, 0, R.string.info);
            menu.add(Menu.FIRST, ContactMenu.COMMAND_STATUS, 0, R.string.user_statuses);
        }
        menu.add(Menu.FIRST, ContactMenu.ACTION_TO_NOTES, 0, R.string.add_to_notes);
        contact.addChatMenuItems(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        MyListView.RecyclerContextMenuInfo info = (MyListView.RecyclerContextMenuInfo) item.getMenuInfo();
        if (getMessagesAdapter() == null) return super.onContextItemSelected(item);
        final MessData md = getMessagesAdapter().getMessData(getMessagesAdapter().getItem(info.position));
        if (md == null) return super.onContextItemSelected(item);
        final Contact contact = chat.getContact();
        String nick = md.getNick();
        CharSequence msg = md.getText();
        Protocol protocol = RosterHelper.getInstance().getProtocol();
        switch (item.getItemId()) {
            case ContactMenu.MENU_COPY_TEXT:
                Clipboard.setClipBoardText(getActivity(), msg + "\n");
                Toast.makeText(getActivity(), R.string.hint_citation, Toast.LENGTH_LONG).show();
                break;

            case ContactMenu.ACTION_QUOTE:
                StringBuilder sb = new StringBuilder();
                if (md.isMe() || md.isPresence()) {
                    Clipboard.insertQuotingChars(sb, msg, false, md.isIncoming() ? '\u00bb' : '\u00ab');
                    sb.append(CITATION_DIVIDER);
                } else {
                    sb.append(Clipboard.serialize(true, md.isIncoming(), md.getNick(), md.getStrTime(), msg));
                    sb.append(CITATION_DIVIDER);
                }
                Clipboard.setClipBoardText(getActivity(), 0 == sb.length() ? null : sb.toString());
                Toast.makeText(getActivity(), R.string.hint_citation, Toast.LENGTH_LONG).show();
                break;

            case ContactMenu.COMMAND_PRIVATE:
                String jid = Jid.realJidToSawimJid(contact.getUserId() + "/" + nick);
                XmppServiceContact c = (XmppServiceContact) protocol.getItemByUID(jid);
                if (null == c) {
                    c = (XmppServiceContact) protocol.createTempContact(jid, false);
                    protocol.addTempContact(c);
                }
                pause(getCurrentChat());
                openChat(c);
                resume(getCurrentChat());
                getActivity().supportInvalidateOptionsMenu();
                break;
            case ContactMenu.COMMAND_INFO:
                protocol.showUserInfo((BaseActivity) getActivity(), ((XmppServiceContact) contact).getPrivateContact(nick));
                break;
            case ContactMenu.COMMAND_STATUS:
                protocol.showStatus((BaseActivity) getActivity(), ((XmppServiceContact) contact).getPrivateContact(nick));
                break;

            case ContactMenu.ACTION_TO_NOTES:
                MirandaNotes notes = ((Xmpp) protocol).getMirandaNotes();
                notes.showIt((BaseActivity) getActivity());
                MirandaNotes.Note note = notes.addEmptyNote();
                note.tags = md.getNick() + " " + md.getStrTime();
                note.text = md.getText();
                notes.showNoteEditor((BaseActivity) getActivity(), note);
                break;
        }
        return super.onContextItemSelected(item);
    }

    public void showKeyboard() {
        FixedEditText messageEditor = chatViewLayout.getChatInputBarView().getMessageEditor();
        if (messageEditor == null) return;
        messageEditor.requestFocus();
        if (Resources.getSystem().getConfiguration().hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO) {
            InputMethodManager keyboard = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(messageEditor, InputMethodManager.SHOW_FORCED);
        }
    }

    private void hideKeyboard() {
        if (Options.getBoolean(JLocale.getString(R.string.pref_hide_keyboard))) {
            Util.hideKeyboard(getActivity());
        }
    }

    @Override
    public void pastText(final String text) {
        setText(" " + text + " ");
        showKeyboard();
    }

    public void send() {
        final boolean isScrollEnd = isScrollEnd();
        hideKeyboard();
        final String text = getText();
        resetText();
        if (chat != null) {
            chat.sendMessage(text);
        }
        RecyclerView chatListView = chatViewLayout.getChatListsView().getChatListView();
        if (chatListView != null) {
            chatListView.post(new Runnable() {
                @Override
                public void run() {
                    if (chat != null) {
                        chat.savedMessage = null;
                        getMessagesAdapter().setPosition(-1);
                        chat.currentPosition = 0;
                        newMessageCount = 0;
                        getMessagesAdapter().notifyDataSetChanged();
                        if (isScrollEnd) {
                            RecyclerView chatListView = chatViewLayout.getChatListsView().getChatListView();
                            LinearLayoutManager linearLayoutManager = ((LinearLayoutManager) chatListView.getLayoutManager());
                            linearLayoutManager.scrollToPositionWithOffset(getMessagesAdapter().getItemCount() - 1, -chat.offset);
                        }
                    }
                }
            });
        }
    }

    public static void showAuthorizationDialog(final BaseActivity activity, final Chat newChat) {
        ContactAuthDialogFragment contactAuthDialogFragment = new ContactAuthDialogFragment();
        contactAuthDialogFragment.setNewChat(newChat);
        contactAuthDialogFragment.show(activity.getSupportFragmentManager(), "auth");
    }

    private boolean canAdd(String what) {
        String text = getText();
        if (0 == text.length()) return false;
        // more then one comma
        if (text.indexOf(',') != text.lastIndexOf(',')) return true;
        // replace one post number to another
        if (what.startsWith("#") && !text.contains(" ")) return false;
        return true/*!text.endsWith(", ")*/;
    }

    private void resetText() {
        FixedEditText messageEditor = chatViewLayout.getChatInputBarView().getMessageEditor();
        messageEditor.setText("");
    }

    private String getText() {
        FixedEditText messageEditor = chatViewLayout.getChatInputBarView().getMessageEditor();
        return messageEditor.getText().toString();
    }

    private void setText(final String text) {
        FixedEditText messageEditor = chatViewLayout.getChatInputBarView().getMessageEditor();
        String t = null == text ? "" : text;
        if ((0 == t.length()) || !canAdd(t)) {
            messageEditor.setText(t);
            messageEditor.setSelection(t.length());
        } else {
            insert(t);
        }
    }

    public void insert(String text) {
        FixedEditText messageEditor = chatViewLayout.getChatInputBarView().getMessageEditor();
        if (messageEditor == null) return;
        int start = messageEditor.getSelectionStart();
        int end = messageEditor.getSelectionEnd();
        messageEditor.getText().replace(Math.min(start, end), Math.max(start, end),
                text, 0, text.length());
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        private boolean compose = false;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (chat == null) return;
            final Protocol protocol = RosterHelper.getInstance().getProtocol();
            final Contact contact = chat.getContact();
            int length = s.length();
            if (length > 0) {
                if (!compose) {
                    compose = true;
                    protocol.sendTypingNotify(contact, true);
                }
            } else {
                if (compose) {
                    compose = false;
                    protocol.sendTypingNotify(contact, false);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            String text = s.subSequence(0, s.length()).toString();
            if (sharingText != null && sharingText.equals(text)) {
                sharingText = null;
            }
            if (getMessagesAdapter() != null && getMessagesAdapter().isMultiQuoteMode()) {
                String clipBoardText = Clipboard.getClipBoardText(getActivity());
                if (clipBoardText != null && text.equals(clipBoardText)) {
                    destroyMultiCitationMode();
                }
            }
            TextFormatter.getInstance().detectEmotions(s);
        }
    };

    @Override
    public void onForceGoToChat(Chat selectedChat) {
        forceGoToChat(selectedChat);
    }

    @Override
    public void onItemClick(View v, int position) {
        final Protocol protocol = RosterHelper.getInstance().getProtocol();
        final Contact contact = chat.getContact();
        MessData mData = getMessagesAdapter().getMessData(getMessagesAdapter().getItem(position));
        if (mData == null) return;
        String msg = mData.getText().toString();
        if (getMessagesAdapter().isMultiQuoteMode()) {
            mData.setMarked(!mData.isMarked());
            StringBuilder multiQuoteBuffer = buildQuote();
            Clipboard.setClipBoardText(getActivity(), 0 == multiQuoteBuffer.length() ? null : multiQuoteBuffer.toString());
            getMessagesAdapter().notifyDataSetChanged();
        } else {
            if (chat.isBlogBot()) {
                new JuickMenu(getActivity(), contact.getUserId(), chat.getBlogPostId(msg)).show();
                return;
            }
            if (mucUsersFragment != null) {
                XmppServiceContact xmppServiceContact = ((XmppServiceContact) contact);
                if (xmppServiceContact.getName().equals(mData.getNick())) return;
                if (xmppServiceContact.getContact(mData.getNick()) == null) {
                    Toast.makeText(getActivity(), getString(R.string.contact_walked), Toast.LENGTH_LONG).show();
                }
                String text = chat.onMessageSelected(mData);
                if (!text.equals("")) {
                    setText(text);
                }
            }
            showKeyboard();
            if (SawimApplication.isManyPane()) {
                if (nickList.getVisibility() == View.VISIBLE
                        && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                    nickList.setVisibility(View.GONE);
            } else {
                if (drawerLayout.isDrawerOpen(nickList)
                        && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                    drawerLayout.closeDrawer(nickList);
            }
        }
    }

    @Override
    public void onConfigurationChanged() {
        hideKeyboard();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            send();
            return true;
        }
        if (sendByEnter) {
            if (keyEvent != null && actionId == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                send();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK && smileysPopup != null && smileysPopup.isShown(chatViewLayout)) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                smileysPopup.hide(chatViewLayout);
            }
            return true;
        }
        if (sendByEnter) {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                send();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        insert(PlainMessage.CMD_ME);
        showKeyboard();
        return true;
    }
}
