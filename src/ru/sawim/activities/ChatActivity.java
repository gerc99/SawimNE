package ru.sawim.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.General;
import sawim.ExternalApi;
import sawim.cl.ContactList;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.view.ChatView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.05.13
 * Time: 11:41
 * To change this template use File | Settings | File Templates.
 */
public class ChatActivity extends FragmentActivity {

    public static ChatActivity THIS;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        THIS = this;
        setContentView(R.layout.chat_fragment);
        ExternalApi.instance.setActivity(this);
        Intent i = getIntent();
        ChatView view = (ChatView) getSupportFragmentManager().findFragmentById(R.id.chat_fragment);
        Protocol protocol = ContactList.getInstance().getProtocol(i.getStringExtra("protocol_id"));
        Contact currentContact = protocol.getItemByUIN(i.getStringExtra("contact_id"));
        view.openChat(protocol, currentContact);
    }

    @Override
    public boolean onKeyUp(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_MENU) {
            ChatView view = (ChatView) getSupportFragmentManager().findFragmentById(R.id.chat_fragment);
            if (view != null)
                view.showMenu();
        }
        return super.onKeyUp(key, event);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (ExternalApi.instance.onActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        General.minimize();
    }

    @Override
    public void onResume() {
        super.onResume();
        General.maximize();
    }
}
