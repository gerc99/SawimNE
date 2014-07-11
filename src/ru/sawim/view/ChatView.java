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
import ru.sawim.activities.SawimActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.models.MessagesAdapter;
import ru.sawim.models.RosterAdapter;
import ru.sawim.modules.history.HistoryStorage;
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

    private static final int ADD_MESSAGE = 0;
    private static final int UPDATE_MESSAGES = 1;
    private static final int UPDATE_CHAT = 3;
    private static final int UPDATE_MUC_LIST = 4;
    private static final int LOAD_STORY = 5;
    private Handler handler;

    private Chat chat;
    private String oldChat;
    private static String lastChat;
    private boolean isOldChat;
    private int unreadMessageCount;
    private String sharingText;
    private boolean sendByEnter;
    private static int offsetNewMessage;

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
                    final MyMenu menu = new MyMenu(activity);
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
                    menu.add(R.string.user_statuses, ContactMenu.USER_MENU_STATUSES);

                    menu.add(R.string.delete_chat, ContactMenu.ACTION_CURRENT_DEL_CHAT);
                    menu.add(R.string.all_contact_except_this, ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR);
                    menu.add(R.string.clear_all_contacts, ContactMenu.ACTION_DEL_ALL_CHATS);
                    if (!contact.isSingleUserContact() && contact.isOnline()) {
                        menu.add(R.string.leave_chat, ContactMenu.CONFERENCE_DISCONNECT);
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
        sendByEnter = Options.getBoolean(Options.OPTION_SIMPLE_INPUT);
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
    public void onStart() {
        super.onStart();
        Contact currentContact = RosterHelper.getInstance().getCurrentContact();
        if (chat == null && !SawimApplication.isManyPane()) {
            chat = currentContact.getProtocol().getChat(currentContact);
        }
        if (chat == null) {
            if (currentContact != null)
                initChat(currentContact.getProtocol(), currentContact);
        } else {
            openChat(chat.getProtocol(), chat.getContact());
        }
        if (SawimApplication.isManyPane()) {
            if (chat == null)
                chatViewLayout.showHint();
        } else {
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
        boolean isOldChatInTablet = chat != null && chat.getContact() == RosterHelper.getInstance().getCurrentContact() && SawimApplication.isManyPane();
        if (isOldChatInTablet) return;
        resume(chat);
    }

    public void pause(Chat chat) {
        if (chat == null) return;
        initChat(chat.getProtocol(), chat.getContact());
        chat.message = getText().length() == 0 ? null : getText();

        if (adapter != null) {
            if (chat.getMessCount() == 0) {
                chat.dividerPosition = adapter.getCount();
            } else {
                chat.dividerPosition = adapter.getItems().indexOf(chat.getMessData().get(chat.getMessCount() - 1)) + 1;
            }
            chat.oldMessageCount = adapter.getCount();
        }
        chat.firstVisiblePosition = chatListView.getFirstVisiblePosition();
        chat.lastVisiblePosition = chatListView.getLastVisiblePosition() + 1;
        boolean isBottomScroll = chat.lastVisiblePosition == chat.dividerPosition;
        if (chat.lastVisiblePosition == chat.oldMessageCount) {
            unreadMessageCount = 0;
            chat.oldMessageCount = 0;
        }
        View item = chatListView.getChildAt(0);
        chat.offset = (item == null) ? 0 : Math.abs(isBottomScroll ? item.getTop() : item.getBottom());
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
        removeMessages(Options.getInt(Options.OPTION_MAX_MSG_COUNT) + unreadMessageCount);
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
        boolean hasHistory = chat.getHistory() != null && chat.getHistory().getFirstMessageCount() > 0;
        if (chat.empty() || hasHistory) {
            loadStory(false);
        } else {
            adapter.addAll(chat.getMessData());
            setPosition();
        }
    }

    private void setPosition() {
        adapter.setPosition(chat.dividerPosition);
        boolean hasUnreadMessages = unreadMessageCount > 0;
        boolean isBottomScroll = chat.lastVisiblePosition == chat.dividerPosition;
        int position = chat.getMessCount() - unreadMessageCount;
        if (isBottomScroll && !hasUnreadMessages) {
            chatListView.setSelectionFromTop(chat.firstVisiblePosition, -chat.offset);
        } else if (!hasUnreadMessages || !isBottomScroll) {
            chatListView.setSelectionFromTop(chat.firstVisiblePosition + 1, chat.offset);
        } else {
            if (isBottomScroll) {
                chatListView.setSelectionFromTop(position, offsetNewMessage);
            }
        }
    }

    public boolean isLastPosition() {
        return chat != null && chatListView.getLastVisiblePosition() == adapter.getCount() - 1;
    }

    public void setSharingText(String text) {
        sharingText = text;
    }

    private void removeMessages(final int limit) {
        if (chat.getMessCount() < limit) return;
        boolean isBottomScroll = chat.lastVisiblePosition == chat.dividerPosition;
        if (0 < limit && 0 < chat.getMessCount() && isBottomScroll) {
            while (limit < chat.getMessCount()) {
                if (chat.dividerPosition > 0) {
                    chat.dividerPosition--;
                    chat.lastVisiblePosition--;
                }
                chat.getMessData().remove(0);
            }
        }
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

        lastChat = chat.getContact().getUserId();
        isOldChat = oldChat != null && oldChat.equals(lastChat);

        if (isOldChat) {
            chat.oldMessageCount = 0;
            for (int i = chat.getUnreadMessageCount(); i > 0; --i) {
                adapter.getItems().add(chat.getMessData().get(chat.getMessCount() - i));
            }
            adapter.notifyDataSetChanged();
            return;
        }
        oldChat = chat.getContact().getUserId();
        ChatHistory.instance.registerChat(chat);
        initLabel();
        initList();
        initMucUsers();
    }

    public Chat getCurrentChat() {
        return chat;
    }

    public static String getLastChat() {
        return lastChat;
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
            boolean isScroll;
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                isScroll = scrollState != -1;
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (visibleItemCount > 0 && firstVisibleItem == 0) {
                    loadStory(isScroll);
                }
            }
        });
    }

    private void initMucUsers() {
        if (SawimApplication.isManyPane())
            nickList.setVisibility(View.VISIBLE);
        else if (drawerLayout.isDrawerOpen(nickList))
            drawerLayout.closeDrawer(nickList);
        boolean isConference = chat.getContact() instanceof XmppServiceContact && chat.getContact().isConference();
        if (isConference) {
            mucUsersView = new MucUsersView();
            mucUsersView.init(chat.getProtocol(), (XmppServiceContact) chat.getContact());
            mucUsersView.show(this, nickList);
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
                    adapter.add(mess);
                }
                break;
            case UPDATE_MESSAGES:
                Contact contact = (Contact) msg.obj;
                if (chat != null && chat.getContact() == contact) {
                    adapter.notifyDataSetChanged();
                    if (chatListView.getLastVisiblePosition() + 1 == adapter.getCount() - 1) {
                        chatListView.setSelectionFromTop(adapter.getCount(), offsetNewMessage);
                        chat.oldMessageCount = 0;
                    }
                }
                break;
            case UPDATE_CHAT:
                updateChatIcon();
                if (chatsSpinnerAdapter != null && chatsDialogFragment != null && chatsDialogFragment.isVisible())
                    chatsSpinnerAdapter.refreshList();
                break;
            case UPDATE_MUC_LIST:
                if (mucUsersView != null) {
                    if (SawimApplication.isManyPane()
                            || (drawerLayout != null && nickList != null && drawerLayout.isDrawerOpen(nickList))) {
                        mucUsersView.update();
                    }
                }
                break;
            case LOAD_STORY:
                boolean isScroll = (boolean) msg.obj;
                if (chat != null && adapter != null) {
                    HistoryStorage historyStorage = chat.getHistory();
                    if (historyStorage != null) {
                        int historySize = historyStorage.getHistorySize();
                        int oldCount = adapter.getCount();
                        if (historySize > 0 && historySize != oldCount) {
                            boolean hasUnreadMessages = unreadMessageCount > 0;
                            boolean isBottomScroll = chat.lastVisiblePosition == chat.dividerPosition;
                            int limit = HISTORY_MESSAGES_LIMIT;
                            if (chat.oldMessageCount > 0) {
                                limit = chat.oldMessageCount;
                            } else if (hasUnreadMessages && (isBottomScroll || 0 == chat.dividerPosition) && !isScroll) {
                                limit = unreadMessageCount;
                            }
                            boolean isNewChat = !isOldChat;
                            if (isOldChat) {
                                isOldChat = false;
                                limit = 0;
                                oldCount = 0;
                            }
                            historyStorage.addNextListMessages(adapter.getItems(), chat, limit, oldCount);
                            int position = adapter.getCount() - unreadMessageCount;
                            adapter.setPosition(position);
                            adapter.notifyDataSetChanged();
                            if (isScroll) {
                                chatListView.setSelection(adapter.getCount() - oldCount + 1);
                            } else {
                                if (hasUnreadMessages) {
                                    chatListView.setSelectionFromTop(position, offsetNewMessage);
                                } else {
                                    if (isBottomScroll || chat.oldMessageCount == 0 && isNewChat) {
                                        chatListView.setSelectionFromTop(position, offsetNewMessage);
                                    } else {
                                        chatListView.setSelectionFromTop(chat.firstVisiblePosition + 1, chat.offset);
                                    }
                                }
                            }
                        }
                    }
                }
                break;
        }
        return false;
    }

    @Override
    public void addMessage(Contact contact, MessData mess) {
        handler.sendMessage(Message.obtain(handler, ADD_MESSAGE, new Object[]{contact, mess}));
    }

    @Override
    public void updateMessages(Contact contact) {
        handler.sendMessage(Message.obtain(handler, UPDATE_MESSAGES, contact));
    }

    @Override
    public void updateChat() {
        handler.sendMessage(Message.obtain(handler, UPDATE_CHAT));
    }

    @Override
    public void updateMucList() {
        handler.sendEmptyMessage(UPDATE_MUC_LIST);
    }

    private void loadStory(boolean isScroll) {
        handler.sendMessage(Message.obtain(handler, LOAD_STORY, isScroll));
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
        for (int i = 0; i < chat.getMessData().size(); ++i) {
            MessData messData = chat.getMessageDataByIndex(i);
            if (messData.isMarked()) {
                messData.setMarked(false);
            }
        }
    }

    private StringBuilder buildQuote() {
        StringBuilder multiQuoteBuffer = new StringBuilder();
        for (int i = 0; i < chat.getMessData().size(); ++i) {
            MessData mData = chat.getMessageDataByIndex(i);
            CharSequence msg = mData.getText();
            if (mData.isMarked()) {
                if (mData.isMe())
                    msg = "*" + mData.getNick() + " " + msg;
                String msgSerialize = Clipboard.serialize(false, mData.isIncoming(), mData.getNick(), mData.strTime, msg);
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
                    new JuickMenu(getActivity(), protocol, contact.getUserId(), chat.getBlogPostId(msg)).show();
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
        if (SawimActivity.externalApi.onActivityResult(requestCode, resultCode, data)) return;
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
        menu.add(Menu.FIRST, ContactMenu.USER_MENU_STATUSES, 2, R.string.user_statuses);

        menu.add(Menu.FIRST, ContactMenu.ACTION_CURRENT_DEL_CHAT, 2, R.string.delete_chat);
        menu.add(Menu.FIRST, ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR, 2, R.string.all_contact_except_this);
        menu.add(Menu.FIRST, ContactMenu.ACTION_DEL_ALL_CHATS, 2, R.string.clear_all_contacts);
        if (!contact.isSingleUserContact() && contact.isOnline()) {
            menu.add(Menu.FIRST, ContactMenu.CONFERENCE_DISCONNECT, 2, R.string.leave_chat);
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
                BaseActivity.externalApi.setFragment(this);
                new ContactMenu(protocol, contact).doAction((BaseActivity) getActivity(), ContactMenu.USER_MENU_FILE_TRANS);
                break;
            case ContactMenu.USER_MENU_CAM_TRANS:
                BaseActivity.externalApi.setFragment(this);
                new ContactMenu(protocol, contact).doAction((BaseActivity) getActivity(), ContactMenu.USER_MENU_CAM_TRANS);
                break;

            case ContactMenu.ACTION_CURRENT_DEL_CHAT:
                ChatHistory.instance.unregisterChat(chat);
                chat.setVisibleChat(false);
                RosterHelper.getInstance().setOnUpdateChat(null);
                chat = null;
                if (SawimApplication.isManyPane()) {
                    adapter.getItems().clear();
                    updateMessages(contact);
                } else {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
                break;

            case ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR:
                ChatHistory.instance.removeAll(chat);
                if (SawimApplication.isManyPane()) {
                    adapter.getItems().clear();
                    updateMessages(contact);
                }
                break;

            case ContactMenu.ACTION_DEL_ALL_CHATS:
                ChatHistory.instance.removeAll(null);
                chat.setVisibleChat(false);
                RosterHelper.getInstance().setOnUpdateChat(null);
                chat = null;
                if (SawimApplication.isManyPane()) {
                    adapter.getItems().clear();
                    updateMessages(contact);
                } else {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
                break;

            case ContactMenu.CONFERENCE_DISCONNECT:
                ChatHistory.instance.unregisterChat(chat);
                chat.setVisibleChat(false);
                RosterHelper.getInstance().setOnUpdateChat(null);
                chat = null;
                new ContactMenu(protocol, contact).doAction((BaseActivity) getActivity(), ContactMenu.CONFERENCE_DISCONNECT);
                if (SawimApplication.isManyPane()) {
                    adapter.getItems().clear();
                    updateMessages(contact);
                } else {
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
        if (!Options.getBoolean(Options.OPTION_HISTORY)) {
            menu.add(Menu.FIRST, ContactMenu.ACTION_ADD_TO_HISTORY, 0, R.string.add_to_history);
        }
        contact.addChatMenuItems(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final MessData md = adapter.getItem(info.position);
        if (md == null) return super.onContextItemSelected(item);
        final Protocol protocol = chat.getProtocol();
        final Contact contact = chat.getContact();
        String nick = md.getNick();
        CharSequence msg = md.getText();
        switch (item.getItemId()) {
            case ContactMenu.MENU_COPY_TEXT:
                if (null == md) {
                    return false;
                }
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                Clipboard.setClipBoardText(getActivity(), msg + "\n");
                Toast.makeText(getActivity(), R.string.hint_citation, Toast.LENGTH_LONG).show();
                break;

            case ContactMenu.ACTION_QUOTE:
                StringBuilder sb = new StringBuilder();
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                sb.append(Clipboard.serialize(true, md.isIncoming(), md.getNick(), md.strTime, msg));
                sb.append(MESS_DIVIDER);
                Clipboard.setClipBoardText(getActivity(), 0 == sb.length() ? null : sb.toString());
                Toast.makeText(getActivity(), R.string.hint_citation, Toast.LENGTH_LONG).show();
                break;

            case ContactMenu.COMMAND_PRIVATE:
                String jid = Jid.realJidToSawimJid(contact.getUserId() + "/" + nick);
                XmppServiceContact c = (XmppServiceContact) protocol.getItemByUIN(jid);
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
                protocol.showStatus(((XmppServiceContact) contact).getPrivateContact(nick));
                break;

            case ContactMenu.ACTION_ADD_TO_HISTORY:
                chat.addTextToHistory(md);
                break;

            case ContactMenu.ACTION_TO_NOTES:
                MirandaNotes notes = ((Xmpp) protocol).getMirandaNotes();
                notes.showIt();
                MirandaNotes.Note note = notes.addEmptyNote();
                note.tags = md.getNick() + " " + md.strTime;
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
        if (Options.getBoolean(Options.OPTION_HIDE_KEYBOARD) && messageEditor != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(messageEditor.getWindowToken(), 0);
        }
    }

    @Override
    public void pastText(final String text) {
        setText(" " + text + " ");
        showKeyboard();
    }

    public void send() {
        if (chat == null) return;
        hideKeyboard();
        chat.sendMessage(getText());
        resetText();
        chat.message = null;
        adapter.setPosition(-1);
        chatListView.post(new Runnable() {
            @Override
            public void run() {
                if (chatListView.getLastVisiblePosition() + 1 == adapter.getCount()) {
                    chatListView.setSelection(adapter.getCount() - 1);
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