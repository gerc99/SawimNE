package ru.sawim.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.view.VirtualListView;
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
        Intent i = getIntent();
        ChatView view = (ChatView) getSupportFragmentManager().findFragmentById(R.id.chat_fragment);
        Protocol protocol = ContactList.getInstance().getProtocol(i.getStringExtra("protocol_id"));
        Contact currentContact = protocol.getItemByUIN(i.getStringExtra("contact_id"));
        view.openChat(protocol, currentContact);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ChatView view = (ChatView) getSupportFragmentManager().findFragmentById(R.id.chat_fragment);
        view.onCreateMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ChatView view = (ChatView) getSupportFragmentManager().findFragmentById(R.id.chat_fragment);
        view.onMenuItemSelected(item);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("ChatActivity", "onDestroy()");
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.e("ChatActivity", "onPause()");
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.e("ChatActivity", "onResume()");
    }
}
