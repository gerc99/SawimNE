package ru.sawim.view;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.jabber.*;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.models.ChatsSpinnerAdapter;
import ru.sawim.models.MessagesAdapter;
import ru.sawim.view.menu.MyMenu;
import sawim.Clipboard;
import sawim.FileTransfer;
import sawim.Options;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.MessData;
import ru.sawim.Scheme;
import sawim.roster.Roster;
import sawim.util.JLocale;

import java.util.Hashtable;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.01.13
 * Time: 20:30
 * To change this template use File | Settings | File Templates.
 */
public class ChatView extends SawimFragment implements Roster.OnUpdateChat {

    public static final String TAG = "ChatView";
    public static final String PROTOCOL_ID = "protocol_id";
    public static final String CONTACT_ID = "contact_id";
    private Chat chat;
    private Protocol protocol;
    private Contact contact;
    private static Protocol lastProtocol;
    private static Contact lastContact;
    private boolean sendByEnter;

    private ImageButton usersImage;
    private ImageButton chatsImage;
    private ImageButton menuButton;
    private ImageButton smileButton;
    private ImageButton sendButton;
    private EditText messageEditor;
    private MyListView nickList;
    private MyListView chatListView;
    private MySpinner spinner;
    private ChatViewRoot chat_viewLayout;
    private ChatBarView chatBarLayout;
    private ChatListsView chatListsView;
    private ChatInputBarView chatInputBarView;
    private MucUsersView mucUsersView;
    private MessagesAdapter adapter;
    private ChatsSpinnerAdapter chatsSpinnerAdapter;

    public static Hashtable<String, ScrollState> positionHash = new Hashtable<String, ScrollState>();
    public static Hashtable<String, State> chatHash = new Hashtable<String, State>();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        usersImage = new ImageButton(activity);
        spinner = new MySpinner(activity);
        chatsImage = new ImageButton(activity);

        menuButton = new ImageButton(activity);
        smileButton = new ImageButton(activity);
        messageEditor = new EditText(activity);
        sendButton = new ImageButton(activity);

        chatListView = new MyListView(activity);
        nickList = new MyListView(activity);

        chatBarLayout = new ChatBarView(getActivity());
        chatListsView = new ChatListsView(getActivity());
        chatInputBarView = new ChatInputBarView(getActivity());

        chatBarLayout.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {Scheme.getColor(Scheme.THEME_CAP_BACKGROUND), Scheme.getColor(Scheme.THEME_BACKGROUND)}));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            messageEditor.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_BACKGROUND), PorterDuff.Mode.MULTIPLY);
        }
    }

    private class ChatViewRoot extends LinearLayout {

        public ChatViewRoot(Context context) {
            super(context);
            setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
            setOrientation(LinearLayout.VERTICAL);
            setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            addViewInLayout(chatBarLayout, 0, chatBarLayout.getLayoutParams());
            addViewInLayout(chatListsView, 1, chatListsView.getLayoutParams());
            addViewInLayout(chatInputBarView, 2, chatInputBarView.getLayoutParams());
        }
    }

    private class ChatBarView extends LinearLayout {

        public ChatBarView(Context context) {
            super(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, General.dipToPixels(getActivity(), 50));
            setOrientation(HORIZONTAL);
            setLayoutParams(layoutParams);

            LinearLayout.LayoutParams usersImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            View usersImageDivider = General.getDivider(getActivity(), Scheme.getColor(Scheme.THEME_BACKGROUND));
            usersImageLP.gravity = Gravity.CENTER_VERTICAL;
            usersImage.setMinimumWidth(76);
            addViewInLayout(usersImage, 0, usersImageLP);
            addViewInLayout(usersImageDivider, 1, usersImageDivider.getLayoutParams());

            LinearLayout.LayoutParams spinnerLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            spinnerLP.gravity = Gravity.CENTER_HORIZONTAL;
            spinnerLP.weight = 1;
            addViewInLayout(spinner, 2, spinnerLP);

            LinearLayout.LayoutParams chatsImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            View chatsImageDivider = General.getDivider(getActivity(), Scheme.getColor(Scheme.THEME_BACKGROUND));
            chatsImageLP.gravity = Gravity.CENTER_VERTICAL;
            chatsImage.setMinimumWidth(76);
            addViewInLayout(chatsImageDivider, 3, chatsImageDivider.getLayoutParams());
            addViewInLayout(chatsImage, 4, chatsImageLP);
        }
    }

    private class ChatListsView extends LinearLayout {

        public ChatListsView(Context context) {
            super(context);
            rebuild();
        }

        public void rebuild() {
            removeAllViews();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            setOrientation(HORIZONTAL);
            layoutParams.weight = 2;
            setLayoutParams(layoutParams);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && getResources().getDisplayMetrics().densityDpi < 200)
                lp.weight = 10;
            else
                lp.weight = 1;
            lp.bottomMargin = 8;
            addViewInLayout(chatListView, 0, lp);

            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && getResources().getDisplayMetrics().densityDpi < 200)
                lp.weight = 0;
            else
                lp.weight = (float) 1.5;
            lp.bottomMargin = 8;
            addViewInLayout(nickList, 1, lp);
        }
    }

    private class ChatInputBarView extends LinearLayout {

        public ChatInputBarView(Context context) {
            super(context);
            init();
        }

        private void init() {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            setOrientation(HORIZONTAL);
            setPadding(5, 4, 4, 4);
            setLayoutParams(layoutParams);

            LinearLayout.LayoutParams menuButtonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            menuButton.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            addViewInLayout(menuButton, 0, menuButtonLP);

            LinearLayout.LayoutParams smileButtonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            smileButton.setImageResource(R.drawable.input_smile_button);
            addViewInLayout(smileButton, 1, smileButtonLP);

            LinearLayout.LayoutParams messageEditorLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            messageEditorLP.gravity = Gravity.CENTER | Gravity.LEFT;
            messageEditorLP.weight = (float) 0.87;
            messageEditor.setSingleLine(false);
            messageEditor.setPadding(2, 2, 2, 2);
            messageEditor.setMaxLines(4);
            messageEditor.setHorizontallyScrolling(false);
            messageEditor.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            messageEditor.setHint(R.string.hint_message);
            addViewInLayout(messageEditor, 2, messageEditorLP);

            LinearLayout.LayoutParams sendButtonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            sendButton.setImageResource(R.drawable.input_send_button);
            addViewInLayout(sendButton, 3, sendButtonLP);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceStateLog) {
        if (chat_viewLayout == null)
            chat_viewLayout = new ChatViewRoot(getActivity());
        else
            ((ViewGroup)chat_viewLayout.getParent()).removeView(chat_viewLayout);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            usersImage.setBackgroundDrawable(new ColorDrawable(0));
            usersImage.setImageDrawable(General.usersIcon);
            usersImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (nickList == null) return;
                    Animation inAnimation = AnimationUtils.makeInAnimation(General.sawimActivity, false);
                    Animation outAnimation = AnimationUtils.makeOutAnimation(General.sawimActivity, true);
                    inAnimation.setDuration(200);
                    outAnimation.setDuration(200);
                    nickList.startAnimation(nickList.getVisibility() == View.VISIBLE
                            ? outAnimation : inAnimation);
                    nickList.setVisibility(nickList.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
            });
        } else
            usersImage.setVisibility(ImageButton.GONE);
        chatsImage.setBackgroundDrawable(new ColorDrawable(0));
        chatsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetSpinner();
                forceGoToChat(ChatHistory.instance.getPreferredItem());
            }
        });

        int background = Scheme.getColor(Scheme.THEME_BACKGROUND);
        if (General.isTablet(getActivity())) {
            menuButton.setVisibility(ImageButton.VISIBLE);
            menuButton.setBackgroundColor(background);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMenu();
                }
            });
        } else {
            menuButton.setVisibility(ImageButton.GONE);
        }
        smileButton.setBackgroundColor(background);
        smileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(view);
                new SmilesView().show(getActivity().getSupportFragmentManager(), "show-smiles");
            }
        });
        messageEditor.addTextChangedListener(textWatcher);
        messageEditor.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        if (sendByEnter) {
            messageEditor.setImeOptions(EditorInfo.IME_ACTION_SEND);
            messageEditor.setOnEditorActionListener(enterListener);
        }
        sendByEnter = Options.getBoolean(Options.OPTION_SIMPLE_INPUT);
        if (sendByEnter) {
            sendButton.setVisibility(ImageButton.GONE);
        } else {
            sendButton.setVisibility(ImageButton.VISIBLE);
            sendButton.setBackgroundColor(background);
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    send();
                    if (nickList.getVisibility() == View.VISIBLE && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                        nickList.setVisibility(View.GONE);
                }
            });
            sendButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    insert("/me ");
                    showKeyboard();
                    return true;
                }
            });
        }
        return chat_viewLayout;
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle args = getArguments();
        if (args != null) {
            Protocol protocol = Roster.getInstance().getProtocol(args.getString(PROTOCOL_ID));
            Contact contact = protocol.getItemByUIN(args.getString(CONTACT_ID));
            openChat(protocol, contact);
        }
        if (General.sawimActivity.findViewById(R.id.fragment_container) == null) {
            if (lastContact == null)
                chat_viewLayout.setVisibility(LinearLayout.GONE);
            else
                openChat(lastProtocol, lastContact);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chat == null) return;
        if (chat.empty()) ChatHistory.instance.unregisterChat(chat);
    }

    public void pause(Chat chat) {
        if (chat == null) return;
        lastProtocol = protocol;
        lastContact = contact;
        Bundle args = getArguments();
        if (args != null) {
            args.putString(ChatView.PROTOCOL_ID, protocol.getUserId());
            args.putString(ChatView.CONTACT_ID, contact.getUserId());
        }
        chat.message = getText();

        View item = chatListView.getChildAt(0);
        addLastPosition(chat.getContact().getUserId(), chatListView.getFirstVisiblePosition(), (item == null) ? 0 : Math.abs(item.getBottom()));

        chat.setVisibleChat(false);
        Roster.getInstance().setOnUpdateChat(null);
        chat.resetUnreadMessages();
    }

    public void resume(Chat chat) {
        if (chat == null) return;
    //    chatListsView.rebuild();
        ScrollState lastPosition = getLastPosition(chat.getContact().getUserId());
        if (lastPosition != null && lastPosition.position > 0) {
            chatListView.setSelectionFromTop(lastPosition.position + 1, lastPosition.offset);
        } else chatListView.setSelection(0);

        messageEditor.setText(chat.message);
        chat.setVisibleChat(true);
        ChatHistory.instance.registerChat(chat);
        Roster.getInstance().setOnUpdateChat(this);
        chat.resetUnreadMessages();
        updateChatIcon();
        updateList(contact);
    }

    public boolean hasBack() {
        if (nickList != null)
            if (nickList.getVisibility() == View.VISIBLE) {
                nickList.setVisibility(View.GONE);
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

    private void addLastPosition(String jid, int position, int offset) {
        ScrollState scrollState = new ScrollState();
        scrollState.position = position;
        scrollState.offset = offset;
        positionHash.put(jid, scrollState);
    }

    private ScrollState getLastPosition(String jid) {
        if (positionHash.containsKey(jid)) return positionHash.get(jid);
        else return null;
    }

    public void openChat(Protocol p, Contact c) {
        if (General.sawimActivity.findViewById(R.id.fragment_container) == null)
            chat_viewLayout.setVisibility(LinearLayout.VISIBLE);
        protocol = p;
        contact = c;
        chat = protocol.getChat(contact);

        if (spinner.getOnItemSelectedEvenIfUnchangedListener() == null) initSpinner();
        initList();
        initMucUsers();
    }

    public void resetSpinner() {
        spinner.setOnItemSelectedEvenIfUnchangedListener(null);
    }

    private void initSpinner() {
        chatsSpinnerAdapter = new ChatsSpinnerAdapter(General.sawimActivity, ChatHistory.instance.historyTable);
        spinner.setAdapter(chatsSpinnerAdapter);
        spinner.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_CAP_BACKGROUND), PorterDuff.Mode.MULTIPLY);
        spinner.setSelection(ChatHistory.instance.getItemChat(contact));
        spinner.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                forceGoToChat(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void initList() {
        State chatState = chatHash.get(contact.getUserId());
        if (chatState == null) {
            chatState = new State();
            adapter = new MessagesAdapter();
            adapter.init(General.sawimActivity, chat);
            chatState.adapter = adapter;
            chatHash.put(contact.getUserId(), chatState);
        } else {
            adapter = chatState.adapter;
        }
        chatListView.setAdapter(adapter);
        chatListView.setFastScrollEnabled(true);
        chatListView.setStackFromBottom(true);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        chatListView.setOnCreateContextMenuListener(this);
        chatListView.setOnItemClickListener(new ChatClick());
        chatListView.setFocusable(true);
    }

    private void initMucUsers() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            nickList.setVisibility(View.GONE);
        else
            nickList.setVisibility(View.VISIBLE);
        nickList.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        if (contact instanceof JabberServiceContact && contact.isConference()) {
            mucUsersView = new MucUsersView();
            mucUsersView.init(protocol, (JabberServiceContact) contact);
            mucUsersView.show(this, nickList);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                usersImage.setVisibility(View.VISIBLE);
            else
                usersImage.setVisibility(View.GONE);
        } else {
            usersImage.setVisibility(View.GONE);
            nickList.setVisibility(View.GONE);
        }
    }

    private void updateChatIcon() {
        Icon icMess = ChatHistory.instance.getUnreadMessageIcon();
        if (icMess == null) {
            chatsImage.setVisibility(ImageView.GONE);
        } else {
            chatsImage.setVisibility(ImageView.VISIBLE);
            chatsImage.setImageDrawable(icMess.getImage());
        }
    }

    @Override
    public void updateChat(final Contact contact) {
        General.sawimActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateChatIcon();
                updateList(contact);
            }
        });
    }

    private void updateList(Contact contact) {
        if (chatsSpinnerAdapter != null)
            chatsSpinnerAdapter.refreshList(ChatHistory.instance.historyTable);
        if (contact == this.contact)
            if (adapter != null)
                adapter.refreshList(chat.getMessData());
    }

    @Override
    public void updateMucList() {
        General.sawimActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mucUsersView != null)
                    mucUsersView.update();
            }
        });
    }

    public static class ScrollState {
        public int position;
        public int offset;
    }

    public static class State {
        public MessagesAdapter adapter;
    }

    private StringBuffer sb = new StringBuffer();
    private class ChatClick implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            MessData mData = (MessData) adapterView.getAdapter().getItem(position);
            if (adapter.isMultiСitation()) {
                mData.setMarked(!mData.isMarked());
                String msg = mData.getText();
                if (mData.isMe()) {
                    msg = "*" + mData.getNick() + " " + msg;
                }
                if (mData.isMarked()) {
                    sb.append(Clipboard.serialize(mData.isIncoming(), mData.getNick() + " " + mData.strTime, msg));
                    sb.append("\n-----\n");
                }
                Clipboard.setClipBoardText(0 == sb.length() ? null : sb.toString());
                adapter.notifyDataSetChanged();
            } else {
                if (contact instanceof JabberServiceContact) {
                    JabberServiceContact jabberServiceContact = ((JabberServiceContact) contact);
                    if (jabberServiceContact.getContact(mData.getNick()) == null && !jabberServiceContact.getUserId().equals(mData.getCurrentContact().getUserId())) {
                        Toast.makeText(General.sawimActivity, getString(R.string.contact_walked), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                setText(chat.onMessageSelected(mData));
                if (nickList.getVisibility() == View.VISIBLE && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                    nickList.setVisibility(View.GONE);
            }
        }
    }

    public Chat getCurrentChat() {
        return chat;
    }

    public void showMenu() {
        final MyMenu menu = new MyMenu(General.sawimActivity);
        boolean accessible = chat.getWritable() && (contact.isSingleUserContact() || contact.isOnline());
        menu.add(getString(adapter.isMultiСitation() ?
                R.string.disable_multi_citation : R.string.include_multi_citation), ContactMenu.MENU_MULTI_CITATION);
        if (0 < chat.getAuthRequestCounter()) {
            menu.add(JLocale.getString("grant"), ContactMenu.USER_MENU_GRANT_AUTH);
            menu.add(JLocale.getString("deny"), ContactMenu.USER_MENU_DENY_AUTH);
        }
        if (!contact.isAuth()) {
            menu.add(JLocale.getString("requauth"), ContactMenu.USER_MENU_REQU_AUTH);
        }
        if (accessible) {
            if (sawim.modules.fs.FileSystem.isSupported()) {
                menu.add(JLocale.getString("ft_name"), ContactMenu.USER_MENU_FILE_TRANS);
            }
            if (FileTransfer.isPhotoSupported()) {
                menu.add(JLocale.getString("ft_cam"), ContactMenu.USER_MENU_CAM_TRANS);
            }
        }
        menu.add(General.sawimActivity.getResources().getString(R.string.user_statuses), ContactMenu.USER_MENU_STATUSES);
        if (!contact.isSingleUserContact() && contact.isOnline()) {
            menu.add(JLocale.getString("leave_chat"), ContactMenu.CONFERENCE_DISCONNECT);
        }
        menu.add(JLocale.getString("delete_chat"), ContactMenu.ACTION_CURRENT_DEL_CHAT);
        menu.add(JLocale.getString("all_contact_except_this"), ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR);
        menu.add(JLocale.getString("all_contacts"), ContactMenu.ACTION_DEL_ALL_CHATS);

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(General.sawimActivity, R.style.AlertDialogCustom));
        builder.setTitle(contact.getName());
        builder.setCancelable(true);
        builder.setAdapter(menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (menu.getItem(which).idItem) {
                    case ContactMenu.MENU_MULTI_CITATION:
                        if (adapter.isMultiСitation()) {
                            adapter.setMultiСitation(false);
                        } else {
                            adapter.setMultiСitation(true);
                            Toast.makeText(General.sawimActivity, R.string.hint_multi_citation, Toast.LENGTH_LONG).show();
                        }
                        adapter.notifyDataSetChanged();
                        break;

                    case ContactMenu.ACTION_CURRENT_DEL_CHAT:
                        /*chat.removeMessagesAtCursor(chatListView.getFirstVisiblePosition() + 1);
                        if (0 < messData.size()) {
                            updateChat();
                        }*/
                        ChatHistory.instance.unregisterChat(chat);
                        if (General.sawimActivity.findViewById(R.id.fragment_container) != null)
                            getFragmentManager().popBackStack();
                        else
                            chat_viewLayout.setVisibility(LinearLayout.GONE);
                        break;

                    case ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR:
                        ChatHistory.instance.removeAll(chat);
                        break;

                    case ContactMenu.ACTION_DEL_ALL_CHATS:
                        ChatHistory.instance.removeAll(null);
                        if (General.sawimActivity.findViewById(R.id.fragment_container) != null)
                            getFragmentManager().popBackStack();
                        else
                            chat_viewLayout.setVisibility(LinearLayout.GONE);
                        break;

                    default:
                        new ContactMenu(protocol, contact).doAction(menu.getItem(which).idItem);
                }
            }
        });
        builder.create().show();
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
                sb.append(Clipboard.serialize(md.isIncoming(), md.getNick() + " " + md.strTime, msg));
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
                resetSpinner();
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
                MirandaNotes notes = ((Jabber)protocol).getMirandaNotes();
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
            InputMethodManager keyboard = (InputMethodManager) General.sawimActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) General.sawimActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showKeyboard() {
        messageEditor.requestFocus();
        showKeyboard(messageEditor);
    }

    @Override
    public void pastText(final String text) {
        General.sawimActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                insert(" " + text + " ");
                showKeyboard();
            }
        });
    }

    private void send() {
        hideKeyboard(messageEditor);
        chat.sendMessage(getText());
        resetText();
        updateList(contact);
        //updatePosition();
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
        General.sawimActivity.runOnUiThread(new Runnable() {
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
        General.sawimActivity.runOnUiThread(new Runnable() {
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
        public void afterTextChanged(Editable editable) {
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