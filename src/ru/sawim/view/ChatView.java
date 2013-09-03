package ru.sawim.view;

import DrawControls.icons.Icon;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
public class ChatView extends SawimFragment implements General.OnUpdateChat {

    public static final String TAG = "ChatView";
    public static final String PROTOCOL_ID = "protocol_id";
    public static final String CONTACT_ID = "contact_id";
    private Chat chat;
    private Protocol protocol;
    private Contact currentContact;
    private boolean sendByEnter;
    private MyListView nickList;
    private MyListView chatListView;
    private MySpinner spinner;
    private MucUsersView mucUsersView;
    private MessagesAdapter adapter;
    private ChatsSpinnerAdapter chatsSpinnerAdapter;
    private LinearLayout chatBarLayout;
    private LinearLayout chatLayout;
    private LinearLayout chat_viewLayout;
    private ImageButton usersImage;
    private ImageButton chatsImage;
    private ImageButton menuButton;
    private ImageButton smileButton;
    private ImageButton sendButton;
    private EditText messageEditor;

    private static Protocol lastProtocol;
    private static Contact lastContact;

    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {Scheme.getColor(Scheme.THEME_CAP_BACKGROUND),Scheme.getColor(Scheme.THEME_BACKGROUND)});
        gd.setCornerRadius(0f);
        chatBarLayout.setBackgroundDrawable(gd);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            usersImage.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_CAP_BACKGROUND), PorterDuff.Mode.MULTIPLY);
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
        chatsImage.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_CAP_BACKGROUND), PorterDuff.Mode.MULTIPLY);
        chatsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetSpinner();
                forceGoToChat(ChatHistory.instance.getPreferredItem());
            }
        });
    }

    public void resetSpinner() {
        spinner.setOnItemSelectedEvenIfUnchangedListener(null);
    }

    private void initSpinner() {
        chatsSpinnerAdapter = new ChatsSpinnerAdapter(General.sawimActivity, ChatHistory.instance.historyTable);
        spinner.setAdapter(chatsSpinnerAdapter);
        spinner.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_CAP_BACKGROUND), PorterDuff.Mode.MULTIPLY);
        spinner.setSelection(ChatHistory.instance.getItemChat(currentContact));
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

    public void showMenu() {
        final MyMenu menu = new MyMenu(General.sawimActivity);
        boolean accessible = chat.getWritable() && (currentContact.isSingleUserContact() || currentContact.isOnline());
        menu.add(getString(adapter.isMultiСitation() ?
                R.string.disable_multi_citation : R.string.include_multi_citation), ContactMenu.MENU_MULTI_CITATION);
        if (0 < chat.getAuthRequestCounter()) {
            menu.add(JLocale.getString("grant"), ContactMenu.USER_MENU_GRANT_AUTH);
            menu.add(JLocale.getString("deny"), ContactMenu.USER_MENU_DENY_AUTH);
        }
        if (!currentContact.isAuth()) {
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
        if (!currentContact.isSingleUserContact() && currentContact.isOnline()) {
            menu.add(JLocale.getString("leave_chat"), ContactMenu.CONFERENCE_DISCONNECT);
        }
        menu.add(JLocale.getString("delete_chat"), ContactMenu.ACTION_CURRENT_DEL_CHAT);
        menu.add(JLocale.getString("all_contact_except_this"), ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR);
        menu.add(JLocale.getString("all_contacts"), ContactMenu.ACTION_DEL_ALL_CHATS);

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(General.sawimActivity, R.style.AlertDialogCustom));
        builder.setTitle(currentContact.getName());
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
                        chatListView.setAdapter(adapter);
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
                        new ContactMenu(protocol, currentContact).doAction(menu.getItem(which).idItem);
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
        if (currentContact instanceof JabberServiceContact && currentContact.isConference()) {
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
        currentContact.addChatMenuItems(menu);
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
                String jid = Jid.realJidToSawimJid(currentContact.getUserId() + "/" + nick);
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
                protocol.showUserInfo(((JabberServiceContact) currentContact).getPrivateContact(nick));
                break;
            case ContactMenu.COMMAND_STATUS:
                protocol.showStatus(((JabberServiceContact) currentContact).getPrivateContact(nick));
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.chat, container, false);
        chat_viewLayout = (LinearLayout) v.findViewById(R.id.chat_view);
        chatLayout = (LinearLayout) chat_viewLayout.findViewById(R.id.list);

        chatBarLayout = (LinearLayout) chat_viewLayout.findViewById(R.id.chat_bar);
        usersImage = (ImageButton) chatBarLayout.findViewById(R.id.usersImage);
        spinner = (MySpinner) chatBarLayout.findViewById(R.id.spinner);
        chatsImage = (ImageButton) chatBarLayout.findViewById(R.id.chatsImage);

        menuButton = (ImageButton) chat_viewLayout.findViewById(R.id.menu_button);
        smileButton = (ImageButton) chat_viewLayout.findViewById(R.id.input_smile_button);
        sendButton = (ImageButton) chat_viewLayout.findViewById(R.id.input_send_button);
        messageEditor = (EditText) chat_viewLayout.findViewById(R.id.messageBox);
        return v;
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
        lastContact = currentContact;
        Bundle args = getArguments();
        if (args != null) {
            args.putString(ChatView.PROTOCOL_ID, protocol.getUserId());
            args.putString(ChatView.CONTACT_ID, currentContact.getUserId());
        }

        View item = chatListView.getChildAt(0);
        addLastPosition(chat.getContact().getUserId(), chatListView.getFirstVisiblePosition(), (item == null) ? 0 : Math.abs(item.getBottom()) - item.getHeight());

        chat.setVisibleChat(false);
        General.getInstance().setOnUpdateChat(null);
        chat.resetUnreadMessages();
    }

    public void resume(Chat chat) {
        if (chat == null) return;
        Chat.ScrollState lastPosition = getLastPosition(chat.getContact().getUserId());
        Log.e(TAG, "lastPosition "+chat.getContact().getUserId());
        if (lastPosition != null && lastPosition.position > 0) {
            Log.e(TAG, "position "+General.positionHash.size());
            chatListView.setSelectionFromTop(lastPosition.position, lastPosition.offset);
        } else chatListView.setSelection(0);

        chat.setVisibleChat(true);
        ChatHistory.instance.registerChat(chat);
        General.getInstance().setOnUpdateChat(this);
        chat.resetUnreadMessages();
        updateChatIcon();
        updateList();
    }

    public boolean hasBack() {
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
        Chat.ScrollState scrollState = new Chat.ScrollState();
        scrollState.position = position;
        scrollState.offset = offset;
        General.positionHash.put(jid, scrollState);
    }

    private Chat.ScrollState getLastPosition(String jid) {
        if (General.positionHash.containsKey(jid)) return General.positionHash.get(jid);
        else return null;
    }

    public void openChat(Protocol p, Contact c) {
        if (General.sawimActivity.findViewById(R.id.fragment_container) == null)
            chat_viewLayout.setVisibility(LinearLayout.VISIBLE);
        protocol = p;
        currentContact = c;
        final FragmentActivity currentActivity = General.sawimActivity;
        chat = protocol.getChat(currentContact);

        if (spinner.getOnItemSelectedEvenIfUnchangedListener() == null)
            initSpinner();
        initList();
        initMucUsers();

        int background = Scheme.getColor(Scheme.THEME_BACKGROUND);
        chat_viewLayout.setBackgroundColor(background);
        if (General.isTablet(currentActivity)) {
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
                new SmilesView().show(currentActivity.getSupportFragmentManager(), "show-smiles");
            }
        });
        messageEditor.addTextChangedListener(textWatcher);
        messageEditor.getBackground().setColorFilter(background, PorterDuff.Mode.MULTIPLY);;
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
    }

    private void initList() {
        chatListView = new MyListView(General.sawimActivity);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && getResources().getDisplayMetrics().densityDpi < 200)
            lp.weight = 10;
        else
            lp.weight = 1;
        lp.bottomMargin = 8;
        chatListView.setLayoutParams(lp);
        chatListView.setFastScrollEnabled(true);
        chatListView.setStackFromBottom(true);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        chatListView.setOnCreateContextMenuListener(this);
        chatListView.setOnItemClickListener(new ChatClick());
        chatListView.setFocusable(true);
        if (chatListView.getAdapter() == null) {
            adapter = new MessagesAdapter();
            adapter.init(General.sawimActivity, chat);
            chatListView.setAdapter(adapter);
        } else {
            adapter.refreshList(chat.getMessData());
            chatListView.setAdapter(adapter);
        }
        chatLayout.removeAllViews();
        chatLayout.addView(chatListView);
    }

    private void initMucUsers() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nickList = new MyListView(General.sawimActivity);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && getResources().getDisplayMetrics().densityDpi < 200)
            lp.weight = 0;
        else
            lp.weight = (float) 1.5;
        lp.bottomMargin = 8;
        nickList.setLayoutParams(lp);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            nickList.setVisibility(View.GONE);
        else
            nickList.setVisibility(View.VISIBLE);
        nickList.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        if (currentContact instanceof JabberServiceContact && currentContact.isConference()) {
            mucUsersView = new MucUsersView();
            mucUsersView.init(protocol, (JabberServiceContact) currentContact);
            mucUsersView.show(this, nickList);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                usersImage.setVisibility(View.VISIBLE);
            else
                usersImage.setVisibility(View.GONE);
            /*if (nickList.getVisibility() == View.VISIBLE) {
                nickList.setVisibility(View.VISIBLE);
            } else {
                nickList.setVisibility(View.GONE);
            }*/
        } else {
            usersImage.setVisibility(View.GONE);
            nickList.setVisibility(View.GONE);
        }
        chatLayout.addView(nickList);
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
                if (contact == currentContact) {
                    updateList();
                }
            }
        });
    }

    private void updateList() {
        if (chatsSpinnerAdapter != null)
            chatsSpinnerAdapter.refreshList(ChatHistory.instance.historyTable);
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

    /*@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e(TAG, "___________===");
        outState.putString(ChatView.PROTOCOL_ID, protocol.getUserId());
        outState.putString(ChatView.CONTACT_ID, currentContact.getUserId());
    }*/

    private class ChatClick implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            MessData msg = (MessData) adapterView.getAdapter().getItem(position);
            if (currentContact instanceof JabberServiceContact) {
                JabberServiceContact jabberServiceContact = ((JabberServiceContact) currentContact);
                if (jabberServiceContact.getContact(msg.getNick()) == null && !jabberServiceContact.getUserId().equals(msg.getNick()))
                    Toast.makeText(General.sawimActivity, getString(R.string.contact_walked), Toast.LENGTH_LONG).show();
            }
            setText(chat.onMessageSelected(msg));
            if (nickList.getVisibility() == View.VISIBLE && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                nickList.setVisibility(View.GONE);
        }
    }

    public Chat getCurrentChat() {
        return chat;
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
        updateList();
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