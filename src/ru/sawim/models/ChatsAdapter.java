package ru.sawim.models;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import ru.sawim.Scheme;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new RosterItemView(context);
        }
        RosterItemView rosterItemView = ((RosterItemView) convertView);
        Object o = getItem(position);
        if (o instanceof String) {
            rosterItemView.addLayer((String) o);
            rosterItemView.setBackgroundColor(Scheme.getInversColor(Scheme.THEME_BACKGROUND));
        }
        if (o instanceof Chat) {
            Chat chat = (Chat) o;
            ((RosterItemView) convertView).populateFromContact(Roster.getInstance(), chat.getProtocol(), chat.getContact());
            rosterItemView.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        }
        ((RosterItemView) convertView).repaint();
        return convertView;
    }
}
