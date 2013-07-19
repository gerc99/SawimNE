package ru.sawim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.ContextThemeWrapper;
import sawim.cl.ContactList;
import protocol.Contact;
import protocol.Protocol;
import protocol.jabber.Jabber;
import protocol.jabber.Jid;
import ru.sawim.activities.SawimActivity;

/**
 * Created with IntelliJ IDEA.
 * <preferences/>
 * Date: 30.12.12 3:15
 *
 * @author vladimir
 */
public class OpenUriActivity extends Activity {
    public void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            final boolean delay = null == SawimActivity.getInstance();
            final Uri uri = intent.getData();
            startActivity(new Intent(this, SawimActivity.class));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (delay) try {
                        // has not started yet
                        Thread.sleep(5000);
                    } catch (Exception ignored) {
                    }
                    process(uri);
                }
            }).start();
        }
    }


    public boolean process(Uri uri) {
        try {
            String path = uri.toString();
            if (path.startsWith("xmpp")) {
                processXmpp(path.substring("xmpp:".length()));
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public void processXmpp(String path) {
        String jid = path;
        if (-1 < path.indexOf('?')) {
            jid = path.substring(0, path.indexOf('?'));
        }
        sawim.modules.DebugLog.println("open xmpp " + path + " " + jid);
        Jabber jabber = getFirstJabber();
        if (null == jabber) {
            alert();
            return;
        }
        try {
            Contact c = jabber.createTempContact(Jid.getBareJid(jid));
            while (jabber.isConnecting()) {
                try {
                    Thread.sleep(2000);
                } catch (Exception ignored) {
                }
            }
            jabber.addTempContact(c);
            ContactList.getInstance().activate(c);
        } catch (Exception e) {
            sawim.modules.DebugLog.panic("uri", e);
        }
    }

    private Jabber getFirstJabber() {
        for (Protocol p : ContactList.getInstance().getProtocols()) {
            if (p instanceof Jabber) return (Jabber) p;
        }
        return null;
    }

    private void alert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(SawimActivity.getInstance());
        alertDialog.setCancelable(true);
        alertDialog.setTitle(getText(R.string.app_name));
        alertDialog.setMessage(getText(R.string.xmppAccountDontFound));
        alertDialog.show();
    }
}
