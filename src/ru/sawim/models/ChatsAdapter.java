package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Contact;
import protocol.Protocol;
import protocol.XStatusInfo;
import ru.sawim.General;
import ru.sawim.Scheme;
import ru.sawim.widget.roster.RosterItemView;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.modules.tracking.Tracking;
import sawim.roster.RosterHelper;

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
        for (int i = 0; i < RosterHelper.getInstance().getProtocolCount(); ++i) {
            ChatHistory.instance.addLayerToListOfChats(RosterHelper.getInstance().getProtocol(i), items);
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

    @Override
    public boolean isEnabled(int position) {
        Object o = getItem(position);
        if (o instanceof String) return false;
        return super.isEnabled(position);
    }

    void populateFromContact(RosterItemView rosterItemView, RosterHelper roster, Protocol p, Contact item) {
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
            rosterItemView.itemFirstImage = icStatus.getImage().getBitmap();
        if (item.isTyping()) {
            rosterItemView.itemFirstImage = Message.getIcon(Message.ICON_TYPE).getBitmap();
        } else {
            BitmapDrawable icMess = Message.getIcon((byte) item.getUnreadMessageIcon());
            if (icMess != null)
                rosterItemView.itemFirstImage = icMess.getBitmap();
        }

        if (item.getXStatusIndex() != XStatusInfo.XSTATUS_NONE)
            rosterItemView.itemSecondImage = p.getXStatusInfo().getIcon(item.getXStatusIndex()).getImage().getBitmap();

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
                    rosterItemView.itemThirdImage = item.serverListsIcons.iconAt(privacyList).getImage().getBitmap();
            } else {
                rosterItemView.itemThirdImage = icAuth.getImage().getBitmap();
            }
        }

        //rosterItemView.itemFourthImage

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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new RosterItemView(context);
        }
        RosterItemView rosterItemView = ((RosterItemView) convertView);
        Object o = getItem(position);
        if (o instanceof String) {
            rosterItemView.addLayer((String) o);
        }
        if (o instanceof Chat) {
            Chat chat = (Chat) o;
            populateFromContact(rosterItemView, RosterHelper.getInstance(), chat.getProtocol(), chat.getContact());
            rosterItemView.setBackgroundColor(0);
        }
        ((RosterItemView) convertView).repaint();
        return convertView;
    }
}
