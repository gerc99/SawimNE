package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.widget.SimpleItemView;
import ru.sawim.widget.Util;
import ru.sawim.widget.roster.RosterItemView;
import sawim.chat.ChatHistory;
import sawim.roster.RosterHelper;
import sawim.util.JLocale;

/**
 * Created by admin on 01.03.14.
 */
public class ProtocolsAdapter extends BaseAdapter {

    private Context context;
    private int activeItem;

    public ProtocolsAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return RosterHelper.getInstance().getProtocolCount() + 1;
    }

    @Override
    public Protocol getItem(int position) {
        return RosterHelper.getInstance().getProtocol(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setActiveItem(int activeItem) {
        this.activeItem = activeItem;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new RosterItemView(context);
        }
        RosterItemView rosterItemView = (RosterItemView) convertView;
        rosterItemView.setNull();
        boolean isActiveContacts = position == RosterHelper.getInstance().getProtocolCount();
        if (isActiveContacts) {
            rosterItemView.itemName = JLocale.getString(R.string.active_contacts);
        } else {
            Protocol protocol = getItem(position);
            Icon statusIcon = protocol.getCurrentStatusIcon();
            BitmapDrawable icon = null;
            BitmapDrawable messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
            if (statusIcon != null)
                icon = statusIcon.getImage();
            if (null != messageIcon)
                icon = messageIcon;
            if (icon != null)
                rosterItemView.itemFirstImage = icon.getBitmap();
            rosterItemView.itemName = protocol.getUserId();
        }
        if (Util.isNeedToFixSpinnerAdapter()) {
            rosterItemView.itemNameColor = 0xFF000000;
        } else {
            rosterItemView.itemNameColor = Scheme.getColor(Scheme.THEME_TEXT);
        }
        rosterItemView.itemNameFont = Typeface.DEFAULT;
        rosterItemView.setBackgroundColor(position == activeItem ? Scheme.getColor(Scheme.THEME_ITEM_SELECTED) : 0);
        rosterItemView.isShowDivider = true;
        rosterItemView.repaint();
        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new SimpleItemView(context);
        }
        SimpleItemView itemView = (SimpleItemView) convertView;
        Protocol protocol = getItem(position);
        boolean isActiveContacts = position == RosterHelper.getInstance().getProtocolCount();
        if (!isActiveContacts) {
            Icon statusIcon = protocol.getCurrentStatusIcon();
            if (statusIcon != null && position != RosterHelper.getInstance().getProtocolCount())
                itemView.setImage(statusIcon.getImage().getBitmap());
        }
        itemView.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        itemView.setTypeface(Typeface.DEFAULT);
        itemView.setTextSize(SawimApplication.getFontSize());
        itemView.setText(isActiveContacts ? JLocale.getString(R.string.active_contacts) : protocol.getUserId());
        return convertView;
    }


}
