package ru.sawim.view;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
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
import sawim.util.JLocale;

import java.util.Hashtable;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.01.13
 * Time: 20:30
 * To change this template use File | Settings | File Templates.
 */
public class ChatView extends Fragment implements General.OnUpdateChat {

    private static final String TAG = "ChatView";
    private Chat chat;
    private Protocol protocol;
    private Contact currentContact;
    private boolean sendByEnter;
    private ListView nickList;
    private ListView chatListView;
    private MySpinner spinner;
    private MucUsersView mucUsersView;
    private MessagesAdapter adapter = new MessagesAdapter();
    private ChatsSpinnerAdapter chatsSpinnerAdapter;
    private LinearLayout chatBarLayout;
    private LinearLayout chat_viewLayout;
    private LinearLayout sidebar;
    private ImageButton usersImage;
    private ImageButton chatsImage;
    private ImageButton menuButton;
    private ImageButton smileButton;
    private ImageButton sendButton;
    private EditText messageEditor;
    private Bitmap usersIcon = ImageList.createImageList("/participants.png").iconAt(0).getImage();
    private static Hashtable<String, Chat.ScrollState> positionHash = new Hashtable<String, Chat.ScrollState>();

    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);
        chatBarLayout.setBackgroundColor(Scheme.getColorWithAlpha(Scheme.THEME_CAP_BACKGROUND));
        usersImage.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_CAP_BACKGROUND), PorterDuff.Mode.MULTIPLY);
        usersImage.setImageBitmap(usersIcon);
        usersImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sidebar.setVisibility(sidebar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                updateChat();
            }
        });

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
        chatsSpinnerAdapter = new ChatsSpinnerAdapter(getActivity());
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
        final MyMenu menu = new MyMenu(getActivity());
        boolean accessible = chat.getWritable() && (currentContact.isSingleUserContact() || currentContact.isOnline());
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
        menu.add(getActivity().getResources().getString(R.string.user_statuses), ContactMenu.USER_MENU_STATUSES);
        if (!currentContact.isSingleUserContact() && currentContact.isOnline()) {
            menu.add(JLocale.getString("leave_chat"), ContactMenu.CONFERENCE_DISCONNECT);
        }
        menu.add(JLocale.getString("delete_chat"), ContactMenu.ACTION_CURRENT_DEL_CHAT);
        menu.add(JLocale.getString("all_contact_except_this"), ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR);
        menu.add(JLocale.getString("all_contacts"), ContactMenu.ACTION_DEL_ALL_CHATS);

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
        builder.setTitle(currentContact.getName());
        builder.setCancelable(true);
        builder.setAdapter(menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (menu.getItem(which).idItem) {
                    case ContactMenu.ACTION_CURRENT_DEL_CHAT:
                        /*chat.removeMessagesAtCursor(chatListView.getFirstVisiblePosition() + 1);
                        if (0 < messData.size()) {
                            updateChat();
                        }*/
                        ChatHistory.instance.unregisterChat(chat);
                        getActivity().finish();
                        break;

                    case ContactMenu.ACTION_DEL_ALL_CHATS_EXCEPT_CUR:
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ChatHistory.instance.removeAll(chat);
                            }
                        });
                        break;

                    case ContactMenu.ACTION_DEL_ALL_CHATS:
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ChatHistory.instance.removeAll(null);
                            }
                        });
                        getActivity().finish();
                        break;

                    default:
                        new ContactMenu(protocol, currentContact).doAction(getActivity(), menu.getItem(which).idItem);
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
                protocol.showUserInfo(getActivity(), ((JabberServiceContact) currentContact).getPrivateContact(nick));
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
        chatBarLayout = (LinearLayout) v.findViewById(R.id.chat_bar);
        usersImage = (ImageButton) v.findViewById(R.id.usersImage);
        spinner = (MySpinner) v.findViewById(R.id.spinner);
        chatsImage = (ImageButton) v.findViewById(R.id.chatsImage);
        nickList = (ListView) v.findViewById(R.id.muc_user_list);
        sidebar = (LinearLayout) v.findViewById(R.id.sidebar);

        menuButton = (ImageButton) v.findViewById(R.id.menu_button);
        smileButton = (ImageButton) v.findViewById(R.id.input_smile_button);
        sendButton = (ImageButton) v.findViewById(R.id.input_send_button);
        messageEditor = (EditText) v.findViewById(R.id.messageBox);
        return v;
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
        View item = chatListView.getChildAt(0);
        addLastPosition(chatListView.getFirstVisiblePosition(), (item == null) ? 0 : Math.abs(item.getBottom()));

        General.getInstance().setOnUpdateChat(null);
        chat.resetUnreadMessages();
        chat.setVisibleChat(false);
        if (chat.empty())
            ChatHistory.instance.unregisterChat(chat);
    }

    public void resume(Chat chat) {
        if (chat == null) return;
        final Chat.ScrollState lastPosition = getLastPosition(chat.getContact().getUserId());
        if (lastPosition != null)
            chatListView.setSelectionFromTop(lastPosition.position + 1, lastPosition.offset);

        General.getInstance().setOnUpdateChat(this);
        chat.resetUnreadMessages();
        chat.setVisibleChat(true);
        if (chat.empty())
            ChatHistory.instance.registerChat(chat);
        updateChat();
    }

    private void forceGoToChat(int position) {
        Chat current = ChatHistory.instance.chatAt(position);
        if (current == null) return;
        pause(chat);
        openChat(current.getProtocol(), current.getContact());
        resume(current);
    }

    private void addLastPosition(int position, int offset) {
        Chat.ScrollState scrollState = new Chat.ScrollState();
        scrollState.position = position;
        scrollState.offset = offset;
        positionHash.put(currentContact.getUserId(), scrollState);
    }

    private Chat.ScrollState getLastPosition(String jid) {
        if (positionHash.containsKey(jid)) return positionHash.remove(jid);
        else return null;
    }

    public void openChat(Protocol p, Contact c) {
        protocol = p;
        currentContact = c;
        final FragmentActivity currentActivity = getActivity();
        chat = protocol.getChat(currentContact);
        chatListView = (ListView) currentActivity.findViewById(R.id.chat_history_list);
        adapter.init(currentActivity, chat);
        chatListView.setStackFromBottom(true);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        chatListView.setOnCreateContextMenuListener(this);
        chatListView.setOnItemClickListener(new ChatClick());
        chatListView.setFocusable(true);
        chatListView.setCacheColorHint(0x00000000);
        chatListView.setAdapter(adapter);
        if (spinner.getOnItemSelectedEvenIfUnchangedListener() == null)
            initSpinner();
        int background = Scheme.getColorWithAlpha(Scheme.THEME_BACKGROUND);
        chat_viewLayout.setBackgroundColor(background);

        sidebar.setVisibility(View.GONE);
        sidebar.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        if (currentContact instanceof JabberServiceContact && currentContact.isConference()) {
            mucUsersView = new MucUsersView();
            mucUsersView.init(protocol, (JabberServiceContact) currentContact);
            mucUsersView.show(this, nickList);
            usersImage.setVisibility(View.VISIBLE);
            if (sidebar.getVisibility() == View.VISIBLE) {
                sidebar.setVisibility(View.VISIBLE);
            } else {
                sidebar.setVisibility(View.GONE);
            }
        } else {
            usersImage.setVisibility(View.GONE);
            nickList.setVisibility(View.GONE);
        }

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
                new SmilesView().show(currentActivity.getSupportFragmentManager(), "show-smiles");
            }
        });
        messageEditor.addTextChangedListener(textWatcher);
        messageEditor.setBackgroundColor(background);
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
                }
            });
        }
    }

    private void updateChatIcon() {
        Icon icMess = ChatHistory.instance.getUnreadMessageIcon();
        if (icMess == null) {
            chatsImage.setVisibility(ImageView.GONE);
        } else {
            chatsImage.setVisibility(ImageView.VISIBLE);
            chatsImage.setImageBitmap(icMess.getImage());
        }
    }

    @Override
    public void updateChat() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateChatIcon();
                if (adapter != null) {
                    adapter.refreshList(chat.getMessData());
                }
            }
        });
        /*RosterView rosterView = (RosterView) getActivity().getSupportFragmentManager().findFragmentById(R.id.roster_fragment);
        if (rosterView != null) {
            rosterView.updateBarProtocols();
            rosterView.updateRoster();
        }*/
    }

    @Override
    public void updateMucList() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mucUsersView != null)
                    mucUsersView.update();
            }
        });
    }

    private class ChatClick implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            MessData msg = (MessData) adapterView.getAdapter().getItem(position);
            setText("");
            setText(chat.onMessageSelected(msg));
        }
    }

    public Chat getCurrentChat() {
        return chat;
    }

    private void showKeyboard(View view) {
        Configuration conf = Resources.getSystem().getConfiguration();
        if (conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO) {
            InputMethodManager keyboard = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showKeyboard() {
        messageEditor.requestFocus();
        showKeyboard(messageEditor);
    }

    @Override
    public void pastText(final String text) {
        getActivity().runOnUiThread(new Runnable() {
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
        updateChat();
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
        getActivity().runOnUiThread(new Runnable() {
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
        getActivity().runOnUiThread(new Runnable() {
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