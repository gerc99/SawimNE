package ru.sawim.models;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import sawim.util.JLocale;
import protocol.Contact;
import protocol.Protocol;
import protocol.jabber.Jabber;
import protocol.jabber.JabberContact;
import protocol.jabber.JabberServiceContact;
import protocol.jabber.Jid;
import ru.sawim.view.widgets.LayerViewFactory;
import ru.sawim.view.widgets.MucUserViewFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 26.01.13
 * Time: 20:26
 * To change this template use File | Settings | File Templates.
 */
public class MucUsersAdapter extends BaseAdapter {

    private static final int ITEM_GROUP = 0;
    private static final int ITEM_CONTACT = 1;
    private static final int ITEM_TYPECOUNT = 2;
    private LayerViewFactory layerView;
    private MucUserViewFactory mucUserView;
    private JabberServiceContact conference;
    private List<Object> items = new ArrayList<Object>();
    private Jabber protocol;
    private int myRole;
    private int myAffiliation;

    public MucUsersAdapter(Context context, Jabber jabber, JabberServiceContact conf) {
        layerView = new LayerViewFactory(context);
        protocol = jabber;
        mucUserView = new MucUserViewFactory(context, jabber);
        conference = conf;
        myRole = getRole(conference.getMyName());
        myAffiliation = getAffiliation(conference.getMyName());
        update();
    }

    private void update() {
        items.clear();
        //Util.sort(conference.subcontacts);
        final int moderators = getContactCount(JabberServiceContact.ROLE_MODERATOR);
        final int participants = getContactCount(JabberServiceContact.ROLE_PARTICIPANT);
        final int visitors = getContactCount(JabberServiceContact.ROLE_VISITOR);

        addLayerToListOfSubcontacts("list_of_moderators", moderators, JabberServiceContact.ROLE_MODERATOR);
        addLayerToListOfSubcontacts("list_of_participants", participants, JabberServiceContact.ROLE_PARTICIPANT);
        addLayerToListOfSubcontacts("list_of_visitors", visitors, JabberServiceContact.ROLE_VISITOR);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getViewTypeCount() {
        return ITEM_TYPECOUNT;
    }

    @Override
    public int getItemViewType(int position) {
        Object o = items.get(position);
        if (o instanceof String) return ITEM_GROUP;
        if (o instanceof JabberContact.SubContact) return ITEM_CONTACT;
        return -1;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public final int getRole(String nick) {
        JabberContact.SubContact c = getContact(nick);
        final int priority = (null == c) ? JabberServiceContact.ROLE_VISITOR : c.priority;
        return priority;
    }

    public final int getAffiliation(String nick) {
        JabberContact.SubContact c = getContact(nick);
        final int priorityA = (null == c) ? JabberServiceContact.AFFILIATION_NONE : c.priorityA;
        return priorityA;
    }

    private final int getContactCount(byte priority) {
        int count = 0;
        Vector subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact) subcontacts.elementAt(i);
            if (contact.priority == priority) {
                count++;
            }
        }
        return (count);
    }

    private final JabberContact.SubContact getContact(String nick) {
        if (TextUtils.isEmpty(nick)) {
            return null;
        }
        Vector subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact) subcontacts.elementAt(i);
            if (nick.equals(contact.resource)) {
                return contact;
            }
        }
        return null;
    }

    private void addLayerToListOfSubcontacts(String layer, int size, byte priority) {
        boolean hasLayer = false;
        items.add(JLocale.getString(layer) + "(" + size + ")");
        Vector subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact) subcontacts.elementAt(i);
            if (contact.priority == priority) {
                items.add(contact);
                hasLayer = true;
            }
        }
        if (!hasLayer) {
            items.remove(items.size() - 1);
            return;
        }
    }

    public String getCurrentSubContact(Object o) {
        if (o instanceof JabberContact.SubContact) {
            JabberContact.SubContact c = (JabberContact.SubContact)o;
            return c.resource;
        }
        return null;
    }
    public Contact getContactForVCard(String nick) {
        String jid = Jid.realJidToSawimJid(conference.getUserId() + "/" + nick);
        return protocol.createTempContact(jid);
    }
    public Contact getPrivateContact(String nick) {
        String jid = Jid.realJidToSawimJid(conference.getUserId() + "/" + nick);
        return protocol.createTempContact(jid);
    }

    public void setMucRole(String nick, String role) {
        protocol.getConnection().setMucRole(conference.getUserId(), nick, role);
    }
    public void setMucAffiliation(String nick, String affiliation) {
        JabberContact.SubContact c = conference.getExistSubContact(nick);
        if ((null == c) || (null == c.realJid)) {
            return;
        }
        protocol.getConnection().setMucAffiliation(conference.getUserId(),
                c.realJid, affiliation);
    }
    public void setMucRoleR(String nick, String role, String setReason) {
        protocol.getConnection().setMucRoleR(conference.getUserId(), nick, role, setReason);
    }
    public void setMucAffiliationR(String nick, String affiliation, String setReason) {
        JabberContact.SubContact c = conference.getExistSubContact(nick);
        if ((null == c) || (null == c.realJid)) {
            return;
        }
        protocol.getConnection().setMucAffiliationR(conference.getUserId(),
                c.realJid, affiliation, setReason);
    }

    @Override
    public View getView(int i, View convView, ViewGroup viewGroup) {
        Object o = items.get(i);
        if (o instanceof String) return layerView.getView(convView, (String)o);
        if (o instanceof JabberContact.SubContact) return mucUserView.getView(convView, (JabberContact.SubContact)o);
        return convView;
    }
}
