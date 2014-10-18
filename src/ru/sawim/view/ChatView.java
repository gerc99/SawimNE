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
import android.support.v4.app.ActionBarDrawerToggle;
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
import ru.sawim.listener.OnUpdateChat;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.xmpp.Jid;
import protocol.xmpp.MirandaNotes;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppServiceContact;
import ru.sawim.*;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.models.MessagesAdapter;
import ru.sawim.models.RosterAdapter;
import ru.sawim.roster.RosterHelper;
import ru.sawim.text.TextFormatter;
import ru.sawim.view.menu.JuickMenu;
import ru.sawim.view.menu.MyMenu;
import ru.sawim.widget.FixedEditText;
import ru.sawim.widget.MyImageButton;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;
import ru.sawim.widget.chat.ChatBarView;
import ru.sawim.widget.chat.ChatInputBarView;
import ru.sawim.widget.chat.ChatListsView;
import ru.sawim.widget.chat.ChatViewRoot;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.01.13
 * Time: 20:30
 * To change this template use File | Settings | File Templates.
 */
public class ChatView extends SawimFragment implements OnUpdateChat, Handler.Callback {

    public static final String TAG = ChatView.class.getSimpleName();

    private static final int HISTORY_MESSAGES_LIMIT = 20;

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
    private static int offsetNewMessage;
    private int newMessageCount = 0;

    private RosterAdapter chatsSpinnerAdapter;
    private MessagesAdapter adapter;
    private EditText messageEditor;
    private MyListView nickList;
    private MyListView chatListView;
    private ChatListsView chatListsView;
    private ChatInputBarView chatInputBarView;
    private ChatViewRoot chatViewLayout;
    private SmileysPopup smileysPopup;
    private MucUsersView mucUsersView;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private MyImageButton chatsImage;
    private MyImageButton menuButton;
    private MyImageButton smileButton;
    private MyImageButton sendButton;
    private ChatBarView chatBarLayout;
    private DialogFragment chatsDialogFragment;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        handler = new Handler(this);
        chatsImage = new MyImageButton(activity);

        menuButton = new MyImageButton(activity);
        smileButton = new MyImageButton(activity);
        sendButton = new MyImageButton(activity);
        messageEditor = new FixedEditText(activity);

        chatBarLayout = new ChatBarView(activity, chatsImage);
        chatListView = new MyListView(activity);
        nickList = new MyListView(activity);
        chatListsView = new ChatListsView(activity, SawimApplication.isManyPane(), chatListView, nickList);
        chatInputBarView = new ChatInputBarView(activity, menuButton, smileButton, messageEditor, sendButton);
        chatViewLayout = new ChatViewRoot(activity, chatListsView, chatInputBarView);
        smileysPopup = new SmileysPopup(activity, chatViewLayout, messageEditor);
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
        updateChatIcon();
        if (drawerLayout != null && drawerLayout.getParent() != null) {
            ((ViewGroup) drawerLayout.getParent()).removeView(drawerLayout);
        }
        if (chatViewLayout.getParent() != null) {
            ((ViewGroup) chatViewLayout.getParent()).removeView(chatViewLayout);
        }
        if (Scheme.isSystemBackground()) {
            chatViewLayout.setBackgroundResource(Util.getSystemBackground(activity));
        } else {
            chatViewLayout.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        }
        chatBarLayout.update();
        chatViewLayout.update();
        chatListsView.update();
        chatInputBarView.setImageButtons(menuButton, smileButton, sendButton);
        if (!SawimApplication.isManyPane()) {
            DrawerLayout.LayoutParams nickListLP = new DrawerLayout.LayoutParams(Util.dipToPixels(activity, 240), DrawerLayout.LayoutParams.MATCH_PARENT);
            DrawerLayout.LayoutParams drawerLayoutLP = new DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT);
            drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
            drawerLayout.setScrimColor(Scheme.isBlack() ? 0x55FFFFFF : 0x99000000);
            nickListLP.gravity = Gravity.START;
            drawerLayout.setLayoutParams(drawerLayoutLP);
            nickList.setBackgroundResource(Util.getSystemBackground(activity));
            nickList.setLayoutParams(nickListLP);
            if (nickList.getParent() != null) {
                ((ViewGroup) nickList.getParent()).removeView(nickList);
            }
            drawerLayout.addView(chatViewLayout);
            drawerLayout.addView(nickList);
            drawerToggle = new ActionBarDrawerToggle(activity, drawerLayout, R.drawable.ic_drawer, 0, 0) {
                public void onDrawerClosed(View view) {
                }

                public void onDrawerOpened(View view) {
                    if (mucUsersView != null)
                        mucUsersView.update();
                }
            };
            drawerLayout.setDrawerListener(drawerToggle);
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerToggle.syncState();
        }
        chatsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (0 < chat.getAuthRequestCounter()) {
                    new DialogFragment() {
                        @Override
                        public Dialog onCreateDialog(Bundle savedInstanceState) {
                            final Protocol protocol = chat.getProtocol();
                            final Contact contact = chat.getContact();
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                            dialogBuilder.setInverseBackgroundForced(Util.isNeedToInverseDialogBackground());
                            dialogBuilder.setMessage(JLocale.getString(R.string.grant) + " " + contact.getName() + "?");
                            dialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new ContactMenu(protocol, contact).doAction(activity, ContactMenu.USER_MENU_GRANT_AUTH);
                                    activity.supportInvalidateOptionsMenu();
                                    updateRoster();
                                }
                            });
                            dialogBuilder.setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new ContactMenu(protocol, contact).doAction(activity, ContactMenu.USER_MENU_DENY_AUTH);
                                    activity.supportInvalidateOptionsMenu();
                                    updateRoster();
                                }
                            });
                            Dialog dialog = dialogBuilder.create();
                            dialog.setCanceledOnTouchOutside(true);
                            return dialog;
                        }
                    }.show(getFragmentManager().beginTransaction(), "auth");
                } else {
                    forceGoToChat(ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem()));
                }
            }
        });
        if (SawimApplication.isManyPane()) {
            menuButton.setVisibility(ImageButton.VISIBLE);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (chat == null) return;
                    Contact contact = chat.getContact();
                    final MyMenu menu = new MyMenu();
                    boolean accessible = chat.getWritable() && (contact.isSingleUserContact() || contact.isOnline());
                    menu.add(getString(adapter.isMultiQuote() ?
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
                smileysPopup.show();
            }
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            messageEditor.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
            messageEditor.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
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
                smileysPopup.hide();
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
                if (i == KeyEvent.KEYCODE_BACK && smileysPopup != null && smileysPopup.isShown()) {
                    if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        smileysPopup.hide();
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
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    send();
                    closePane();
                }
            });
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
            if (adapter.isEmpty()) {
                ChatHistory.instance.unregisterChat(chat);
            }
            chat = null;
        }
        handler = null;
        chatsImage = null;
        menuButton = null;
        smileButton = null;
        sendButton = null;
        messageEditor = null;
        chatBarLayout = null;
        chatListView = null;
        nickList = null;
        chatListsView = null;
        chatInputBarView = null;
        chatViewLayout = null;
        smileysPopup = null;
        drawerLayout = null;
        adapter = null;
        mucUsersView = null;
        chatsSpinnerAdapter = null;
        chatsDialogFragment = null;
    }

    public boolean hasBack() {
        return !closePane();
    }

    private boolean closePane() {
        if (nickList != null && !SawimApplication.isManyPane()) {
            if (drawerLayout.isDrawerOpen(nickList)) {
                drawerLayout.closeDrawer(nickList);
                return true;
            }
        }
        if (smileysPopup != null) {
            return smileysPopup.hide();
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
        if (savedInstanceState != null) {
            Protocol protocol = RosterHelper.getInstance().getProtocol(savedInstanceState.getString(PROTOCOL_ID));
            Contact contact = protocol.getItemByUID(savedInstanceState.getString(CONTACT_ID));
            if (contact != null) {
                chat = protocol.getChat(contact);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Contact currentContact = RosterHelper.getInstance().getCurrentContact();
        if (chat == null && currentContact != null && !SawimApplication.isManyPane()) {
            chat = currentContact.getProtocol().getChat(currentContact);
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
        chat.message = getText().length() == 0 ? null : getText();

        chat.currentPosition = adapter.getCount();
        chat.firstVisiblePosition = chatListView.getFirstVisiblePosition();
        if (isScrollEnd()) {
            unreadMessageCount = 0;
            chat.currentPosition = 0;
        }
        View item = chatListView.getChildAt(0);
        chat.offset = (item == null) ? 0 : Math.abs(isScrollEnd() ? item.getTop() : item.getBottom());
        offsetNewMessage = chatListView.getHeight() / 4;

        chat.setVisibleChat(false);
        RosterHelper.getInstance().setOnUpdateChat(null);
        chat.resetUnreadMessages();
    }

    public void resume(Chat chat) {
        resetBar();
        if (chat == null) return;
        chat.setVisibleChat(true);
        RosterHelper.getInstance().setOnUpdateChat(this);
        unreadMessageCount = chat.getUnreadMessageCount();
        chat.resetUnreadMessages();
        if (sharingText != null) {
            if (null != chat.message) {
                chat.message += " " + sharingText;
            } else {
                chat.message = sharingText;
            }
        }
        messageEditor.setText(chat.message);
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
    }

    public boolean isScrollEnd() {
        return chatListView != null && !chatListView.canScrollVertically(0);
    }

    public void setSharingText(String text) {
        sharingText = text;
    }

    private void forceGoToChat(Chat current) {
        if (current == null) return;
        pause(chat);
        chatListView.stopScroll();
        openChat(current.getProtocol(), current.getContact());
        resume(current);
        getActivity().supportInvalidateOptionsMenu();
        updateRoster();
    }

    public void initChat(Protocol p, Contact c) {
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
    }

    public Chat getCurrentChat() {
        return chat;
    }

    private void initLabel() {
        chatsSpinnerAdapter = new RosterAdapter();
        chatsSpinnerAdapter.setType(RosterHelper.ACTIVE_CONTACTS);
        chatBarLayout.updateLabelIcon(chatsSpinnerAdapter.getImageChat(chat, false));
        chatBarLayout.updateTextView(chat.getContact().getName());
        chatBarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatsDialogFragment = new DialogFragment() {
                    @Override
                    public Dialog onCreateDialog(Bundle savedInstanceState) {
                        final Context context = getActivity();
                        View dialogView = LayoutInflater.from(context).inflate(R.layout.chats_dialog, null);
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                        dialogBuilder.setView(dialogView);
                        dialogBuilder.setInverseBackgroundForced(Util.isNeedToInverseDialogBackground());
                        MyListView lv = (MyListView) dialogView.findViewById(R.id.listView);
                        lv.setAdapter(chatsSpinnerAdapter);
                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Object o = parent.getAdapter().getItem(position);
                                if (o instanceof Chat) {
                                    forceGoToChat((Chat) o);
                                    dismiss();
                                }
                            }
                        });
                        Dialog dialog = dialogBuilder.create();
                        dialog.setCanceledOnTouchOutside(true);
                        return dialog;
                    }
                };
                chatsDialogFragment.show(getFragmentManager().beginTransaction(), "force-go-to-chat");
                chatsSpinnerAdapter.refreshList();
            }
        });
    }

    private void initList() {
        adapter = new MessagesAdapter();
        chatListView.setAdapter(adapter);
        chatListView.setStackFromBottom(true);
        chatListView.setFastScrollEnabled(true);
        chatListView.setOnCreateContextMenuListener(this);
        chatListView.setOnItemClickListener(chatClick);
        chatListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            int oldFirstVisibleItem = -1;
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
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (oldFirstVisibleItem != firstVisibleItem) {
                    oldFirstVisibleItem = firstVisibleItem;
                    if (visibleItemCount > 0 && firstVisibleItem == 0) {
                        loadStory(isScroll, false);
                    }
                } else {
                    oldFirstVisibleItem = -1;
                }
                boolean isScrollEnd_ = isScrollEnd();
                if (isScrollEnd != isScrollEnd_) {
                    isScrollEnd = isScrollEnd_;
                    chatListsView.setShowDividerForUnreadMessage(!isScrollEnd);
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
            mucUsersView.init(chat.getContact().getMyName());
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

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case ADD_MESSAGE:
                Contact c = (Contact) ((Object[]) msg.obj)[0];
                MessData mess = (MessData) ((Object[]) msg.obj)[1];
                if (chat != null && chat.getContact() == c) {
                    adapter.getItems().add(mess);
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

    private void setScroll() {
        boolean isScrollEnd = isScrollEnd();
        adapter.notifyDataSetChanged();
        if (isScrollEnd) {
            chatListView.setSelectionFromTop(adapter.getCount() - newMessageCount, chatListView.getHeight() / 4);
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
    public void updateChat() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateChatIcon();
                if (chatsSpinnerAdapter != null && chatsDialogFragment != null && chatsDialogFragment.isVisible())
                    chatsSpinnerAdapter.refreshList();
            }
        });
    }

    @Override
    public void updateMucList() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mucUsersView != null) {
                    if (SawimApplication.isManyPane()
                            || (drawerLayout != null && nickList != null && drawerLayout.isDrawerOpen(nickList))) {
                        mucUsersView.update();
                    }
                }
            }
        });
    }

    private void loadStory(boolean isScroll, boolean isLoad) {
        if (isOldChat) {
            isOldChat = false;
            return;
        }
        if (chat != null && adapter != null) {
            boolean hasUnreadMessages = unreadMessageCount > 0;
            boolean isBottomScroll = chat.currentPosition == 0;
            boolean isFirstOpenChat = chat.currentPosition == -2;
            int limit = HISTORY_MESSAGES_LIMIT;
            if (chat.currentPosition > 0) {
                limit = chat.currentPosition;
            } else if (unreadMessageCount > 0) {
                limit += unreadMessageCount;
            }

            boolean isAdded = false;
            int oldCount = adapter.getCount();
            if (!isScroll && isLoad) {
                isAdded = chat.getHistory().addNextListMessages(adapter.getItems(), chat, limit, oldCount, true);
            } else if (isScroll && !isLoad) {
                isAdded = chat.getHistory().addNextListMessages(adapter.getItems(), chat, HISTORY_MESSAGES_LIMIT, oldCount, true);
            }
            int newCount = adapter.getItems().size();
            if (isAdded && (isLoad || isScroll)) {
                int position = newCount - unreadMessageCount;
                adapter.setPosition(position);
                adapter.notifyDataSetChanged();
                if (isScroll && !isLoad) {
                    chatListView.setSelection(newCount == oldCount ? 0 : newCount - oldCount + 1);
                } else {
                    if (hasUnreadMessages) {
                        if (isFirstOpenChat || isBottomScroll) {
                            chatListView.setSelectionFromTop(position, offsetNewMessage);
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
    }

    private void addUnreadMessagesFromHistoryToList() {
        if (unreadMessageCount == 0) return;
        if (isOldChat) {
            isOldChat = false;
        }
        boolean isFirstOpenChat = chat.currentPosition == -2;
        boolean isBottomScroll = chat.currentPosition == 0;
        adapter.setPosition(adapter.getCount());
        boolean isAdded = chat.getHistory().addNextListMessages(adapter.getItems(), chat, unreadMessageCount, 0, false);
        if (isAdded) {
            adapter.notifyDataSetChanged();
            if (isBottomScroll && !isFirstOpenChat) {
                chatListView.setSelectionFromTop(adapter.getCount(), offsetNewMessage);
            }
            chat.currentPosition = -1;
            unreadMessageCount = 0;
        }
    }

    private void updateChatIcon() {
        if (chat == null || chatBarLayout == null) return;
        Drawable icMess = ChatHistory.instance.getUnreadMessageIcon();
        BitmapDrawable confIcon = StatusInfo.STATUS_OFFLINE == chat.getContact().getStatusIndex()
                ? SawimResources.usersIcon : SawimResources.usersIconOn;
        if (SawimApplication.isManyPane()) {
            if (chatsSpinnerAdapter != null) {
                chatBarLayout.updateLabelIcon(chat.getContact().isConference() ? confIcon : chatsSpinnerAdapter.getImageChat(chat, false));
            }
        } else {
            ((BaseActivity) getActivity()).getSupportActionBar().setIcon(confIcon);
            if (icMess == null) {
                chatBarLayout.setVisibilityChatsImage(View.GONE);
            } else {
                chatBarLayout.setVisibilityChatsImage(View.VISIBLE);
                chatsImage.setImageDrawable(icMess);
            }
            if (chatsSpinnerAdapter != null) {
                chatBarLayout.updateLabelIcon(chat.getContact().isConference() ? null : chatsSpinnerAdapter.getImageChat(chat, false));
            }
        }
    }

    private void updateRoster() {
        RosterView rosterView = (RosterView) getFragmentManager().findFragmentById(R.id.roster_fragment);
        if (rosterView != null)
            rosterView.update();
    }

    private static final String MESS_DIVIDER = "\n---\n";

    private void destroyMultiCitation() {
        Clipboard.setClipBoardText(getActivity(), null);
        adapter.setMultiQuote(false);
        for (int i = 0; i < adapter.getCount(); ++i) {
            MessData messData = adapter.getItem(i);
            if (messData.isMarked()) {
                messData.setMarked(false);
            }
        }
    }

    private StringBuilder buildQuote() {
        StringBuilder multiQuoteBuffer = new StringBuilder();
        for (int i = 0; i < adapter.getCount(); ++i) {
            MessData mData = adapter.getItem(i);
            CharSequence msg = mData.getText();
            if (mData.isMarked()) {
                if (mData.isMe())
                    msg = "*" + mData.getNick() + " " + msg;
                String msgSerialize = Clipboard.serialize(false, mData.isIncoming(), mData.getNick(), mData.getStrTime(), msg);
                multiQuoteBuffer.append(msgSerialize);
                multiQuoteBuffer.append(MESS_DIVIDER);
            }
        }
        return multiQuoteBuffer;
    }

    private ListView.OnItemClickListener chatClick = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            final Protocol protocol = chat.getProtocol();
            final Contact contact = chat.getContact();
            MessData mData = (MessData) adapterView.getAdapter().getItem(position);
            String msg = mData.getText().toString();
            if (adapter.isMultiQuote()) {
                mData.setMarked(!mData.isMarked());
                StringBuilder multiQuoteBuffer = buildQuote();
                Clipboard.setClipBoardText(getActivity(), 0 == multiQuoteBuffer.length() ? null : multiQuoteBuffer.toString());
                adapter.notifyDataSetChanged();
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
                    setText(chat.onMessageSelected(mData));
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

    public void onPrepareOptionsMenu_(Menu menu) {
        if (chat == null) return;
        final Contact contact = chat.getContact();
        menu.clear();
        boolean accessible = chat.getWritable() && (contact.isSingleUserContact() || contact.isOnline());
        menu.add(Menu.FIRST, ContactMenu.MENU_MULTI_CITATION, 2, getString(adapter.isMultiQuote() ?
                R.string.disable_multi_citation : R.string.include_multi_citation));
        if (0 < chat.getAuthRequestCounter()) {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_GRANT_AUTH, 2, R.string.grant);
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_DENY_AUTH, 2, R.string.deny);
        }
        if (!contact.isAuth()) {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_REQU_AUTH, 2, R.string.requauth);
        }
        if (accessible) {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_FILE_TRANS, 2, R.string.ft_name);
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_CAM_TRANS, 2, R.string.ft_cam);
        }

        if (contact.isSingleUserContact()) {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_STATUSES, 2, R.string.user_statuses);
        } else {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_STATUSES, 2, R.string.conference_theme);
            if (contact.isOnline()) {
                menu.add(Menu.FIRST, ContactMenu.CONFERENCE_DISCONNECT, 2, R.string.leave_chat);
            }
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
            case ContactMenu.MENU_MULTI_CITATION:
                if (adapter.isMultiQuote()) {
                    destroyMultiCitation();
                } else {
                    adapter.setMultiQuote(true);
                    Toast.makeText(getActivity(), R.string.hint_multi_citation, Toast.LENGTH_LONG).show();
                }
                adapter.notifyDataSetChanged();
                getActivity().supportInvalidateOptionsMenu();
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
        if (adapter == null) return super.onContextItemSelected(item);
        final MessData md = adapter.getItem(info.position);
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
                    sb.append(MESS_DIVIDER);
                } else {
                    sb.append(Clipboard.serialize(true, md.isIncoming(), md.getNick(), md.getStrTime(), msg));
                    sb.append(MESS_DIVIDER);
                }
                Clipboard.setClipBoardText(getActivity(), 0 == sb.length() ? null : sb.toString());
                Toast.makeText(getActivity(), R.string.hint_citation, Toast.LENGTH_LONG).show();
                break;

            case ContactMenu.COMMAND_PRIVATE:
                String jid = Jid.realJidToSawimJid(contact.getUserId() + "/" + nick);
                XmppServiceContact c = (XmppServiceContact) protocol.getItemByUID(jid);
                if (null == c) {
                    c = (XmppServiceContact) protocol.createTempContact(jid);
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
        SawimApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (chat != null) {
                    chat.sendMessage(getText());
                }
                if (chatListView != null) {
                    chatListView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (chat != null) {
                                resetText();
                                chat.message = null;
                                adapter.setPosition(-1);
                                chat.currentPosition = 0;
                                newMessageCount = 0;
                                adapter.notifyDataSetChanged();
                                if (isScrollEnd) {
                                    chatListView.setSelectionFromTop(adapter.getCount(), -chat.offset);
                                }
                            }
                        }
                    });
                }
            }
        });
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
        messageEditor.setText("");
    }

    private String getText() {
        return messageEditor.getText().toString();
    }

    private void setText(final String text) {
        String t = null == text ? "" : text;
        if ((0 == t.length()) || !canAdd(t)) {
            messageEditor.setText(t);
            messageEditor.setSelection(t.length());
        } else {
            insert(t);
        }
    }

    public void insert(String text) {
        if (messageEditor == null) return;
        int start = messageEditor.getSelectionStart();
        int end = messageEditor.getSelectionEnd();
        messageEditor.getText().replace(Math.min(start, end), Math.max(start, end),
                text, 0, text.length());
    }

    private boolean compose = false;
    private final TextWatcher textWatcher = new TextWatcher() {
        private String previousText;
        private int lineCount = 0;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (sendByEnter) {
                previousText = s.toString();
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            /*if (sendByEnter && (start + count <= s.length()) && (1 == count)) {
                boolean enter = ('\n' == s.charAt(start));
                if (enter) {
                    messageEditor.setText(previousText);
                    messageEditor.setSelection(start);
                    send();
                    if (lineCount != messageEditor.getLineCount()) {
                        lineCount = messageEditor.getLineCount();
                        messageEditor.requestLayout();
                    }
                    return;
                }
            }*/
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
            if (adapter != null && adapter.isMultiQuote()) {
                String clipBoardText = Clipboard.getClipBoardText(getActivity());
                if (clipBoardText != null && text.equals(clipBoardText)) {
                    destroyMultiCitation();
                    adapter.notifyDataSetChanged();
                    getActivity().supportInvalidateOptionsMenu();
                }
            }
            TextFormatter.getInstance().detectEmotions(s);
        }
    };
}