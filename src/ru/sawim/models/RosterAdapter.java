package ru.sawim.models;

import android.content.Context;
import android.widget.*;
import sawim.chat.ChatHistory;
import sawim.comm.Util;
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

    public static final int ALL_CONTACTS = 0;
    public static final int ONLINE_CONTACTS = 1;
    public static final int ACTIVE_CONTACTS = 2;
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
        if (roster.getCurrPage() == RosterAdapter.ACTIVE_CONTACTS) {
            items.clear();
            ChatHistory.instance.sort();
            for (int i = 0; i < ChatHistory.instance.historyTable.size(); ++i) {
                items.add(ChatHistory.instance.contactAt(i));
            }
        } else {
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
        }
        notifyDataSetChanged();
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (observer != null) {
            super.unregisterDataSetObserver(observer);
        }
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = new RosterItemView(context);
        }
        Protocol protocol = roster.getCurrentProtocol();
        TreeNode o = getItem(i);
        if (o != null)
            if (type != ALL_CONTACTS && o.isContact()) {
                if (type != ACTIVE_CONTACTS)
                    ((RosterItemView) convertView).populateFromContact(roster, protocol, (Contact) o);
                else
                    if (((Contact) o).hasChat())
                        ((RosterItemView) convertView).populateFromContact(roster, ((Contact) o).getProtocol(), (Contact) o);
            } else {
                if (o.isGroup()) {
                    ((RosterItemView) convertView).populateFromGroup((Group) o);
                } else if (o.isContact()) {
                    ((RosterItemView) convertView).populateFromContact(roster, protocol, (Contact) o);
                }
            }
        return convertView;
    }
}