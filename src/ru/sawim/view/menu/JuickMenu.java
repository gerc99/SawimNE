package ru.sawim.view.menu;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.activities.ChatActivity;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 01.08.13
 * Time: 20:02
 * To change this template use File | Settings | File Templates.
 */
public class JuickMenu implements DialogInterface.OnClickListener {

    public static final String JUICK = "juick@juick.com";
    public static final String JUBO  = "jubo@nologin.ru";
    public static final String PSTO  = "psto@psto.net";
    public enum Mode {none, juick, psto};

    private Context context;
    private String currentProtocol;
    private String currentContact;
    private String text;

    public JuickMenu(Context baseContext, String currentProtocol, String currentContact, String clickedString) {
        context = baseContext;
        text = clickedString;
        this.currentProtocol = currentProtocol;
        this.currentContact = currentContact;
    }

    public void show() {
        CharSequence[] items = null;
        if (text.startsWith("#")) {
            items = new CharSequence[6];
            items[0] = context.getString(R.string.reply);
            items[1] = context.getString(R.string.view_comments);
            items[2] = context.getString(R.string.recommend_post);
            items[3] = context.getString(R.string.subscribe);
            items[4] = context.getString(R.string.unsubscribe);
            items[5] = context.getString(R.string.remove);
        } else if (text.startsWith("@")) {
            items = new CharSequence[5];
            items[0] = text;
            items[1] = context.getString(R.string.private_message);
            items[2] = context.getString(R.string.subscribe);
            items[3] = context.getString(R.string.unsubscribe);
            items[4] = context.getString(R.string.to_black_list);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.actions);
        builder.setItems(items, this);
        try {
            builder.create().show();
        } catch(Exception e){
            // WindowManager$BadTokenException will be caught and the app would not display
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (currentContact.equals(JUBO)) {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("protocol_id", currentProtocol);
            intent.putExtra("contact_id", "juick@juick.com");
            context.startActivity(intent);
        }
        String textToInser = "";
        if (text.startsWith("#")) {
            String id = null;
            if (text.indexOf("/") > 0) id = text.substring(0, text.indexOf("/"));
            else id = text;

            switch(which) {
                case 0:
                    textToInser = text;
                    break;
                case 1:
                    textToInser =  id + "+";
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
            switch(which) {
                case 0:
                    textToInser = text;
                    break;
                case 1:
                    if (currentContact.equals(PSTO))
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
        if (General.getInstance().getUpdateChatListener() != null)
            General.getInstance().getUpdateChatListener().pastText(textToInser);
    }
}