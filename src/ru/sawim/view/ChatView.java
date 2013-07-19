package ru.sawim.view;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import ru.sawim.SawimApplication;
import ru.sawim.models.MessagesAdapter;
import ru.sawim.view.menu.MyMenu;
import sawim.Clipboard;
import sawim.FileTransfer;
import sawim.Options;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.MessData;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
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
public class ChatView extends Fragment implements AbsListView.OnScrollListener, General.OnUpdateChat {

    private Chat chat;
    private Protocol protocol;
    private Contact currentContact;
    private List<MessData> messData;
    private MyListView chatListView;
    private EditText messageEditor;
    private boolean sendByEnter;
    private MessagesAdapter adapter;
    private LinearLayout sidebar;
    private ImageButton usersImage;
    private ListView nickList;
    private ImageButton chatsImage;
    private TextView contactName;
    private TextView contactStatus;
    private LinearLayout chatBarLayout;
    private LinearLayout chat_viewLayout;
    private MucUsersView mucUsersView;
    private static Hashtable<String, Integer> positionHash = new Hashtable<String, Integer>();
    private Bitmap usersIcon = ImageList.createImageList("/participants.png").iconAt(0).getImage();

    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);
        chatBarLayout.setBackgroundColor(Scheme.getColorWithAlpha(Scheme.THEME_CAP_BACKGROUND));
        usersImage.setImageBitmap(usersIcon);
        usersImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sidebar.setVisibility(sidebar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                updateChat();
            }
        });
        chatsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forceGoToChat();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.chat, container, false);
        chat_viewLayout = (LinearLayout) v.findViewById(R.id.chat_view);
        chatBarLayout = (LinearLayout) v.findViewById(R.id.chat_bar);
        usersImage = (ImageButton) v.findViewById(R.id.usersImage);
        contactName = (TextView) v.findViewById(R.id.item_name);
        contactStatus = (TextView) v.findViewById(R.id.item_description);
        chatsImage = (ImageButton) v.findViewById(R.id.chatsImage);
        return v;
    }

    public static final int MENU_COPY_TEXT = 1;
    private static final int ACTION_ADD_TO_HISTORY = 2;
    private static final int ACTION_TO_NOTES = 3;
    private static final int ACTION_QUOTE = 4;
    private static final int ACTION_DEL_CHAT = 5;

    public void showMenu() {
        final MyMenu menu = new MyMenu(getActivity());
        boolean accessible = chat.getWritable() && (currentContact.isSingleUserContact() || currentContact.isOnline());
        if (0 < chat.getAuthRequestCounter()) {
            menu.add(JLocale.getString("grant"), Contact.USER_MENU_GRANT_AUTH);
            menu.add(JLocale.getString("deny"), Contact.USER_MENU_DENY_AUTH);
        }
        if (!currentContact.isAuth()) {
            menu.add(JLocale.getString("requauth"), Contact.USER_MENU_REQU_AUTH);
        }
        if (accessible) {
            if (sawim.modules.fs.FileSystem.isSupported()) {
                menu.add(JLocale.getString("ft_name"), Contact.USER_MENU_FILE_TRANS);
            }
            if (FileTransfer.isPhotoSupported()) {
                menu.add(JLocale.getString("ft_cam"), Contact.USER_MENU_CAM_TRANS);
            }
        }
        menu.add(getActivity().getResources().getString(R.string.user_statuses), Contact.USER_MENU_STATUSES);
        if (!currentContact.isSingleUserContact() && currentContact.isOnline()) {
            menu.add(JLocale.getString("leave_chat"), Contact.CONFERENCE_DISCONNECT);
        }
        menu.add(JLocale.getString("delete_chat"), ACTION_DEL_CHAT);

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
        builder.setTitle(currentContact.getName());
        builder.setCancelable(true);
        builder.setAdapter(menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (menu.getItem(which).idItem == ACTION_DEL_CHAT) {
                    /*chat.removeMessagesAtCursor(chatListView.getFirstVisiblePosition() + 1);
                    if (0 < messData.size()) {
                        updateChat();
                    }*/
                    ChatHistory.instance.unregisterChat(chat);
                    getActivity().finish();
                    return;
                }
                new ContactMenu(protocol, currentContact).doAction(getActivity(), menu.getItem(which).idItem);
            }
        });
        builder.create().show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.FIRST, MENU_COPY_TEXT, 0, android.R.string.copy);
        menu.add(Menu.FIRST, ACTION_QUOTE, 0, JLocale.getString("quote"));
        if (protocol instanceof Jabber) {
            menu.add(Menu.FIRST, ACTION_TO_NOTES, 0, R.string.add_to_notes);
        }
        if (!Options.getBoolean(Options.OPTION_HISTORY) && chat.hasHistory()) {
            menu.add(Menu.FIRST, ACTION_ADD_TO_HISTORY, 0, JLocale.getString("add_to_history"));
        }
        currentContact.addChatMenuItems(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        MessData md = messData.get(info.position);
        String msg = md.getText();
        switch (item.getItemId()) {
            case MENU_COPY_TEXT:
                if (null == md) {
                    return false;
                }
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                Clipboard.setClipBoardText(msg + "\n");
                break;

            case ACTION_QUOTE:
                StringBuffer sb = new StringBuffer();
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                sb.append(Clipboard.serialize(md.isIncoming(), md.getNick() + " " + md.strTime, msg));
                sb.append("\n-----\n");
                Clipboard.setClipBoardText(0 == sb.length() ? null : sb.toString());
                break;

            case ACTION_ADD_TO_HISTORY:
                chat.addTextToHistory(md);
                break;

            case ACTION_TO_NOTES:
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
    public void pastText(final String text) {
        SawimApplication.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                insert(" " + text + " ");
                showKeyboard();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroy(chat);
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

    public void destroy(Chat chat) {
        General.getInstance().setOnUpdateChat(null);
        if (chat != null) {
            chat.resetUnreadMessages();
            chat.setVisibleChat(false);
            chat = null;
        }
    }

    public void pause(Chat chat) {
        if (chat == null) return;
            addLastPosition(chat.getContact().getUserId(), chatListView.getFirstVisiblePosition());
    }

    public void resume(final Chat chat) {
        if (chat == null) return;
        SawimApplication.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int count = chatListView.getCount();
                int unreadMessages = chat.getUnreadMessageCount();
                int lastPosition = getLastPosition(chat.getContact().getUserId()) + 1;
                Log.e("ChatView", "lastPosition "+lastPosition);
                Log.e("ChatView", "count "+count);
                if (lastPosition >= 0) {
                    chatListView.setScroll(false);
                    chatListView.setSelection(lastPosition);
                } else {
                    if (unreadMessages > 0) {
                        chatListView.setScroll(false);
                        chatListView.setSelection(count - (unreadMessages + 1));
                    } else {
                        if (chatListView.isScroll()) chatListView.setSelection(count);
                    }
                }
            }
        });
        chat.resetUnreadMessages();
        updateChat();
    }

    private void forceGoToChat() {
        addLastPosition(chat.getContact().getUserId(), chatListView.getFirstVisiblePosition());
        chat.resetUnreadMessages();
        chat.setVisibleChat(false);
        ChatHistory chatHistory = ChatHistory.instance;
        Chat current = chatHistory.chatAt(chatHistory.getPreferredItem());
        if (0 < current.getUnreadMessageCount()) {
            openChat(current.getProtocol(), current.getContact());
            resume(current);
        }
    }

    private void addLastPosition(String jid, int position) {
        positionHash.put(jid, position);
    }

    private int getLastPosition(String jid) {
        if (positionHash.containsKey(jid)) return positionHash.remove(jid);
        else return -1;
    }

    public void openChat(Protocol p, Contact c) {
        chat = null;
        General.getInstance().setOnUpdateChat(null);
        General.getInstance().setOnUpdateChat(this);
        final Activity currentActivity = getActivity();
        protocol = p;
        currentContact = c;
        chat = protocol.getChat(currentContact);
        messData = chat.getMessData();
        messageEditor = (EditText) currentActivity.findViewById(R.id.messageBox);
        adapter = new MessagesAdapter(getActivity(), chat, messData);
        chatListView = (MyListView) getActivity().findViewById(R.id.chat_history_list);
        messageEditor.addTextChangedListener(textWatcher);
        chatListView.setStackFromBottom(true);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        chatListView.setOnCreateContextMenuListener(this);
        chatListView.setOnScrollListener(this);
        chatListView.setOnItemClickListener(new ChatClick());
        chatListView.setFocusable(true);
        chatListView.setCacheColorHint(0x00000000);
        chatListView.setAdapter(adapter);
        chat.setVisibleChat(true);

        contactName.setTextColor(Scheme.getColor(Scheme.THEME_CAP_TEXT));
		contactName.setTextSize(General.getFontSize());
        contactName.setText(currentContact.getName());
        contactStatus.setTextColor(Scheme.getColor(Scheme.THEME_CAP_TEXT));
		contactStatus.setTextSize(General.getFontSize());
        contactStatus.setText(ContactList.getInstance().getManager().getStatusMessage(currentContact));
        int background = Scheme.getColorWithAlpha(Scheme.THEME_BACKGROUND);
        chat_viewLayout.setBackgroundColor(background);
        messageEditor.setBackgroundColor(background);
        messageEditor.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));

        nickList = (ListView) currentActivity.findViewById(R.id.muc_user_list);
        sidebar = (LinearLayout) currentActivity.findViewById(R.id.sidebar);
        sidebar.setVisibility(View.GONE);
        sidebar.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        if (currentContact instanceof JabberServiceContact && currentContact.isConference()) {
            mucUsersView = new MucUsersView();
            mucUsersView.init(protocol, (JabberServiceContact) currentContact);
            mucUsersView.show(getActivity(), nickList, usersImage, this);
            if (sidebar.getVisibility() == View.VISIBLE) {
                sidebar.setVisibility(View.VISIBLE);
            } else {
                sidebar.setVisibility(View.GONE);
            }
        } else {
            mucUsersView = null;
            usersImage.setVisibility(View.GONE);
            nickList.setVisibility(View.GONE);
        }
        ImageButton menuButton = (ImageButton) currentActivity.findViewById(R.id.menu_button);
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

        ImageButton smileButton = (ImageButton) currentActivity.findViewById(R.id.input_smile_button);
        smileButton.setBackgroundColor(background);
        smileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SmilesView().show(getActivity().getSupportFragmentManager(), "show-smiles");
            }
        });
        sendByEnter = Options.getBoolean(Options.OPTION_SIMPLE_INPUT);
        ImageButton sendButton = (ImageButton) currentActivity.findViewById(R.id.input_send_button);
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
        if (sendByEnter) {
            messageEditor.setImeOptions(EditorInfo.IME_ACTION_SEND);
            messageEditor.setOnEditorActionListener(enterListener);
        }
    }

    public class ChatClick implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            MessData msg = (MessData) adapterView.getAdapter().getItem(position);
            setText("");
            setText(onMessageSelected(msg));
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

    private void send() {
        hideKeyboard(messageEditor);
        chat.sendMessage(getText());
        resetText();
        updateChat();
    }

    public boolean canAdd(String what) {
        String text = getText();
        if (0 == text.length()) return false;
        // more then one comma
        if (text.indexOf(',') != text.lastIndexOf(',')) return true;
        // replace one post number to another
        if (what.startsWith("#") && !text.contains(" ")) return false;
        return true/*!text.endsWith(", ")*/;
    }

    private void resetText() {
        SawimApplication.getInstance().runOnUiThread(new Runnable() {
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
        SawimApplication.getInstance().runOnUiThread(new Runnable() {
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

    private void updateChatIcon() {
        Icon icMess = ChatHistory.instance.getUnreadMessageIcon();
        if (icMess == null) {
            chatsImage.setVisibility(ImageView.GONE);
        } else {
            chatsImage.setVisibility(ImageView.VISIBLE);
            chatsImage.setImageBitmap(icMess.getImage());
        }
    }

    private String getBlogPostId(String text) {
        if (StringConvertor.isEmpty(text)) {
            return null;
        }
        String lastLine = text.substring(text.lastIndexOf('\n') + 1);
        if (0 == lastLine.length()) {
            return null;
        }
        if ('#' != lastLine.charAt(0)) {
            return null;
        }
        int numEnd = lastLine.indexOf(' ');
        if (-1 != numEnd) {
            lastLine = lastLine.substring(0, numEnd);
        }
        return lastLine + " ";
    }

    private String writeMessageTo(String nick) {
        if (null != nick) {
            if ('/' == nick.charAt(0)) {
                nick = ' ' + nick;
            }
            nick += Chat.ADDRESS;

        } else {
            nick = "";
        }
        return nick;
    }

    private boolean isBlogBot() {
        if (currentContact instanceof JabberContact) {
            return ((Jabber) protocol).isBlogBot(currentContact.getUserId());
        }
        return false;
    }

    public String onMessageSelected(MessData md) {
        if (currentContact.isSingleUserContact()) {
            if (isBlogBot()) {
                return getBlogPostId(md.getText());
            }
            return "";
        }
        String nick = ((null == md) || md.isFile()) ? null : md.getNick();
        return writeMessageTo(chat.getMyName().equals(nick) ? null : nick);
    }

    @Override
    public void updateChat() {
        SawimApplication.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateChatIcon();
                boolean scroll = chatListView.isScroll();
                if (adapter != null) {
                    if (scroll && chatListView.getCount() >= 1) {
                        chatListView.setSelection(chatListView.getCount());
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        });
        updateMucList();

        RosterView rosterView = (RosterView) getActivity().getSupportFragmentManager().findFragmentById(R.id.roster_fragment);
        if (rosterView != null) {
            rosterView.updateBarProtocols();
            rosterView.updateRoster();
        }
    }

    @Override
    public void addMessage(final Chat chat, final MessData mess) {
        SawimApplication.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (adapter != null) {
                    chat.removeOldMessages();
                    chat.getMessData().add(mess);
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void updateMucList() {
        SawimApplication.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mucUsersView != null)
                    mucUsersView.update();
            }
        });
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    public void onScroll(AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
        if (firstVisibleItem + visibleItemCount == totalItemCount) chatListView.setScroll(true);
        else chatListView.setScroll(false);
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
