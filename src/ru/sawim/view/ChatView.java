package ru.sawim.view;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.jabber.Jabber;
import protocol.jabber.JabberServiceContact;
import protocol.jabber.Jid;
import protocol.jabber.MirandaNotes;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.models.ChatsSpinnerAdapter;
import ru.sawim.models.MessagesAdapter;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;
import ru.sawim.widget.chat.ChatBarView;
import ru.sawim.widget.chat.ChatInputBarView;
import ru.sawim.widget.chat.ChatListsView;
import ru.sawim.widget.chat.ChatViewRoot;
import sawim.Clipboard;
import sawim.Options;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.MessData;
import sawim.roster.Roster;
import sawim.util.JLocale;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.01.13
 * Time: 20:30
 * To change this template use File | Settings | File Templates.
 */
public class ChatView extends SawimFragment implements Roster.OnUpdateChat {

    public static final String TAG = "ChatView";
    private Chat chat;
    private Protocol protocol;
    private Contact contact;
    private static Protocol lastProtocol;
    private static Contact lastContact;
    private boolean sendByEnter;
    private static boolean isTablet;
    private String sharingText = "";

    private EditText messageEditor;
    private MyListView nickList;
    private MyListView chatListView;
    private ChatsSpinnerAdapter chatsSpinnerAdapter;
    private ChatListsView chatListsView;
    private ChatInputBarView chatInputBarView;
    private ChatViewRoot chat_viewLayout;
    private MucUsersView mucUsersView;
    private static final MessagesAdapter adapter = new MessagesAdapter();
    private DrawerLayout drawerLayout;

    private static ViewsState viewsState;
    public boolean isOpenMenu = false;

    private static class ViewsState {
        private ImageButton usersImage;
        private ImageButton chatsImage;
        private ImageButton menuButton;
        private ImageButton smileButton;
        private ImageButton sendButton;
        private ChatBarView chatBarLayout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        General.currentActivity = (ActionBarActivity) activity;
        isTablet = activity.findViewById(R.id.fragment_container) == null;
        messageEditor = new EditText(activity);

        if (viewsState == null) {
            viewsState = new ViewsState();
            viewsState.usersImage = new ImageButton(activity);
            viewsState.chatsImage = new ImageButton(activity);

            viewsState.menuButton = new ImageButton(activity);
            viewsState.smileButton = new ImageButton(activity);
            viewsState.sendButton = new ImageButton(activity);

            viewsState.chatBarLayout = new ChatBarView(activity, viewsState.usersImage, viewsState.chatsImage);
        }
        chatListView = new MyListView(activity);
        if (isTablet)
            nickList = new MyListView(getActivity());
        chatListsView = new ChatListsView(activity, isTablet, chatListView, nickList);
        chatInputBarView = new ChatInputBarView(activity, viewsState.menuButton, viewsState.smileButton, messageEditor, viewsState.sendButton);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            messageEditor.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_BACKGROUND), PorterDuff.Mode.MULTIPLY);
        }

        General.getInstance().setConfigurationChanged(new General.OnConfigurationChanged() {
            @Override
            public void onConfigurationChanged() {
                if (chat != null && adapter != null)
                    adapter.repaintList();
            }
        });
    }

    public void removeTitleBar() {
        if (viewsState.chatBarLayout != null && viewsState.chatBarLayout.getParent() != null)
            ((ViewGroup) viewsState.chatBarLayout.getParent()).removeView(viewsState.chatBarLayout);
    }

    public ChatBarView getTitleBar() {
        return viewsState.chatBarLayout;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceStateLog) {
        isTablet = getActivity().findViewById(R.id.fragment_container) == null;
        General.actionBar.setDisplayShowTitleEnabled(false);
        General.actionBar.setDisplayShowHomeEnabled(false);
        General.actionBar.setDisplayUseLogoEnabled(false);
        if (!isTablet) {
            removeTitleBar();
            General.actionBar.setDisplayShowCustomEnabled(true);
            General.actionBar.setCustomView(viewsState.chatBarLayout);
        }
        updateChatIcon();

        if (!isTablet)
            nickList = new MyListView(getActivity());
        if (!isTablet) {
            drawerLayout = new DrawerLayout(getActivity());
            DrawerLayout.LayoutParams nickListLP = new DrawerLayout.LayoutParams(Util.dipToPixels(getActivity(), 240), DrawerLayout.LayoutParams.MATCH_PARENT);
            DrawerLayout.LayoutParams drawerLayoutLP = new DrawerLayout.LayoutParams(DrawerLayout.LayoutParams.MATCH_PARENT, DrawerLayout.LayoutParams.MATCH_PARENT);
            drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
            drawerLayout.setScrimColor(Scheme.isBlack() ? 0x55FFFFFF : 0x99000000);
            nickListLP.gravity = Gravity.START;
            drawerLayout.setLayoutParams(drawerLayoutLP);

            TypedArray a = getActivity().getTheme().obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
            int background = a.getResourceId(0, 0);
            a.recycle();
            nickList.setBackgroundResource(background);
            nickList.setChoiceMode(MyListView.CHOICE_MODE_SINGLE);
            nickList.setLayoutParams(nickListLP);
        }

        if (chat_viewLayout == null)
            chat_viewLayout = new ChatViewRoot(getActivity(), isTablet, chatListsView, chatInputBarView);
        else
            ((ViewGroup) chat_viewLayout.getParent()).removeView(chat_viewLayout);
        if (!isTablet) {
            drawerLayout.addView(chat_viewLayout);
            drawerLayout.addView(nickList);
        }

        if (!Scheme.isSystemBackground()) {
            int background = Scheme.getColor(Scheme.THEME_BACKGROUND);
            chat_viewLayout.setBackgroundColor(background);
        }

        if (!isTablet) {
            viewsState.chatBarLayout.setVisibilityUsersImage(ImageView.VISIBLE);
            viewsState.usersImage.setBackgroundColor(0);
            viewsState.usersImage.setImageDrawable(General.usersIcon);
            viewsState.usersImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (nickList == null) return;
                    if (drawerLayout.isDrawerOpen(nickList)) {
                        drawerLayout.closeDrawer(nickList);
                    } else {
                        drawerLayout.openDrawer(nickList);
                    }
                }
            });
        } else
            viewsState.chatBarLayout.setVisibilityUsersImage(View.GONE);
        viewsState.chatsImage.setBackgroundColor(0);
        viewsState.chatsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forceGoToChat(ChatHistory.instance.getPreferredItem());
                RosterView rosterView = (RosterView) ChatView.this.getFragmentManager().findFragmentById(R.id.roster_fragment);
                if (rosterView != null)
                    rosterView.update();
            }
        });
        if (isTablet) {
            viewsState.menuButton.setVisibility(ImageButton.VISIBLE);
            viewsState.menuButton.setBackgroundColor(0);
            viewsState.menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (contact == null) return;
                    isOpenMenu = true;
                    getActivity().openOptionsMenu();
                    getActivity().supportInvalidateOptionsMenu();
                }
            });
        } else
            viewsState.menuButton.setVisibility(ImageButton.GONE);
        viewsState.smileButton.setBackgroundColor(0);
        viewsState.smileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(view);
                new SmilesView().show(General.currentActivity.getSupportFragmentManager(), "show-smiles");
            }
        });
        messageEditor.setSingleLine(false);
        messageEditor.setMaxLines(4);
        messageEditor.setHorizontallyScrolling(false);
        messageEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageEditor.setHint(R.string.hint_message);
        messageEditor.addTextChangedListener(textWatcher);
        messageEditor.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        if (sendByEnter) {
            messageEditor.setImeOptions(EditorInfo.IME_ACTION_SEND);
            messageEditor.setOnEditorActionListener(enterListener);
        }
        sendByEnter = Options.getBoolean(Options.OPTION_SIMPLE_INPUT);
        if (sendByEnter) {
            viewsState.sendButton.setVisibility(ImageButton.GONE);
        } else {
            viewsState.sendButton.setVisibility(ImageButton.VISIBLE);
            viewsState.sendButton.setBackgroundColor(0);
            viewsState.sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    send();
                    if (!isTablet)
                        if (drawerLayout.isDrawerVisible(nickList)) {
                            drawerLayout.closeDrawer(nickList);
                        }
                }
            });
            viewsState.sendButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    insert("/me ");
                    showKeyboard();
                    return true;
                }
            });
        }
        return isTablet ? chat_viewLayout : drawerLayout;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        viewsState.chatsImage.setOnClickListener(null);
        viewsState.menuButton.setOnClickListener(null);
        viewsState.usersImage.setOnClickListener(null);
        viewsState.smileButton.setOnClickListener(null);
        General.getInstance().setConfigurationChanged(null);
        messageEditor.addTextChangedListener(null);
        chatListView.setOnCreateContextMenuListener(null);
        chatListView.setOnItemClickListener(null);
        messageEditor = null;
        chatsSpinnerAdapter = null;
        viewsState.chatBarLayout.setOnClickListener(null);
        chatListView = null;
        nickList = null;
        chatListsView = null;
        chatInputBarView = null;
        mucUsersView = null;
        chat_viewLayout = null;
        protocol = null;
        contact = null;
        chat = null;
        sharingText = "";
        chatDialogFragment = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        General.currentActivity = (ActionBarActivity) getActivity();
        if (contact != null)
            openChat(protocol, contact);
        if (isTablet)
            if (lastContact == null)
                chat_viewLayout.showHint();
            else
                openChat(lastProtocol, lastContact);
        if (!isTablet)
            getActivity().supportInvalidateOptionsMenu();
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
        lastProtocol = protocol;
        lastContact = contact;
        initChat(protocol, contact);

        View item = chatListView.getChildAt(0);
        chat.scrollPosition = chatListView.getFirstVisiblePosition();
        chat.offset = (item == null) ? 0 : Math.abs(item.getBottom());
        chat.dividerPosition = chat.getMessData().size();
        chat.message = getText();

        chat.setVisibleChat(false);
        Roster.getInstance().setOnUpdateChat(null);
        chat.resetUnreadMessages();
        if (chat.empty()) ChatHistory.instance.unregisterChat(chat);
    }

    public void resume(Chat chat) {
        if (chat == null) return;
        chat.setVisibleChat(true);
        ChatHistory.instance.registerChat(chat);
        Roster.getInstance().setOnUpdateChat(this);
        chat.resetUnreadMessages();
        removeMessages(Options.getInt(Options.OPTION_MAX_MSG_COUNT));
        messageEditor.setText(chat.message + sharingText);
        adapter.setPosition(chat.dividerPosition);
        if (contact.isConference() && chat.scrollPosition == 0)
            chatListView.setSelection(0);
        else if (chat.getMessCount() > 0 && chat.scrollPosition == 0)
            chatListView.setSelection(chat.getMessCount());
        else
            chatListView.setSelectionFromTop(chat.scrollPosition + 2, chat.offset);
        updateChatIcon();
        updateList(contact);

        if (isTablet)
            adapter.repaintList();
        else
            drawerLayout.setDrawerLockMode(contact.isConference() ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void setSharingText(String sharingText) {
        this.sharingText = sharingText;
    }

    private void removeMessages(final int limit) {
        if (chat.getMessData().size() < limit) return;
        if ((0 < limit) && (0 < chat.getMessData().size())) {
            while (limit < chat.getMessData().size()) {
                chat.scrollPosition--;
                chat.dividerPosition--;
                chat.getMessData().remove(0);
            }
        } else ChatHistory.instance.unregisterChat(chat);
    }

    public boolean hasBack() {
        if (nickList != null && !isTablet)
            if (drawerLayout.isDrawerOpen(nickList)) {
                drawerLayout.closeDrawer(nickList);
                return false;
            }
        return true;
    }

    private void forceGoToChat(int position) {
        Chat current = ChatHistory.instance.chatAt(position);
        if (current == null) return;
        pause(chat);
        openChat(current.getProtocol(), current.getContact());
        resume(current);
    }

    public void initChat(Protocol p, Contact c) {
        protocol = p;
        contact = c;
    }

    public void openChat(Protocol p, Contact c) {
        chat_viewLayout.hideHint();
        initChat(p, c);
        chat = protocol.getChat(contact);

        initLabel();
        initList();
        initMucUsers();
    }

    public Chat getCurrentChat() {
        return chat;
    }

    DialogFragment chatDialogFragment;
    private void initLabel() {
        chatsSpinnerAdapter = new ChatsSpinnerAdapter(getActivity());
        viewsState.chatBarLayout.updateLabelIcon(chatsSpinnerAdapter.getImageChat(chat, false));
        viewsState.chatBarLayout.updateTextView(contact.getName());
        viewsState.chatBarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatDialogFragment = new DialogFragment() {
                    @Override
                    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                             Bundle savedInstanceState) {
                        getDialog().setCanceledOnTouchOutside(true);
                        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                        setStyle(STYLE_NO_FRAME, R.style.BaseThemeLight);
                        View v = inflater.inflate(R.layout.chats_dialog, container, false);
                        MyListView lv = (MyListView) v.findViewById(R.id.listView);
                        if (!Scheme.isSystemBackground())
                            lv.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
                        lv.setAdapter(chatsSpinnerAdapter);
                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Object o = parent.getAdapter().getItem(position);
                                if (o instanceof Chat) {
                                    Chat current = (Chat) o;
                                    pause(chat);
                                    openChat(current.getProtocol(), current.getContact());
                                    resume(current);
                                    dismiss();
                                    RosterView rosterView = (RosterView) ChatView.this.getFragmentManager().findFragmentById(R.id.roster_fragment);
                                    if (rosterView != null)
                                        rosterView.update();
                                }
                            }
                        });
                        return v;
                    }
                };
                chatDialogFragment.show(getFragmentManager().beginTransaction(), "force go to chat");
                chatsSpinnerAdapter.refreshList();
            }
        });
    }

    private void initList() {
        adapter.init(chat);
        chatListView.setAdapter(adapter);
        chatListView.setStackFromBottom(true);
        chatListView.setFastScrollEnabled(true);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        chatListView.setOnCreateContextMenuListener(this);
        chatListView.setOnItemClickListener(chatClick);
        chatListView.setFocusable(true);
    }

    private void initMucUsers() {
        if (isTablet)
            nickList.setVisibility(View.VISIBLE);
        else if (drawerLayout.isDrawerOpen(nickList))
            drawerLayout.closeDrawer(nickList);

        if (contact instanceof JabberServiceContact && contact.isConference()) {
            mucUsersView = new MucUsersView();
            mucUsersView.init(protocol, (JabberServiceContact) contact);
            mucUsersView.show(this, nickList);
            viewsState.chatBarLayout.setVisibilityUsersImage(isTablet ? View.GONE : View.VISIBLE);
        } else {
            viewsState.chatBarLayout.setVisibilityUsersImage(View.GONE);
            if (isTablet) {
                nickList.setVisibility(View.GONE);
            } else {
                if (drawerLayout.isDrawerOpen(nickList)) {
                    drawerLayout.closeDrawer(nickList);
                }
            }
        }
    }

    public void updateChatIcon() {
        Icon icMess = ChatHistory.instance.getUnreadMessageIcon();
        if (icMess == null) {
            viewsState.chatBarLayout.setVisibilityChatsImage(View.GONE);
        } else {
            viewsState.chatBarLayout.setVisibilityChatsImage(View.VISIBLE);
            viewsState.chatsImage.setImageDrawable(icMess.getImage());
        }
    }

    @Override
    public void updateChat(final Contact contact) {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateChatIcon();
                updateList(contact);
            }
        });
    }

    private void updateList(Contact contact) {
        if (contact == this.contact) {
            if (chatsSpinnerAdapter != null) {
                viewsState.chatBarLayout.updateLabelIcon(chatsSpinnerAdapter.getImageChat(chat, false));
            }
            if (adapter != null)
                adapter.refreshList(chat.getMessData());
        }
        if (chatsSpinnerAdapter != null && chatDialogFragment != null && chatDialogFragment.isVisible()) {
            chatsSpinnerAdapter.refreshList();
        }
    }

    @Override
    public void updateMucList() {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (contact != null && contact.isPresence() == (byte) 1)
                    if (adapter != null)
                        adapter.refreshList(chat.getMessData());
                if (mucUsersView != null)
                    mucUsersView.update();
            }
        });
    }

    private ListView.OnItemClickListener chatClick = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            MessData mData = (MessData) adapterView.getAdapter().getItem(position);
            if (adapter.isMultiQuote()) {
                mData.setMarked(!mData.isMarked());
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < chat.getMessData().size(); ++i) {
                    MessData messData = chat.getMessageDataByIndex(i);
                    if (messData.isMarked()) {
                        String msg = messData.getText();
                        if (messData.isMe())
                            msg = "*" + messData.getNick() + " " + msg;
                        sb.append(Clipboard.serialize(false, messData.isIncoming(), messData.getNick() + " " + messData.strTime, msg));
                        sb.append("\n-----\n");
                    }
                }
                Clipboard.setClipBoardText(0 == sb.length() ? null : sb.toString());
                adapter.notifyDataSetChanged();
            } else {
                if (contact instanceof JabberServiceContact) {
                    JabberServiceContact jabberServiceContact = ((JabberServiceContact) contact);
                    if (jabberServiceContact.getContact(mData.getNick()) == null && !jabberServiceContact.getName().equals(mData.getNick())) {
                        Toast.makeText(General.currentActivity, getString(R.string.contact_walked), Toast.LENGTH_LONG).show();
                    }
                }
                setText(chat.onMessageSelected(mData));
                if (isTablet) {
                    if (nickList.getVisibility() == View.VISIBLE && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                        nickList.setVisibility(View.GONE);
                } else {
                    if (drawerLayout.isDrawerOpen(nickList) && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                        drawerLayout.closeDrawer(nickList);
                }
            }
        }
    };

    public void onPrepareOptionsMenu_(Menu menu) {
        menu.clear();
        if (chat == null) return;
        boolean accessible = chat.getWritable() && (contact.isSingleUserContact() || contact.isOnline());
        menu.add(Menu.FIRST, ContactMenu.MENU_MULTI_CITATION, 2, getString(adapter.isMultiQuote() ?
                R.string.disable_multi_citation : R.string.include_multi_citation));
        if (0 < chat.getAuthRequestCounter()) {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_GRANT_AUTH, 2, JLocale.getString("grant"));
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_DENY_AUTH, 2, JLocale.getString("deny"));
        }
        if (!contact.isAuth()) {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_REQU_AUTH, 2, JLocale.getString("requauth"));
        }
        if (accessible) {
            if (sawim.modules.fs.FileSystem.isSupported()) {
                menu.add(Menu.FIRST, ContactMenu.USER_MENU_FILE_TRANS, 2, JLocale.getString("ft_name"));
            }
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_CAM_TRANS, 2, JLocale.getString("ft_cam"));
        }
        menu.add(Menu.FIRST, ContactMenu.USER_MENU_STATUSES, 2, General.currentActivity.getResources().getString(R.string.user_statuses));
        if (!contact.isSingleUserContact() && contact.isOnline()) {
            menu.add(Menu.FIRST, ContactMenu.CONFERENCE_DISCONNECT, 2, JLocale.getString("leave_chat"));
        }
        menu.add(Menu.FIRST, ContactMenu.ACTION_CURRENT_DEL_CHAT, 2, JLocale.getString("delete_chat"));
        menu.add(Menu.FIRST, ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR, 2, JLocale.getString("all_contact_except_this"));
        menu.add(Menu.FIRST, ContactMenu.ACTION_DEL_ALL_CHATS, 2, JLocale.getString("all_contacts"));
        super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected_(MenuItem item) {
        switch (item.getItemId()) {
            case ContactMenu.MENU_MULTI_CITATION:
                if (adapter.isMultiQuote()) {
                    adapter.setMultiQuote(false);
                } else {
                    adapter.setMultiQuote(true);
                    Toast.makeText(General.currentActivity, R.string.hint_multi_citation, Toast.LENGTH_LONG).show();
                }
                adapter.notifyDataSetChanged();
                getActivity().supportInvalidateOptionsMenu();
                break;

            case ContactMenu.ACTION_CURRENT_DEL_CHAT:
                ChatHistory.instance.unregisterChat(chat);
                if (General.currentActivity.findViewById(R.id.fragment_container) != null)
                    getFragmentManager().popBackStack();
                else
                    chat_viewLayout.setVisibility(LinearLayout.GONE);
                break;

            case ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR:
                ChatHistory.instance.removeAll(chat);
                break;

            case ContactMenu.ACTION_DEL_ALL_CHATS:
                ChatHistory.instance.removeAll(null);
                if (General.currentActivity.findViewById(R.id.fragment_container) != null)
                    getFragmentManager().popBackStack();
                else
                    chat_viewLayout.setVisibility(LinearLayout.GONE);
                break;

            default:
                new ContactMenu(protocol, contact).doAction(item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.FIRST, ContactMenu.MENU_COPY_TEXT, 0, android.R.string.copy);
        menu.add(Menu.FIRST, ContactMenu.ACTION_QUOTE, 0, JLocale.getString("quote"));
        if (contact instanceof JabberServiceContact && contact.isConference()) {
            menu.add(Menu.FIRST, ContactMenu.COMMAND_PRIVATE, 0, R.string.open_private);
            menu.add(Menu.FIRST, ContactMenu.COMMAND_INFO, 0, R.string.info);
            menu.add(Menu.FIRST, ContactMenu.COMMAND_STATUS, 0, R.string.user_statuses);
        }
        if (protocol instanceof Jabber) {
            menu.add(Menu.FIRST, ContactMenu.ACTION_TO_NOTES, 0, R.string.add_to_notes);
        }
        if (!Options.getBoolean(Options.OPTION_HISTORY) && chat.hasHistory()) {
            menu.add(Menu.FIRST, ContactMenu.ACTION_ADD_TO_HISTORY, 0, JLocale.getString("add_to_history"));
        }
        contact.addChatMenuItems(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        MessData md = adapter.getItem(info.position);
        String nick = md.getNick();
        String msg = md.getText();
        switch (item.getItemId()) {
            case ContactMenu.MENU_COPY_TEXT:
                if (null == md) {
                    return false;
                }
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                Clipboard.setClipBoardText(msg + "\n");
                break;

            case ContactMenu.ACTION_QUOTE:
                StringBuffer sb = new StringBuffer();
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                sb.append(Clipboard.serialize(true, md.isIncoming(), md.getNick() + " " + md.strTime, msg));
                sb.append("\n-----\n");
                Clipboard.setClipBoardText(0 == sb.length() ? null : sb.toString());
                break;

            case ContactMenu.COMMAND_PRIVATE:
                String jid = Jid.realJidToSawimJid(contact.getUserId() + "/" + nick);
                JabberServiceContact c = (JabberServiceContact) protocol.getItemByUIN(jid);
                if (null == c) {
                    c = (JabberServiceContact) protocol.createTempContact(jid);
                    protocol.addTempContact(c);
                }
                pause(getCurrentChat());
                openChat(protocol, c);
                resume(getCurrentChat());
                break;
            case ContactMenu.COMMAND_INFO:
                protocol.showUserInfo(((JabberServiceContact) contact).getPrivateContact(nick));
                break;
            case ContactMenu.COMMAND_STATUS:
                protocol.showStatus(((JabberServiceContact) contact).getPrivateContact(nick));
                break;

            case ContactMenu.ACTION_ADD_TO_HISTORY:
                chat.addTextToHistory(md);
                break;

            case ContactMenu.ACTION_TO_NOTES:
                MirandaNotes notes = ((Jabber) protocol).getMirandaNotes();
                notes.showIt();
                MirandaNotes.Note note = notes.addEmptyNote();
                note.tags = md.getNick() + " " + md.strTime;
                note.text = md.getText();
                notes.showNoteEditor(note);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void showKeyboard(View view) {
        Configuration conf = Resources.getSystem().getConfiguration();
        if (conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO) {
            InputMethodManager keyboard = (InputMethodManager) General.currentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) General.currentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showKeyboard() {
        messageEditor.requestFocus();
        showKeyboard(messageEditor);
    }

    @Override
    public void pastText(final String text) {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                insert(" " + text + " ");
                showKeyboard();
            }
        });
    }

    private void send() {
        if (chat == null) return;
        hideKeyboard(messageEditor);
        chat.sendMessage(getText());
        resetText();
        adapter.setPosition(-1);
        updateList(contact);
        for (int i = 0; i < chat.getMessData().size(); ++i) {
            MessData messData = chat.getMessageDataByIndex(i);
            if (messData.isMarked()) {
                messData.setMarked(false);
            }
        }
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
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageEditor.setText("");
            }
        });
    }

    private String getText() {
        return messageEditor.getText().toString();
    }

    private void setText(final String text) {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String t = null == text ? "" : text;
                if ((0 == t.length()) || !canAdd(t)) {
                    messageEditor.setText(t);
                    messageEditor.setSelection(t.length());
                } else {
                    insert(t);
                }
                showKeyboard();
            }
        });
    }

    private boolean hasText() {
        return 0 < messageEditor.getText().length();
    }

    public void insert(String text) {
        int start = messageEditor.getSelectionStart();
        int end = messageEditor.getSelectionEnd();
        messageEditor.getText().replace(Math.min(start, end), Math.max(start, end),
                text, 0, text.length());
    }

    private boolean isDone(int actionId) {
        return (EditorInfo.IME_NULL == actionId)
                || (EditorInfo.IME_ACTION_DONE == actionId)
                || (EditorInfo.IME_ACTION_SEND == actionId);
    }

    private boolean compose = false;
    private TextWatcher textWatcher = new TextWatcher() {
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
            if (sendByEnter && (start + count <= s.length()) && (1 == count)) {
                boolean enter = ('\n' == s.charAt(start));
                if (enter) {
                    messageEditor.setText(previousText);
                    messageEditor.setSelection(start);
                    send();
                }
            }
            if (lineCount != messageEditor.getLineCount()) {
                lineCount = messageEditor.getLineCount();
                messageEditor.requestLayout();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (protocol == null) return;
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
    };

    private final TextView.OnEditorActionListener enterListener = new android.widget.TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(android.widget.TextView textView, int actionId, KeyEvent event) {
            if (isDone(actionId)) {
                if ((null == event) || (event.getAction() == KeyEvent.ACTION_DOWN)) {
                    send();
                    return true;
                }
            }
            return false;
        }
    };
}