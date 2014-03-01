package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import protocol.Protocol;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppContact;
import protocol.xmpp.XmppServiceContact;
import ru.sawim.R;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.widget.MyTextView;
import ru.sawim.widget.roster.RosterItemView;
import sawim.Options;
import sawim.chat.ChatHistory;
import sawim.roster.RosterHelper;

/**
 * Created by admin on 01.03.14.
 */
public class ProtocolsAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inf;

    public ProtocolsAdapter(Context context) {
        this.context = context;
        inf = LayoutInflater.from(this.context);
    }

    @Override
    public int getCount() {
        return RosterHelper.getInstance().getProtocolCount();
    }

    @Override
    public Protocol getItem(int position) {
        return RosterHelper.getInstance().getProtocol(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new RosterItemView(context);
        }
        RosterItemView rosterItemView = (RosterItemView) convertView;
        populateFrom(rosterItemView, getItem(position));
        rosterItemView.isShowDivider = true;
        rosterItemView.repaint();
        return convertView;
    }

    void populateFrom(RosterItemView rosterItemView, Protocol protocol) {
        BitmapDrawable icon = null;
        Icon statusIcon = protocol.getCurrentStatusIcon();
        BitmapDrawable messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
        if (statusIcon != null)
            icon = statusIcon.getImage();
        if (null != messageIcon)
            icon = messageIcon;
        rosterItemView.itemFirstImage = icon.getBitmap();
        rosterItemView.itemNameColor = Scheme.getColor(Scheme.THEME_TEXT);
        rosterItemView.itemNameFont = Typeface.DEFAULT;
        rosterItemView.itemName = protocol.getUserId();
    }
}
