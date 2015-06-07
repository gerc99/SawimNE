package ru.sawim.models;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Contact;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppContact;
import protocol.xmpp.XmppServiceContact;
import ru.sawim.*;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.Util;
import ru.sawim.icons.Icon;
import ru.sawim.icons.ImageCache;
import ru.sawim.io.FileSystem;
import ru.sawim.roster.Layer;
import ru.sawim.roster.TreeNode;
import ru.sawim.widget.roster.RosterItemView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    private List<TreeNode> items = new ArrayList<>();
    private Xmpp protocol;

    File avatarsFolder;

    {
        avatarsFolder = FileSystem.openDir(FileSystem.AVATARS);
    }

    public void init(Xmpp xmpp, XmppServiceContact conf) {
        protocol = xmpp;
        conference = conf;
        update();
    }

    public void update() {
        items.clear();

        List<XmppContact.SubContact> subContacts = new ArrayList<>();
        for (XmppContact.SubContact contact : conference.subcontacts.values()) {
            subContacts.add(contact);
        }
        Util.sort(subContacts);

        final int moderators = getContactCount(XmppServiceContact.ROLE_MODERATOR);
        final int participants = getContactCount(XmppServiceContact.ROLE_PARTICIPANT);
        final int visitors = getContactCount(XmppServiceContact.ROLE_VISITOR);
        addLayerToListOfSubcontacts(subContacts, R.string.list_of_moderators, moderators, XmppServiceContact.ROLE_MODERATOR);
        addLayerToListOfSubcontacts(subContacts, R.string.list_of_participants, participants, XmppServiceContact.ROLE_PARTICIPANT);
        addLayerToListOfSubcontacts(subContacts, R.string.list_of_visitors, visitors, XmppServiceContact.ROLE_VISITOR);
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
        TreeNode treeNode = getItem(position);
        if (treeNode != null) {
            if (treeNode.getType() == TreeNode.LAYER) return ITEM_GROUP;
            if (treeNode.getType() == TreeNode.CONTACT) return ITEM_CONTACT;
        }
        return -1;
    }

    @Override
    public boolean isEnabled(int position) {
        TreeNode treeNode = items.get(position);
        if (treeNode.getType() == TreeNode.LAYER) return false;
        return super.isEnabled(position);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public TreeNode getItem(int position) {
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
        Collection<XmppContact.SubContact> subcontacts = conference.subcontacts.values();
        for (XmppContact.SubContact contact : subcontacts) {
            if (contact != null)
                if (contact.priority == priority) {
                    count++;
                }
        }
        return (count);
    }

    private XmppContact.SubContact getContact(String nick) {
        if (TextUtils.isEmpty(nick)) {
            return null;
        }
        Collection<XmppContact.SubContact> subcontacts = conference.subcontacts.values();
        for (XmppContact.SubContact contact : subcontacts) {
            if (nick.equals(contact.resource)) {
                return contact;
            }
        }
        return null;
    }

    private void addLayerToListOfSubcontacts(List<XmppContact.SubContact> subcontacts, int layerStrId, int size, byte priority) {
        boolean hasLayer = false;
        Layer layer = new Layer(JLocale.getString(layerStrId), priority);
        items.add(layer/* + "(" + size + ")"*/);
        for (XmppContact.SubContact contact : subcontacts) {
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
        TreeNode treeNode = items.get(i);
        int itemViewType = getItemViewType(i);
        if (convertView == null) {
            convertView = new RosterItemView(viewGroup.getContext());
        }
        RosterItemView rosterItemView = (RosterItemView) convertView;
        rosterItemView.setNull();
        if (treeNode == null) return rosterItemView;
        if (itemViewType == ITEM_GROUP) {
            rosterItemView.addLayer(treeNode.getText());
            rosterItemView.itemNameFont = Typeface.DEFAULT_BOLD;
        }
        if (itemViewType == ITEM_CONTACT)
            populateFrom(rosterItemView, protocol, (XmppContact.SubContact) treeNode);
        setShowDivider(rosterItemView, getItemViewType(i + 1) == ITEM_CONTACT);
        ((RosterItemView) convertView).repaint();
        return rosterItemView;
    }

    void populateFrom(final RosterItemView rosterItemView, Xmpp protocol, XmppContact.SubContact c) {
        if (Options.getBoolean(JLocale.getString(R.string.pref_users_avatars))) {
            String hash = (c.avatarHash == null || c.avatarHash.isEmpty()) ?
                    c.resource.isEmpty() ? "" : c.resource.substring(0, 1)
                    : c.avatarHash;
            Bitmap avatar = ImageCache.getInstance().get(avatarsFolder, SawimApplication.getExecutor(), hash,
                    SawimResources.DEFAULT_AVATAR, new ImageCache.OnImageLoadListener() {
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
            rosterItemView.avatarBorderColor = Contact.getStatusColor(c.status);
        }

    //    rosterItemView.itemSecondImage = protocol.getStatusInfo().getIcon(c.status).getImage().getBitmap();
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
