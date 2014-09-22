package ru.sawim.models;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppContact;
import protocol.xmpp.XmppServiceContact;
import ru.sawim.*;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.Util;
import ru.sawim.icons.Icon;
import ru.sawim.icons.ImageCache;
import ru.sawim.io.FileSystem;
import ru.sawim.widget.roster.RosterItemView;

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
    private XmppServiceContact conference;
    private List<Object> items = new ArrayList<Object>();
    private Xmpp protocol;

    public void init(Xmpp xmpp, XmppServiceContact conf) {
        protocol = xmpp;
        conference = conf;
        update();
    }

    public void update() {
        items.clear();
        Util.sort(conference.subcontacts);
        final int moderators = getContactCount(XmppServiceContact.ROLE_MODERATOR);
        final int participants = getContactCount(XmppServiceContact.ROLE_PARTICIPANT);
        final int visitors = getContactCount(XmppServiceContact.ROLE_VISITOR);

        addLayerToListOfSubcontacts(R.string.list_of_moderators, moderators, XmppServiceContact.ROLE_MODERATOR);
        addLayerToListOfSubcontacts(R.string.list_of_participants, participants, XmppServiceContact.ROLE_PARTICIPANT);
        addLayerToListOfSubcontacts(R.string.list_of_visitors, visitors, XmppServiceContact.ROLE_VISITOR);
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
        if (o instanceof XmppContact.SubContact) return ITEM_CONTACT;
        return -1;
    }

    @Override
    public boolean isEnabled(int position) {
        Object o = items.get(position);
        if (o instanceof String) return false;
        return super.isEnabled(position);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        if ((items.size() > position) && (position >= 0))
            return items.get(position);
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public final int getRole(String nick) {
        XmppContact.SubContact c = getContact(nick);
        final int priority = (null == c) ? XmppServiceContact.ROLE_VISITOR : c.priority;
        return priority;
    }

    public final int getAffiliation(String nick) {
        XmppContact.SubContact c = getContact(nick);
        final int priorityA = (null == c) ? XmppServiceContact.AFFILIATION_NONE : c.priorityA;
        return priorityA;
    }

    private final int getContactCount(byte priority) {
        int count = 0;
        Vector<XmppContact.SubContact> subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            XmppContact.SubContact contact = subcontacts.elementAt(i);
            if (contact != null)
                if (contact.priority == priority) {
                    count++;
                }
        }
        return (count);
    }

    private final XmppContact.SubContact getContact(String nick) {
        if (TextUtils.isEmpty(nick)) {
            return null;
        }
        Vector<XmppContact.SubContact> subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            XmppContact.SubContact contact = subcontacts.elementAt(i);
            if (nick.equals(contact.resource)) {
                return contact;
            }
        }
        return null;
    }

    private void addLayerToListOfSubcontacts(int layer, int size, byte priority) {
        boolean hasLayer = false;
        items.add(JLocale.getString(layer)/* + "(" + size + ")"*/);
        Vector<XmppContact.SubContact> subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            XmppContact.SubContact contact = subcontacts.elementAt(i);
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
        if (o instanceof XmppContact.SubContact) {
            XmppContact.SubContact c = (XmppContact.SubContact) o;
            return c.resource;
        }
        return null;
    }

    public void setMucRole(String nick, String role) {
        protocol.getConnection().setMucRole(conference.getUserId(), nick, role);
    }

    public void setMucAffiliation(String nick, String affiliation) {
        XmppContact.SubContact c = conference.getExistSubContact(nick);
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
        XmppContact.SubContact c = conference.getExistSubContact(nick);
        if ((null == c) || (null == c.realJid)) {
            return;
        }
        protocol.getConnection().setMucAffiliationR(conference.getUserId(),
                c.realJid, affiliation, setReason);
    }

    void setShowDivider(RosterItemView rosterItemView, boolean value) {
        rosterItemView.isShowDivider = value;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        Object o = items.get(i);
        if (convertView == null) {
            convertView = new RosterItemView(viewGroup.getContext());
        }
        RosterItemView rosterItemView = (RosterItemView) convertView;
        rosterItemView.setNull();
        if (o == null) return rosterItemView;
        if (o instanceof String) {
            rosterItemView.addLayer((String) o);
            rosterItemView.itemNameFont = Typeface.DEFAULT_BOLD;
        }
        if (o instanceof XmppContact.SubContact)
            populateFrom(rosterItemView, protocol, (XmppContact.SubContact) o);
        setShowDivider(rosterItemView, getItem(i + 1) instanceof XmppContact.SubContact);
        ((RosterItemView) convertView).repaint();
        return rosterItemView;
    }

    void populateFrom(final RosterItemView rosterItemView, Xmpp protocol, XmppContact.SubContact c) {
        Bitmap avatar = ImageCache.getInstance().get(FileSystem.openDir(FileSystem.AVATARS), SawimApplication.getExecutor(), c.avatarHash,
                null, ru.sawim.widget.Util.dipToPixels(rosterItemView.getContext(), SawimApplication.AVATAR_SIZE), Scheme.getColor(Scheme.THEME_BACKGROUND), new ImageCache.OnImageLoadListener() {
                    @Override
                    public void onLoad() {
                        rosterItemView.post(new Runnable() {
                            @Override
                            public void run() {
                                notifyDataSetChanged();
                            }
                        });
                    }
                });
        rosterItemView.itemFirstImage = avatar;
        rosterItemView.itemSecondImage = protocol.getStatusInfo().getIcon(c.status).getImage().getBitmap();
        rosterItemView.itemNameColor = Scheme.getColor(Scheme.THEME_TEXT);
        rosterItemView.itemNameFont = Typeface.DEFAULT;
        rosterItemView.itemName = c.resource;
        if (SawimApplication.showStatusLine) {
            String statusMessage = c.statusText == null ? protocol.getStatusInfo().getName(c.status) : c.statusText;
            rosterItemView.itemDescColor = Scheme.getColor(Scheme.THEME_CONTACT_STATUS);
            rosterItemView.itemDesc = statusMessage;
        }
        Icon ic = protocol.clientInfo.getIcon(c.client);
        if (ic != null && !Options.getBoolean(JLocale.getString(R.string.pref_hide_icons_clients))) {
            rosterItemView.itemFifthImage = ic.getImage().getBitmap();
        }
        rosterItemView.itemSixthImage = SawimResources.affiliationIcons.iconAt(XmppServiceContact.getAffiliationName(c.priorityA)).getImage().getBitmap();
    }
}
