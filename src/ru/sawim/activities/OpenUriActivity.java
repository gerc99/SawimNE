package ru.sawim.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import protocol.Contact;
import protocol.Protocol;
import protocol.xmpp.Jid;
import protocol.xmpp.Xmpp;
import ru.sawim.R;
import ru.sawim.roster.RosterHelper;

/**
 * Created with IntelliJ IDEA.
 * <preferences/>
 * Date: 30.12.12 3:15
 *
 * @author vladimir
 */
public class OpenUriActivity extends ActionBarActivity {

    public void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            final boolean delay = true;
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
            },"OpenURI").start();
        }
    }

    private boolean process(Uri uri) {
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

    private void processXmpp(String path) {
        String jid = path;
        if (-1 < path.indexOf('?')) {
            jid = path.substring(0, path.indexOf('?'));
        }
        ru.sawim.modules.DebugLog.println("open xmpp " + path + " " + jid);
        Xmpp xmpp = getFirstXmpp();
        if (null == xmpp) {
            alert();
            return;
        }
        try {
            Contact c = xmpp.createTempContact(Jid.getBareJid(jid));
            while (xmpp.isConnecting()) {
                try {
                    Thread.sleep(2000);
                } catch (Exception ignored) {
                }
            }
            xmpp.addTempContact(c);
            RosterHelper.getInstance().activate(c);
        } catch (Exception e) {
            ru.sawim.modules.DebugLog.panic("uri", e);
        }
    }

    private Xmpp getFirstXmpp() {
        for (Protocol p : RosterHelper.getInstance().getProtocols()) {
            if (p instanceof Xmpp) return (Xmpp) p;
        }
        return null;
    }

    private void alert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setCancelable(true);
        alertDialog.setTitle(getText(R.string.app_name));
        alertDialog.setMessage(getText(R.string.xmppAccountDontFound));
        alertDialog.show();
    }
}