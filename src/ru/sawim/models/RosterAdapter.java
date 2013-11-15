package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.widget.*;
import protocol.XStatusInfo;
import ru.sawim.General;
import ru.sawim.Scheme;
import ru.sawim.widget.roster.RosterItemView;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.comm.Util;
import sawim.modules.tracking.Tracking;
import sawim.roster.TreeNode;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import sawim.roster.Roster;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 05.05.13
 * Time: 17:03
 * To change this template use File | Settings | File Templates.
 */
public class RosterAdapter extends BaseAdapter {

    private final Context context;
    private final Roster roster;
    private int type;
    private List<TreeNode> items = new ArrayList<TreeNode>();
    private Vector updateQueue = new Vector();

    public RosterAdapter(Context context, Roster vcl, int type) {
        this.context = context;
        this.roster = vcl;
        this.type = type;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public TreeNode getItem(int i) {
        if ((items.size() > i) && (i >= 0))
            return items.get(i);
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void putIntoQueue(Group g) {
        if (-1 == Util.getIndex(updateQueue, g)) {
            updateQueue.addElement(g);
        }
    }

    public void buildFlatItems() {
        Protocol p = roster.getCurrentProtocol();
        if (p == null) return;
        while (!updateQueue.isEmpty()) {
            Group group = (Group) updateQueue.firstElement();
            updateQueue.removeElementAt(0);
            roster.updateGroup(group);
        }
        items.clear();
        if (roster.useGroups) {
            roster.rebuildFlatItemsWG(p, items);
        } else {
            roster.rebuildFlatItemsWOG(p, items);
        }
        notifyDataSetChanged();
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (observer != null) {
            super.unregisterDataSetObserver(observer);
        }
    }

    void populateFromGroup(RosterItemView rosterItemView, Group g) {
        rosterItemView.setNull();
        rosterItemView.itemNameColor = Scheme.getColor(Scheme.THEME_GROUP);
        rosterItemView.itemNameFont = Typeface.DEFAULT;
        rosterItemView.itemName = g.getText();

        Icon icGroup = g.getLeftIcon(null);
        if (icGroup != null)
            rosterItemView.itemFirstImage = icGroup.getImage();

        Icon messIcon = ChatHistory.instance.getUnreadMessageIcon(g.getContacts());
        if (!g.isExpanded() && messIcon != null)
            rosterItemView.itemFourthImage = messIcon.getImage();
    }

    void populateFromContact(RosterItemView rosterItemView, Roster roster, Protocol p, Contact item) {
        rosterItemView.setNull();
        rosterItemView.itemNameColor = Scheme.getColor(item.getTextTheme());
        rosterItemView.itemNameFont = item.hasChat() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        rosterItemView.itemName = (item.subcontactsS() == 0) ?
                item.getText() : item.getText() + " (" + item.subcontactsS() + ")";
        if (General.showStatusLine) {
            String statusMessage = roster.getStatusMessage(item);
            rosterItemView.itemDescColor = Scheme.getColor(Scheme.THEME_CONTACT_STATUS);
            rosterItemView.itemDesc = statusMessage;
        }

        Icon icStatus = item.getLeftIcon(p);
        if (icStatus != null)
            rosterItemView.itemFirstImage = icStatus.getImage();
        if (item.isTyping()) {
            rosterItemView.itemFirstImage = Message.msgIcons.iconAt(Message.ICON_TYPE).getImage();
        } else {
            Icon icMess = Message.msgIcons.iconAt(item.getUnreadMessageIcon());
            if (icMess != null)
                rosterItemView.itemFirstImage = icMess.getImage();
        }

        if (item.getXStatusIndex() != XStatusInfo.XSTATUS_NONE)
            rosterItemView.itemSecondImage = p.getXStatusInfo().getIcon(item.getXStatusIndex()).getImage();

        if (!item.isTemp()) {
            Icon icAuth = item.authIcon.iconAt(0);
            if (item.isAuth()) {
                int privacyList = -1;
                if (item.inIgnoreList()) {
                    privacyList = 0;
                } else if (item.inInvisibleList()) {
                    privacyList = 1;
                } else if (item.inVisibleList()) {
                    privacyList = 2;
                }
                if (privacyList != -1)
                    rosterItemView.itemThirdImage = item.serverListsIcons.iconAt(privacyList).getImage();
            } else {
                rosterItemView.itemThirdImage = icAuth.getImage();
            }
        }

        Icon icClient = (null != p.clientInfo) ? p.clientInfo.getIcon(item.clientIndex) : null;
        if (icClient != null && !General.hideIconsClient)
            rosterItemView.itemFourthImage = icClient.getImage();

        String id = item.getUserId();
        if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE)
            rosterItemView.itemFifthImage = (BitmapDrawable) Tracking.getTrackIcon(id);
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = new RosterItemView(context);
        }
        Protocol protocol = roster.getCurrentProtocol();
        TreeNode o = getItem(i);
        RosterItemView rosterItemView = ((RosterItemView) convertView);
        if (o != null)
            if (type != Roster.ALL_CONTACTS) {
                if (type != Roster.ACTIVE_CONTACTS)
                    if (o.isGroup()) {
                        populateFromGroup(rosterItemView, (Group) o);
                    } else if (o.isContact()) {
                        populateFromContact(rosterItemView, roster, protocol, (Contact) o);
                    }
            } else {
                if (o.isGroup()) {
                    populateFromGroup(rosterItemView, (Group) o);
                } else if (o.isContact()) {
                    populateFromContact(rosterItemView, roster, protocol, (Contact) o);
                }
            }
        ((RosterItemView) convertView).repaint();
        return convertView;
    }
}