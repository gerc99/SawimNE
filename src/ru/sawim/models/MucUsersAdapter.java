package ru.sawim.models;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import sawim.Options;
import sawim.util.JLocale;
import protocol.Contact;
import protocol.jabber.Jabber;
import protocol.jabber.JabberContact;
import protocol.jabber.JabberServiceContact;
import protocol.jabber.Jid;

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
    private JabberServiceContact conference;
    private List<Object> items = new ArrayList<Object>();
    private Jabber protocol;
    private static ImageList affiliationIcons = ImageList.createImageList("/jabber-affiliations.png");
    Context context;

    public MucUsersAdapter(Context context, Jabber jabber, JabberServiceContact conf) {
        this.context = context;
        protocol = jabber;
        conference = conf;
        update();
    }

    public void update() {
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
        if (o == null) return convView;
        ItemWrapper wr;
        if (convView == null) {
            LayoutInflater inf = LayoutInflater.from(context);
            convView = inf.inflate(R.layout.muc_users_item, null);
            wr = new ItemWrapper(convView);
            convView.setTag(wr);
        } else {
            wr = (ItemWrapper) convView.getTag();
        }
        if (o instanceof String) {
            convView.setPadding(0, 0, 0, 0);
            wr.populateLayerFrom((String) o);
        }
        if (o instanceof JabberContact.SubContact)
            wr.populateFrom(protocol, o);
        return convView;
    }

    static class ItemWrapper {
        View item = null;
        private TextView itemName = null;
        private ImageView itemStatusImage = null;
        private ImageView itemAffilationImage = null;
        private ImageView itemClientImage = null;

        public ItemWrapper(View item) {
            this.item = item;
        }

        void populateLayerFrom(String layer) {
            getItemStatusImage().setVisibility(ImageView.GONE);
            getItemAffilationImage().setVisibility(ImageView.GONE);
            getItemClientImage().setVisibility(ImageView.GONE);
            TextView itemLayer = getItemName();
            itemLayer.setTextSize(General.getFontSize() - 2);
            itemLayer.setTypeface(Typeface.SANS_SERIF);
            itemLayer.setText(layer);
            itemLayer.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        }

        void populateFrom(Jabber protocol, Object o) {
            JabberContact.SubContact c = (JabberContact.SubContact) o;
            TextView itemName = getItemName();
            itemName.setTextSize(General.getFontSize());
            itemName.setText(c.resource);
            itemName.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
            getItemStatusImage().setImageBitmap(protocol.getStatusInfo().getIcon(c.status).getImage());
            getItemAffilationImage().setImageBitmap(affiliationIcons.iconAt(JabberServiceContact.getAffiliationName(c.priorityA)).getImage());
            Icon ic = protocol.clientInfo.getIcon(c.client);
            ImageView itemClientImage = getItemClientImage();
            if (ic != null && !Options.getBoolean(Options.OPTION_HIDE_ICONS_CLIENTS)) {
                itemClientImage.setVisibility(ImageView.VISIBLE);
                itemClientImage.setImageBitmap(ic.getImage());
            } else {
                itemClientImage.setVisibility(ImageView.GONE);
            }
        }

        public ImageView getItemStatusImage() {
            if (itemStatusImage == null) {
                itemStatusImage = (ImageView) item.findViewById(R.id.first_image);
            }
            return itemStatusImage;
        }

        public ImageView getItemAffilationImage() {
            if (itemAffilationImage == null) {
                itemAffilationImage = (ImageView) item.findViewById(R.id.affilationImage);
            }
            return itemAffilationImage;
        }

        public TextView getItemName() {
            if (itemName == null) {
                itemName = (TextView) item.findViewById(R.id.item_name);
            }
            return itemName;
        }

        public ImageView getItemClientImage() {
            if (itemClientImage == null) {
                itemClientImage = (ImageView) item.findViewById(R.id.fourth_rule_image);
            }
            return itemClientImage;
        }
    }
}
