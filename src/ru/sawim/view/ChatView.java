package ru.sawim.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import protocol.xmpp.*;
import ru.sawim.listener.OnMoreMessagesLoaded;
import ru.sawim.listener.OnUpdateChat;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.StatusInfo;
import ru.sawim.*;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.models.MessagesAdapter;
import ru.sawim.models.MucUsersAdapter;
import ru.sawim.models.RosterAdapter;
import ru.sawim.roster.RosterHelper;
import ru.sawim.text.TextFormatter;
import ru.sawim.view.menu.JuickMenu;
import ru.sawim.view.menu.MyMenu;
import ru.sawim.view.menu.MyMenuItem;
import ru.sawim.widget.FixedEditText;
import ru.sawim.widget.MyImageButton;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;
import ru.sawim.widget.chat.ChatBarView;
import ru.sawim.widget.chat.ChatInputBarView;
import ru.sawim.widget.chat.ChatListsView;
import ru.sawim.widget.chat.ChatViewRoot;

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
public class ChatView extends SawimFragment implements OnUpdateChat, Handler.Callback, ChatsDialogFragment.OnForceGoToChatListener {

    public static final String TAG = ChatView.class.getSimpleName();

    private static final int HISTORY_MESSAGES_LIMIT = 20;
    private static final String CITATION_DIVIDER = "\n---\n";

    private static final String PROTOCOL_ID = "protocol_id";
    private static final String CONTACT_ID = "contact_id";

    private static final int ADD_MESSAGE = 0;
    private static final int UPDATE_MESSAGES = 1;
    private Handler handler;

    private Chat chat;
    private int oldChatHash;
    private boolean isOldChat;
    private int unreadMessageCount;
    private String sharingText;
    private boolean sendByEnter;
    private int newMessageCount = 0;
    private boolean messagesLoaded = true;

    private String oldSearchQuery = "";
    private boolean isSearchMode;
    private int searchPositionsCount;
    private List<Integer> searchMessagesIds;

    private MyListView nickList;
    private ChatViewRoot chatViewLayout;
    private SmileysPopup smileysPopup;
    private MucUsersView mucUsersView;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private ChatBarView chatBarLayout;
    private ChatsDialogFragment chatsDialogFragment;
    private int lastServerMessageCount;

    public static ChatView newInstance(String protocolId, String contactId) {
        ChatView chatView = new ChatView();
        Bundle args = new Bundle();
        args.putString(PROTOCOL_ID, protocolId);
        args.putString(CONTACT_ID, contactId);
        chatView.setArguments(args);
        return chatView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        handler = new Handler(this);

        MyImageButton chatsImage = new MyImageButton(activity);
        MyImageButton menuButton = new MyImageButton(activity);
        MyImageButton smileButton = new MyImageButton(activity);
        MyImageButton sendButton = new MyImageButton(activity);
        FixedEditText messageEditor = new FixedEditText(activity);
        chatBarLayout = new ChatBarView(activity, chatsImage);
        MyListView chatListView = new MyListView(activity);
        nickList = new MyListView(activity);
        ChatListsView chatListsView = new ChatListsView(activity, SawimApplication.isManyPane(), chatListView, nickList);
        ChatInputBarView chatInputBarView = new ChatInputBarView(activity, menuButton, smileButton, messageEditor, sendButton);
        chatViewLayout = new ChatViewRoot(activity, chatListsView, chatInputBarView);
        smileysPopup = new SmileysPopup((BaseActivity) activity, chatViewLayout);
        drawerLayout = new DrawerLayout(activity);

        ((BaseActivity) activity).setConfigurationChanged(new BaseActivity.OnConfigurationChanged() {
            @Override
            public void onConfigurationChanged() {
                hideKeyboard();
            }
        });
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
        chatViewLayout.getChatInputBarView().setImageButtons(menuButton, smileButton, sendButton, isSearchMode);
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
                    if (mucUsersView != null)
                        mucUsersView.update(getMucUsersAdapter());
                }
            };
            drawerLayout.setDrawerListener(drawerToggle);
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerToggle.syncState();
        }
        if (!SawimApplication.isManyPane()) {
            chatBarLayout.getChatsImage().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Chat newChat = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
                    if (newChat == null) return;
                    if (0 < newChat.getAuthRequestCounter()) {
                        showAuthorizationDialog((BaseActivity) getActivity(), newChat);
                    } else {
                        forceGoToChat(newChat);
                    }
                }
            });
        }
        if (SawimApplication.isManyPane()) {
            menuButton.setVisibility(ImageButton.VISIBLE);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (chat == null) return;
                    final MyMenu menu = getMenu();
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setCancelable(true);
                    builder.setTitle(null);
                    builder.setAdapter(menu, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onOptionsItemSelected(menu.getItem(which).idItem);
                        }
                    });
                    builder.create().show();
                }
            });
        } else
            menuButton.setVisibility(ImageButton.GONE);
        smileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                smileysPopup.show(activity, chatViewLayout);
            }
        });
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
        messageEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smileysPopup.hide(chatViewLayout);
            }
        });
        messageEditor.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEND) {
                    send();
                    return true;
                }
                if (sendByEnter) {
                    if (keyEvent != null && i == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        send();
                        return true;
                    }
                }
                return false;
            }
        });
        messageEditor.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_BACK && smileysPopup != null && smileysPopup.isShown(chatViewLayout)) {
                    if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        smileysPopup.hide(chatViewLayout);
                    }
                    return true;
                }
                if (sendByEnter) {
                    if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER) {
                        send();
                        return true;
                    }
                }
                return false;
            }
        });
        if (sendByEnter) {
            sendButton.setVisibility(ImageButton.GONE);
        } else {
            sendButton.setVisibility(ImageButton.VISIBLE);
            sendButton.setOnClickListener(sendBtnClickListener);
            sendButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    insert(PlainMessage.CMD_ME);
                    showKeyboard();
                    return true;
                }
            });
        }

        return SawimApplication.isManyPane() ? chatViewLayout : drawerLayout;
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
            if (getMessagesAdapter().isEmpty()) {
                ChatHistory.instance.unregisterChat(chat);
            }
            chat = null;
        }
        handler = null;
        chatBarLayout = null;
        chatViewLayout = null;
        smileysPopup = null;
        drawerLayout = null;
        mucUsersView = null;
        chatsDialogFragment = null;
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
            outState.putString(PROTOCOL_ID, chat.getProtocol().getUserId());
            outState.putString(CONTACT_ID, chat.getContact().getUserId());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            savedInstanceState = getArguments();
        }
        if (savedInstanceState != null) {
            Protocol protocol = RosterHelper.getInstance().getProtocol(savedInstanceState.getString(PROTOCOL_ID));
            if (protocol != null) {
                Contact contact = protocol.getItemByUID(savedInstanceState.getString(CONTACT_ID));
                if (contact != null) {
                    initChat(protocol, contact);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Contact currentContact = RosterHelper.getInstance().getCurrentContact();
        if (chat == null && currentContact != null && !SawimApplication.isManyPane()) {
            initChat(currentContact.getProtocol(), currentContact);
        }
        if (chat == null) {
            if (SawimApplication.isManyPane()) {
                chatViewLayout.showHint();
            } else {
                getActivity().getSupportFragmentManager().popBackStack();
            }
            if (currentContact != null)
                initChat(currentContact.getProtocol(), currentContact);
            return;
        } else {
            openChat(chat.getProtocol(), chat.getContact());
        }
        if (!SawimApplication.isManyPane()) {
            getActivity().supportInvalidateOptionsMenu();
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
        resume(chat);
    }

    public void pause(Chat chat) {
        if (chat == null) return;
        MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
        chat.savedMessage = getText().length() == 0 ? null : getText();

        chat.currentPosition = getMessagesAdapter().getCount();
        chat.firstVisiblePosition = chatListView.getFirstVisiblePosition();
        if (isScrollEnd()) {
            unreadMessageCount = 0;
            chat.currentPosition = 0;
        }
        View item = chatListView.getChildAt(0);
        chat.offset = (item == null) ? 0 : Math.abs(isScrollEnd() ? item.getTop() : item.getBottom());

        chat.setVisibleChat(false);
        RosterHelper.getInstance().setOnUpdateChat(null);
        chat.resetUnreadMessages();
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
        MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
        chatListView.stopScroll();
        openChat(current.getProtocol(), current.getContact());
        resume(current);
        getActivity().supportInvalidateOptionsMenu();
        updateRoster();
    }

    private void initChat(Protocol p, Contact c) {
        c.activate((BaseActivity) getActivity(), p);
        chat = p.getChat(c);
    }

    public void openChat(Protocol p, Contact c) {
        chatViewLayout.hideHint();
        initChat(p, c);

        boolean isNewChat = oldChatHash != chat.hashCode();
        if (isNewChat) {
            ChatHistory.instance.registerChat(chat);
            initLabel();
            initList();
            initMucUsers();
        }

        /*new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 21; i < 100; i++) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    chat.sendMessage(""+i);
                }
            }
        }).start();*/
    }

    public Chat getCurrentChat() {
        return chat;
    }

    private void initLabel() {
        chatBarLayout.updateLabelIcon(RosterAdapter.getImageChat(chat, false));
        chatBarLayout.updateTextView(chat.getContact().getName());
        chatBarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatsDialogFragment = new ChatsDialogFragment();
                chatsDialogFragment.setForceGoToChatListener(ChatView.this);
                chatsDialogFragment.show(getFragmentManager(), "force-go-to-chat");
            }
        });
    }

    private void initList() {
        MessagesAdapter adapter = new MessagesAdapter();
        MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
        chatListView.setAdapter(adapter);
        chatListView.setStackFromBottom(true);
        chatListView.setFastScrollEnabled(true);
        chatListView.setOnCreateContextMenuListener(this);
        chatListView.setOnItemClickListener(chatClick);
        chatListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            //int oldFirstVisibleItem = -1;
            boolean isScroll;
            boolean isScrollEnd;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                isScroll = scrollState > 0;
                if (isScrollEnd()) {
                    chat.currentPosition = 0;
                    newMessageCount = 0;
                } else {
                    chat.currentPosition = -3;
                }
            }

            @Override
            public void onScroll(AbsListView view, final int firstVisibleItem, final int visibleItemCount, int totalItemCount) {
                synchronized (getMessagesAdapter().getItems()) {
                    //if (oldFirstVisibleItem != firstVisibleItem) {
                    //    oldFirstVisibleItem = firstVisibleItem;
                        if (visibleItemCount > 0 && firstVisibleItem < 5) {
                            loadStory(isScroll, false);
                        }
                    //} else {
                    //    oldFirstVisibleItem = -1;
                    //}
                }

                boolean isScrollEnd_ = isScrollEnd();
                if (isScrollEnd != isScrollEnd_) {
                    isScrollEnd = isScrollEnd_;
                    chatViewLayout.getChatListsView().setShowDividerForUnreadMessage(!isScrollEnd);
                }
            }
        });
    }

    private void initMucUsers() {
        if (SawimApplication.isManyPane()) {
            nickList.setVisibility(View.VISIBLE);
        } else if (drawerLayout.isDrawerOpen(nickList)) {
            drawerLayout.closeDrawer(nickList);
        }
        boolean isConference = chat.getContact() instanceof XmppServiceContact && chat.getContact().isConference();
        if (isConference) {
            mucUsersView = new MucUsersView();
            mucUsersView.show(chat.getProtocol(), (XmppServiceContact) chat.getContact(), this, nickList);
        } else {
            mucUsersView = null;
            if (SawimApplication.isManyPane()) {
                nickList.setVisibility(View.GONE);
            } else {
                if (drawerLayout.isDrawerOpen(nickList)) {
                    drawerLayout.closeDrawer(nickList);
                }
            }
        }
    }

    public void sort() {
        Collections.sort(getMessagesAdapter().getItems(), new Comparator<MessData>() {
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
                    getMessagesAdapter().getItems().add(mess);
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
        }
        return false;
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
            MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
            chatListView.setSelectionFromTop(getMessagesAdapter().getCount() - newMessageCount, chatListView.getHeight() / 4);
        }
    }

    @Override
    public void addMessage(Contact contact, MessData mess) {
        synchronized (getMessagesAdapter().getItems()) {
            if (chat != null && chat.getContact() == contact) {
                handler.sendMessage(Message.obtain(handler, ADD_MESSAGE, new Object[]{contact, mess}));
            }
        }
    }

    @Override
    public void updateMessages(Contact contact) {
        if (chat != null && chat.getContact() == contact) {
            handler.sendMessage(Message.obtain(handler, UPDATE_MESSAGES, contact));
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
                if (mucUsersView != null) {
                    if (SawimApplication.isManyPane()
                            || (drawerLayout != null && nickList != null && drawerLayout.isDrawerOpen(nickList))) {
                        mucUsersView.update(getMucUsersAdapter());
                    }
                }
            }
        });
    }

    private void setPositionAfterHistoryLoaded(boolean hasUnreadMessages, boolean isBottomScroll,
                                               boolean isFirstOpenChat, boolean isScroll, boolean isLoad, boolean isAdded, int oldCount) {
        if (getMessagesAdapter() == null) return;
        int newCount = getMessagesAdapter().getCount();
        if (isAdded && (isLoad || isScroll)) {
            int position = newCount - unreadMessageCount;
            getMessagesAdapter().setPosition(position);
            getMessagesAdapter().notifyDataSetChanged();
            MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
            if (isScroll && !isLoad) {
                //chatListView.setSelection(newCount == oldCount ? 0 : newCount - oldCount + 1);
                View v = chatListView.getChildAt(0);
                final int pxOffset = (v == null) ? 0 : v.getTop();
                chatListView.setSelectionFromTop(newCount == oldCount ? 0 : newCount - oldCount + chatListView.getFirstVisiblePosition(), pxOffset);
            } else {
                if (hasUnreadMessages) {
                    if (isFirstOpenChat || isBottomScroll) {
                        chatListView.setSelectionFromTop(position, 30);
                    } else if (!isBottomScroll) {
                        chatListView.setSelectionFromTop(chat.firstVisiblePosition, chat.offset);
                    }
                } else {
                    if (!isFirstOpenChat) {
                        if (isBottomScroll) {
                            chatListView.setSelectionFromTop(chat.firstVisiblePosition, -chat.offset);
                        } else {
                            chatListView.setSelectionFromTop(chat.firstVisiblePosition + 1, chat.offset);
                        }
                    }
                }
            }
        }
        if (chat.currentPosition > 0) {
            chat.currentPosition = -1;
        }
        unreadMessageCount = 0;
    }

    private void loadStory(final boolean isScroll, final boolean isLoad) {
        if (!messagesLoaded) return;
        messagesLoaded = false;
        SawimApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
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

                    List<MessData> messDataList = null;
                    final int oldCount = getMessagesAdapter().getCount();
                    final long oldTimeStamp = oldCount == 0 ? 0 : getMessagesAdapter().getItem(0).getTime();
                    if (!isScroll && isLoad) {
                        messDataList = chat.getHistory().addNextListMessages(chat, limit, oldTimeStamp);
                    } else if (isScroll && !isLoad) {
                        messDataList = chat.getHistory().addNextListMessages(chat, HISTORY_MESSAGES_LIMIT, oldTimeStamp);
                    }
                    final List<MessData> finalMessDataList = messDataList;
                    if ((finalMessDataList == null || finalMessDataList.isEmpty()) && chat.getProtocol() instanceof Xmpp) {
                        ((Xmpp) chat.getProtocol()).queryMessageArchiveManagement(chat.getContact(), new OnMoreMessagesLoaded() {
                            @Override
                            public void onLoaded(int messagesCount) {
                                lastServerMessageCount = messagesCount;
                                //sort();
                                final List<MessData> messDataList = chat.getHistory().addNextListMessages(chat, HISTORY_MESSAGES_LIMIT, oldTimeStamp);
                                if (handler != null) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (messDataList != null) {
                                                if (isLoad || isScroll) {
                                                    getMessagesAdapter().getItems().addAll(0, messDataList);
                                                    setPositionAfterHistoryLoaded(hasUnreadMessages, isBottomScroll,
                                                            isFirstOpenChat, isScroll, isLoad, true, oldCount);
                                                }
                                            }
                                            messagesLoaded = true;
                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        if (handler != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (finalMessDataList != null) {
                                        getMessagesAdapter().getItems().addAll(0, finalMessDataList);
                                        setPositionAfterHistoryLoaded(hasUnreadMessages, isBottomScroll,
                                                isFirstOpenChat, isScroll, isLoad, !finalMessDataList.isEmpty(), oldCount);
                                        messagesLoaded = true;
                                    }
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    private void addUnreadMessagesFromHistoryToList() {
        if (unreadMessageCount == 0) return;
        if (isOldChat) {
            isOldChat = false;
        }
        final boolean isBottomScroll = chat.currentPosition == 0;
        final int oldCount = getMessagesAdapter().getCount();
        getMessagesAdapter().setPosition(oldCount);
        SawimApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final List<MessData> messDataList = chat.getHistory().addNextListMessages(chat, unreadMessageCount, 0);
                if (!messDataList.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getMessagesAdapter().getItems().addAll(messDataList);
                                getMessagesAdapter().notifyDataSetChanged();
                                if (isBottomScroll) {
                                    MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
                                    chatListView.setSelectionFromTop(getMessagesAdapter().getCount() - unreadMessageCount, chatListView.getHeight() / 4);
                                }
                                chat.currentPosition = -1;
                                unreadMessageCount = 0;
                            }
                        });
                    }
                }
            }
        });
    }

    private void updateChatIcon() {
        if (chat == null || chatBarLayout == null) return;
        Drawable icMess = ChatHistory.instance.getLastUnreadMessageIcon();
        BitmapDrawable confIcon = StatusInfo.STATUS_OFFLINE == chat.getContact().getStatusIndex()
                ? SawimResources.usersIcon : SawimResources.usersIconOn;
        if (SawimApplication.isManyPane()) {
            chatBarLayout.updateLabelIcon(chat.getContact().isConference() ? confIcon : RosterAdapter.getImageChat(chat, false));
        } else {
            ((BaseActivity) getActivity()).getSupportActionBar().setIcon(confIcon);
            if (icMess == null) {
                chatBarLayout.setVisibilityChatsImage(View.GONE);
            } else {
                chatBarLayout.setVisibilityChatsImage(View.VISIBLE);
                if (!SawimApplication.isManyPane()) {
                    chatBarLayout.getChatsImage().setImageDrawable(icMess);
                }
            }
            chatBarLayout.updateLabelIcon(chat.getContact().isConference() ? null : RosterAdapter.getImageChat(chat, false));
        }
    }

    private void updateRoster() {
        RosterView rosterView = (RosterView) getFragmentManager().findFragmentById(R.id.roster_fragment);
        if (rosterView != null)
            rosterView.update();
    }

    private void searchTextFromMessage() {
        final String query = getText().toLowerCase();
        if (query.isEmpty()) return;
        if (searchMessagesIds == null) {
            searchMessagesIds = chat.getHistory().getSearchMessagesIds(query);
        }
        final boolean idsIsNtEmpty = !searchMessagesIds.isEmpty();
        final int oldCount = getMessagesAdapter().getCount();
        if (idsIsNtEmpty) {
            if (searchPositionsCount == 0 || searchPositionsCount == searchMessagesIds.size()) {
                getMessagesAdapter().setQuery(query);
                searchPositionsCount = searchMessagesIds.size();
            }
            final int position = searchMessagesIds.get(searchPositionsCount - 1);
            SawimApplication.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    List<MessData> messDataList = null;
                    if (position > oldCount) {
                        messDataList = chat.getHistory().addNextListMessages(chat, position, oldCount);
                    }
                    final List<MessData> finalMessDataList = messDataList;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (finalMessDataList != null) {
                                    getMessagesAdapter().getItems().addAll(0, finalMessDataList);
                                }
                                getMessagesAdapter().notifyDataSetChanged();
                                MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
                                chatListView.setSelection(position);
                                searchPositionsCount--;
                            }
                        });
                    }
                }
            });
        } else {
            resetSearchMode(true);
        }
    }

    private void resetSearchMode(boolean showToast) {
        searchPositionsCount = 0;
        searchMessagesIds = null;
        getMessagesAdapter().setQuery(null);
        getMessagesAdapter().notifyDataSetChanged();
        if (showToast)
            Toast.makeText(getActivity().getApplicationContext(), R.string.not_found, Toast.LENGTH_LONG).show();
    }

    private void enableSearchMode() {
        isSearchMode = true;
        searchTextFromMessage();
        MyImageButton menuButton = chatViewLayout.getChatInputBarView().getMenuButton();
        MyImageButton smileButton = chatViewLayout.getChatInputBarView().getSmileButton();
        MyImageButton sendButton = chatViewLayout.getChatInputBarView().getSendButton();
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchTextFromMessage();
            }
        });
        chatViewLayout.getChatInputBarView().setImageButtons(menuButton, smileButton, sendButton, isSearchMode);
        getMessagesAdapter().notifyDataSetChanged();
    }

    private void destroySearchMode() {
        isSearchMode = false;
        resetSearchMode(false);
        MyImageButton menuButton = chatViewLayout.getChatInputBarView().getMenuButton();
        MyImageButton smileButton = chatViewLayout.getChatInputBarView().getSmileButton();
        MyImageButton sendButton = chatViewLayout.getChatInputBarView().getSendButton();
        sendButton.setOnClickListener(sendBtnClickListener);
        chatViewLayout.getChatInputBarView().setImageButtons(menuButton, smileButton, sendButton, isSearchMode);
        getMessagesAdapter().notifyDataSetChanged();
    }

    private void destroyMultiCitationMode() {
        Clipboard.setClipBoardText(getActivity(), null);
        getMessagesAdapter().setMultiQuoteMode(false);
        for (int i = 0; i < getMessagesAdapter().getCount(); ++i) {
            MessData messData = getMessagesAdapter().getItem(i);
            if (messData.isMarked()) {
                messData.setMarked(false);
            }
        }
        getMessagesAdapter().notifyDataSetChanged();
        getActivity().supportInvalidateOptionsMenu();
    }

    private StringBuilder buildQuote() {
        StringBuilder multiQuoteBuffer = new StringBuilder();
        for (int i = 0; i < getMessagesAdapter().getCount(); ++i) {
            MessData mData = getMessagesAdapter().getItem(i);
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

    private View.OnClickListener sendBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            send();
            closePane();
        }
    };

    private ListView.OnItemClickListener chatClick = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            final Protocol protocol = chat.getProtocol();
            final Contact contact = chat.getContact();
            MessData mData = (MessData) adapterView.getAdapter().getItem(position);
            String msg = mData.getText().toString();
            if (getMessagesAdapter().isMultiQuoteMode()) {
                mData.setMarked(!mData.isMarked());
                StringBuilder multiQuoteBuffer = buildQuote();
                Clipboard.setClipBoardText(getActivity(), 0 == multiQuoteBuffer.length() ? null : multiQuoteBuffer.toString());
                getMessagesAdapter().notifyDataSetChanged();
            } else {
                if (chat.isBlogBot()) {
                    new JuickMenu(getActivity(), protocol.getUserId(), contact.getUserId(), chat.getBlogPostId(msg)).show();
                    return;
                }
                if (mucUsersView != null) {
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
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (BaseActivity.getExternalApi().onActivityResult(requestCode, resultCode, data)) return;
        super.onActivityResult(requestCode, resultCode, data);
    }

    private MyMenu getMenu() {
        Contact contact = chat.getContact();
        final MyMenu menu = new MyMenu();
        boolean accessible = chat.getWritable() && (contact.isSingleUserContact() || contact.isOnline());
        menu.add(R.string.find, ContactMenu.CHAT_MENU_SEARCH);
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
        super.onPrepareOptionsMenu(menu);
    }

    public void onOptionsItemSelected_(MenuItem item) {
        onOptionsItemSelected(item.getItemId());
    }

    private void onOptionsItemSelected(int id) {
        final Protocol protocol = chat.getProtocol();
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
                new ContactMenu(protocol, contact).doAction((BaseActivity) getActivity(), ContactMenu.USER_MENU_FILE_TRANS);
                break;
            case ContactMenu.USER_MENU_CAM_TRANS:
                BaseActivity.getExternalApi().setFragment(this);
                new ContactMenu(protocol, contact).doAction((BaseActivity) getActivity(), ContactMenu.USER_MENU_CAM_TRANS);
                break;

            case ContactMenu.CONFERENCE_DISCONNECT:
                ChatHistory.instance.unregisterChat(chat);
                chat.setVisibleChat(false);
                RosterHelper.getInstance().setOnUpdateChat(null);
                chat = null;
                new ContactMenu(protocol, contact).doAction((BaseActivity) getActivity(), ContactMenu.CONFERENCE_DISCONNECT);
                if (!SawimApplication.isManyPane()) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
                break;

            default:
                new ContactMenu(protocol, contact).doAction((BaseActivity) getActivity(), id);
                getActivity().supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final Protocol protocol = chat.getProtocol();
        final Contact contact = chat.getContact();
        menu.add(Menu.FIRST, ContactMenu.MENU_COPY_TEXT, 0, android.R.string.copy);
        menu.add(Menu.FIRST, ContactMenu.ACTION_QUOTE, 0, R.string.quote);
        if (mucUsersView != null) {
            menu.add(Menu.FIRST, ContactMenu.COMMAND_PRIVATE, 0, R.string.open_private);
            menu.add(Menu.FIRST, ContactMenu.COMMAND_INFO, 0, R.string.info);
            menu.add(Menu.FIRST, ContactMenu.COMMAND_STATUS, 0, R.string.user_statuses);
        }
        if (protocol instanceof Xmpp) {
            menu.add(Menu.FIRST, ContactMenu.ACTION_TO_NOTES, 0, R.string.add_to_notes);
        }
        contact.addChatMenuItems(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (getMessagesAdapter() == null) return super.onContextItemSelected(item);
        final MessData md = getMessagesAdapter().getItem(info.position);
        if (md == null) return super.onContextItemSelected(item);
        final Protocol protocol = chat.getProtocol();
        final Contact contact = chat.getContact();
        String nick = md.getNick();
        CharSequence msg = md.getText();
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
                openChat(protocol, c);
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
        SawimApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (chat != null) {
                    chat.sendMessage(text);
                }
                MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
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
                                    MyListView chatListView = chatViewLayout.getChatListsView().getChatListView();
                                    chatListView.setSelectionFromTop(getMessagesAdapter().getCount(), -chat.offset);
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    public static void showAuthorizationDialog(final BaseActivity activity, final Chat newChat) {
        new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Protocol protocol = newChat.getProtocol();
                final Contact contact = newChat.getContact();
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                dialogBuilder.setInverseBackgroundForced(Util.isNeedToInverseDialogBackground());
                dialogBuilder.setMessage(JLocale.getString(R.string.grant) + " " + contact.getName() + "?");
                dialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ContactMenu(protocol, contact).doAction(activity, ContactMenu.USER_MENU_GRANT_AUTH);
                        activity.supportInvalidateOptionsMenu();
                        RosterHelper.getInstance().updateRoster();
                    }
                });
                dialogBuilder.setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new ContactMenu(protocol, contact).doAction(activity, ContactMenu.USER_MENU_DENY_AUTH);
                        activity.supportInvalidateOptionsMenu();
                        RosterHelper.getInstance().updateRoster();
                    }
                });
                Dialog dialog = dialogBuilder.create();
                dialog.setCanceledOnTouchOutside(true);
                return dialog;
            }
        }.show(activity.getSupportFragmentManager().beginTransaction(), "auth");
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
            final Protocol protocol = chat.getProtocol();
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
            if (isSearchMode) {
                String query = text.toLowerCase();
                if ((oldSearchQuery != null && !oldSearchQuery.isEmpty()) && query.isEmpty()) {
                    oldSearchQuery = query;
                    resetSearchMode(false);
                } else if (oldSearchQuery != null && !oldSearchQuery.equals(query)) {
                    oldSearchQuery = query;
                    resetSearchMode(false);
                    searchTextFromMessage();
                }
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
}