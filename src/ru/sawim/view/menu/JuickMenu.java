package ru.sawim.view.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.chat.Chat;
import ru.sawim.roster.RosterHelper;
import ru.sawim.view.ChatView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 01.08.13
 * Time: 20:02
 * To change this template use File | Settings | File Templates.
 */
public class JuickMenu implements DialogInterface.OnClickListener {

    public static final String JUICK = "juick@juick.com";
    public static final String JUBO = "jubo@nologin.ru";
    public static final String PSTO = "psto@psto.net";
    public static final String POINT = "p@point.im";
    public static final int MODE_JUICK = 0;
    public static final int MODE_PSTO = 1;

    private FragmentActivity activity;
    private String currentProtocol;
    private String currentContact;
    private String text;

    public JuickMenu(FragmentActivity activity, String currentProtocol, String currentContact, String clickedString) {
        this.activity = activity;
        text = clickedString;
        this.currentProtocol = currentProtocol;
        this.currentContact = currentContact;
    }

    public void show() {
        CharSequence[] items = null;
        if (text.startsWith("#")) {
            items = new CharSequence[6];
            items[0] = activity.getString(R.string.reply);
            items[1] = activity.getString(R.string.view_comments);
            items[2] = activity.getString(R.string.recommend_post);
            items[3] = activity.getString(R.string.subscribe);
            items[4] = activity.getString(R.string.unsubscribe);
            items[5] = activity.getString(R.string.remove);
        } else if (text.startsWith("@")) {
            items = new CharSequence[5];
            items[0] = text;
            items[1] = activity.getString(R.string.private_message);
            items[2] = activity.getString(R.string.subscribe);
            items[3] = activity.getString(R.string.unsubscribe);
            items[4] = activity.getString(R.string.to_black_list);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.actions);
        builder.setItems(items, this);
        try {
            builder.create().show();
        } catch (Exception e) {
            // WindowManager$BadTokenException will be caught and the app would not display
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (currentContact.startsWith(JUBO)) {
            Protocol protocol = RosterHelper.getInstance().getProtocol(currentProtocol);
            ChatView chatView = (ChatView) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            if (chatView == null) {
                ChatView newFragment = new ChatView();
                newFragment.initChat(protocol, protocol.getItemByUID(currentContact));
                FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, newFragment, ChatView.TAG);
                transaction.addToBackStack(null);
                transaction.commit();
            } else {
                Chat chat = chatView.getCurrentChat();
                chatView.pause(chat);
                if (currentProtocol != null) {
                    chatView.openChat(protocol, protocol.getItemByUID("juick@juick.com"));
                    chatView.resume(chatView.getCurrentChat());
                }
            }
        }
        String textToInser = "";
        if (text.startsWith("#")) {
            String id;
            if (text.indexOf("/") > 0) id = text.substring(0, text.indexOf("/"));
            else id = text;

            switch (which) {
                case 0:
                    textToInser = text;
                    break;
                case 1:
                    textToInser = id + "+";
                    break;
                case 2:
                    textToInser = "! " + id;
                    break;
                case 3:
                    textToInser = "S " + id;
                    break;
                case 4:
                    textToInser = "U " + id;
                    break;
                case 5:
                    textToInser = "D " + id;
                    break;
                default:
                    break;
            }
        } else if (text.startsWith("@")) {
            switch (which) {
                case 0:
                    textToInser = text;
                    break;
                case 1:
                    if (currentContact.startsWith(PSTO))
                        textToInser = "P " + text;
                    else
                        textToInser = "PM " + text;
                    break;
                case 2:
                    textToInser = "S " + text;
                    break;
                case 3:
                    textToInser = "U " + text;
                    break;
                case 4:
                    textToInser = "BL " + text;
                    break;
                default:
                    break;
            }
        }
        if (RosterHelper.getInstance().getUpdateChatListener() != null)
            RosterHelper.getInstance().getUpdateChatListener().pastText(textToInser);
    }
}