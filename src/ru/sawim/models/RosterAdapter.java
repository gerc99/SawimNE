package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.XStatusInfo;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.widget.roster.RosterItemView;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.comm.Util;
import sawim.modules.tracking.Tracking;
import sawim.roster.RosterHelper;
import sawim.roster.TreeNode;

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
    private int type;
    private List<Object> items = new ArrayList<Object>();
    private Vector updateQueue = new Vector();

    public RosterAdapter(Context context) {
        this.context = context;
    }

    public void setType(int type) {
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
    public Object getItem(int i) {
        if ((items.size() > i) && (i >= 0))
            return items.get(i);
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void putIntoQueue(Group g) {
        if (type == RosterHelper.ACTIVE_CONTACTS) return;
        if (-1 == Util.getIndex(updateQueue, g)) {
            updateQueue.addElement(g);
        }
    }

    public void refreshList() {
        RosterHelper roster = RosterHelper.getInstance();
        Protocol p = roster.getCurrentProtocol();
        if (type == RosterHelper.ACTIVE_CONTACTS) {
            items.clear();
            ChatHistory.instance.sort();
            for (int i = 0; i < roster.getProtocolCount(); ++i) {
                ChatHistory.instance.addLayerToListOfChats(roster.getProtocol(i), items);
            }
        } else {
            if (p == null) return;
            while (!updateQueue.isEmpty()) {
                Group group = (Group) updateQueue.firstElement();
                updateQueue.removeElementAt(0);
                synchronized (p.getRosterLockObject()) {
                    roster.updateGroup(group);
                }
            }
            items.clear();
            synchronized (p.getRosterLockObject()) {
                if (roster.useGroups) {
                    roster.rebuildFlatItemsWG(p, items);
                } else {
                    roster.rebuildFlatItemsWOG(p, items);
                }
            }
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
        rosterItemView.itemNameColor = Scheme.getColor(Scheme.THEME_GROUP);
        rosterItemView.itemNameFont = Typeface.DEFAULT;
        rosterItemView.itemName = g.getText();

        Icon icGroup = g.getLeftIcon(null);
        if (icGroup != null)
            rosterItemView.itemFirstImage = icGroup.getImage().getBitmap();

        BitmapDrawable messIcon = ChatHistory.instance.getUnreadMessageIcon(g.getContacts());
        if (!g.isExpanded() && messIcon != null)
            rosterItemView.itemFourthImage = messIcon.getBitmap();
    }

    void populateFromContact(RosterItemView rosterItemView, RosterHelper roster, Protocol p, Contact item) {
        rosterItemView.itemNameColor = Scheme.getColor(item.getTextTheme());
        rosterItemView.itemNameFont = item.hasChat() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        rosterItemView.itemName = (item.subcontactsS() == 0) ?
                item.getText() : item.getText() + " (" + item.subcontactsS() + ")";
        if (SawimApplication.showStatusLine) {
            String statusMessage = roster.getStatusMessage(item);
            rosterItemView.itemDescColor = Scheme.getColor(Scheme.THEME_CONTACT_STATUS);
            rosterItemView.itemDesc = statusMessage;
        }

        Icon icStatus = item.getLeftIcon(p);
        if (icStatus != null)
            rosterItemView.itemFirstImage = icStatus.getImage().getBitmap();
        if (item.isTyping()) {
            rosterItemView.itemFirstImage = Message.getIcon(Message.ICON_TYPE).getBitmap();
        } else {
            BitmapDrawable icMess = Message.getIcon((byte) item.getUnreadMessageIcon());
            if (icMess != null)
                rosterItemView.itemFirstImage = icMess.getBitmap();
        }

        if (item.getXStatusIndex() != XStatusInfo.XSTATUS_NONE) {
            XStatusInfo xStatusInfo = p.getXStatusInfo();
            if (xStatusInfo != null)
                rosterItemView.itemSecondImage = xStatusInfo.getIcon(item.getXStatusIndex()).getImage().getBitmap();
        }

        if (!item.isTemp()) {
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
                    rosterItemView.itemThirdImage = Contact.serverListsIcons.iconAt(privacyList).getImage().getBitmap();
            } else {
                rosterItemView.itemThirdImage = SawimResources.authIcon.getBitmap();
            }
        }

        Icon icClient = (null != p.clientInfo) ? p.clientInfo.getIcon(item.clientIndex) : null;
        if (icClient != null && !SawimApplication.hideIconsClient)
            rosterItemView.itemFourthImage = icClient.getImage().getBitmap();

        String id = item.getUserId();
        if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE)
            rosterItemView.itemFifthImage = ((BitmapDrawable) Tracking.getTrackIcon(id)).getBitmap();
    }

    public Drawable getImageChat(Chat chat, boolean showMess) {
        if (chat.getContact().isTyping()) {
            return Message.getIcon(Message.ICON_TYPE);
        } else {
            Icon icStatus = chat.getContact().getLeftIcon(chat.getProtocol());
            Drawable icMess = Message.getIcon((byte) chat.getContact().getUnreadMessageIcon());
            return icMess == null || !showMess ? icStatus.getImage() : icMess;
        }
    }

    void setShowDivider(RosterItemView rosterItemView, boolean value) {
        rosterItemView.isShowDivider = value;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = new RosterItemView(context);
        }
        RosterHelper roster = RosterHelper.getInstance();
        Protocol protocol = roster.getCurrentProtocol();
        Object o = getItem(i);
        RosterItemView rosterItemView = (RosterItemView) convertView;
        rosterItemView.setNull();
        if (o != null)
            if (type == RosterHelper.ACTIVE_CONTACTS) {
                if (o instanceof String) {
                    rosterItemView.addLayer((String) o);
                }
                if (o instanceof Chat) {
                    Chat chat = (Chat) o;
                    populateFromContact(rosterItemView, RosterHelper.getInstance(), chat.getProtocol(), chat.getContact());
                }
                setShowDivider(rosterItemView, getItem(i + 1) instanceof Chat);
            } else {
                TreeNode treeNode = (TreeNode) o;
                if (type != RosterHelper.ALL_CONTACTS) {
                    if (type != RosterHelper.ACTIVE_CONTACTS)
                        if (treeNode.isGroup()) {
                            populateFromGroup(rosterItemView, (Group) o);
                        } else if (treeNode.isContact()) {
                            populateFromContact(rosterItemView, roster, protocol, (Contact) o);
                        }
                } else {
                    if (treeNode.isGroup()) {
                        populateFromGroup(rosterItemView, (Group) o);
                    } else if (treeNode.isContact()) {
                        populateFromContact(rosterItemView, roster, protocol, (Contact) o);
                    }
                }
                setShowDivider(rosterItemView, true);
            }
        rosterItemView.repaint();
        return convertView;
    }
}