package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Contact;
import protocol.Protocol;
import protocol.XStatusInfo;
import ru.sawim.General;
import ru.sawim.Scheme;
import ru.sawim.widget.RosterItemView;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.modules.tracking.Tracking;
import sawim.roster.Roster;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 09.10.13
 * Time: 20:20
 * To change this template use File | Settings | File Templates.
 */
public class ChatsAdapter extends BaseAdapter {

    private final Context context;
    private List<Object> items = new ArrayList<Object>();

    public ChatsAdapter(Context context) {
        this.context = context;
    }

    public void refreshList() {
        items.clear();
        ChatHistory.instance.sort();
        for (int i = 0; i < Roster.getInstance().getProtocolCount(); ++i) {
            ChatHistory.instance.addLayerToListOfChats(Roster.getInstance().getProtocol(i), items);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new RosterItemView(context);
        }
        RosterItemView rosterItemView = ((RosterItemView) convertView);
        Object o = getItem(position);
        if (o instanceof String) {
            rosterItemView.addLayer((String) o);
            rosterItemView.setBackgroundColor(Scheme.getColor(Scheme.THEME_PROTOCOL_BACKGROUND));
        }
        if (o instanceof Chat) {
            Chat chat = (Chat) o;
            populateFromContact(rosterItemView, Roster.getInstance(), chat.getProtocol(), chat.getContact());
            rosterItemView.setBackgroundColor(0);
        }
        ((RosterItemView) convertView).repaint();
        return convertView;
    }
}
