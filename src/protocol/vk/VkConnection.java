package protocol.vk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import protocol.Contact;
import protocol.Group;
import protocol.Roster;
import protocol.vk.api.VkApp;
import ru.sawim.SawimApplication;
import ru.sawim.chat.message.PlainMessage;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 19.01.13 18:21
 *
 * @author vladimir
 */
public class VkConnection implements Runnable {
    private VkApp api;
    private Vk vk;
    private volatile boolean running = true;

    VkConnection(Vk vk) {
        this.vk = vk;
        api = new VkApp(SawimApplication.getContext());
    }

    public void login() {
        new Thread(this,"VkLogin").start();
    }

    public void logout() {
        running = false;
    }

    public void sendMessage(final PlainMessage msg) {
        exec(new Runnable() {
            @Override
            public void run() {
                api.sendMessage(msg.getRcvrUin(), msg.getText());
            }
        });
    }

    private void exec(Runnable task) {
        new Thread(task,"VkExec").start();
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (Exception ignored) {
        }
    }

    public boolean isConnected() {
        return running && api.isLogged() && !api.isError();
    }

    private Vector<Contact> to(JSONArray list) throws JSONException {
        Vector<Contact> cl = new Vector<Contact>();
        for (int i = 0; i < list.length(); ++i) {
            JSONObject o = list.getJSONObject(i);
            String userId = "" + o.getInt("uid");
            String name = o.getString("first_name") + " " + o.getString("last_name");
            VkContact c = (VkContact) vk.createContact(userId, name);
            c.setGroupId(1);
            if (0 == o.getInt("online")) {
                c.setOfflineStatus();
            } else {
                c.setOnlineStatus();
            }
            cl.add(c);
        }
        return cl;
    }

    private Vector<Group> groups() {
        Vector<Group> groups = new Vector<Group>();
        Group g = vk.createGroup("General");
        g.setGroupId(1);
        groups.add(g);
        return groups;
    }

    @Override
    public void run() {
        vk.setConnectingProgress(0);
        if (!api.isLogged()) {
            api.showLoginDialog(vk.getUserId(), vk.getPassword());
        }
        if (!api.hasAccessToken()) api.showLoginDialog(vk.getUserId(), vk.getPassword());

        vk.setConnectingProgress(30);
        while (!api.isLogged() && !api.isError()) {
            sleep(5000);
        }
        vk.setConnectingProgress(70);
        processContacts();
        vk.setConnectingProgress(100);
        int onlineCheck = INTERVAL_ONLINE_CHECK;
        int messageCheck = INTERVAL_MESSAGE_CHECK;
        while (running) {
            sleep(INTERVAL_WAIT);
            if (0 == onlineCheck) {
                processOnlineContacts();
                onlineCheck = INTERVAL_ONLINE_CHECK;
            }
            if (0 == messageCheck) {
                processMessages();
                messageCheck = INTERVAL_MESSAGE_CHECK;
            }
            onlineCheck--;
            messageCheck--;
        }
        ru.sawim.modules.DebugLog.println("vk done");
    }


    private void processContacts() {
        try {
            Vector<Contact> contacts = to(api.getFriends().getJSONArray("response"));
            Vector<Group> groups = groups();
            vk.setRoster(new Roster(groups, contacts), false);
        } catch (Exception e) {
            ru.sawim.modules.DebugLog.panic("Vk processContacts", e);
        }
    }

    private void processOnlineContacts() {
        try {
            JSONArray ids = api.getOnlineFriends().getJSONArray("response");
            HashSet<Integer> ids_ = new HashSet<Integer>();
            for (int i = 0; i < ids.length(); ++i) {
                ids_.add(ids.getInt(i));
            }
            for (Object o : vk.getContactItems()) {
                VkContact c = (VkContact) o;
                boolean online = ids_.contains(c.getUid());
                if (online != c.isOnline()) {
                    if (online) {
                        c.setOnlineStatus();
                    } else {
                        c.setOfflineStatus();
                    }
                }
                vk.ui_changeContactStatus(c);
            }
        } catch (Exception ignored) {
        }
    }

    private void processMessages() {
        try {
            JSONArray msgs = api.getMessages().getJSONArray("response");
            if (1 == msgs.length()) return;
            for (int i = 1; i < msgs.length(); ++i) {
                JSONObject msg = msgs.getJSONObject(i);
                PlainMessage m = new PlainMessage("" + msg.getInt("uid"), vk,
                        msg.getInt("date"), msg.getString("body"), false);
                m.setMessageId(msg.getInt("mid"));
                vk.addMessage(m);
            }
            LinkedList<Integer> ids = new LinkedList<Integer>();
            for (int i = 1; i < msgs.length(); ++i) {
                ids.add(msgs.getJSONObject(i).getInt("mid"));
            }
            api.markAsRead(ids);
        } catch (Exception ignored) {
        }
    }

    private static final int INTERVAL_WAIT = 10000;
    private static final int INTERVAL_ONLINE_CHECK = 60000 / INTERVAL_WAIT;
    private static final int INTERVAL_MESSAGE_CHECK = 10000 / INTERVAL_WAIT;
}
