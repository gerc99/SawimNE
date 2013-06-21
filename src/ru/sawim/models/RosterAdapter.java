package ru.sawim.models;

import DrawControls.tree.TreeNode;
import DrawControls.tree.VirtualContactList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Contact;
import protocol.Group;
import ru.sawim.R;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 05.05.13
 * Time: 17:03
 * To change this template use File | Settings | File Templates.
 */
public class RosterAdapter extends BaseAdapter {

    VirtualContactList vcl;
    private List<TreeNode> items;
    private LayoutInflater mInflater;

    public RosterAdapter(LayoutInflater inf, VirtualContactList vcl, List<TreeNode> drawItems) {
        mInflater = inf;
        this.vcl = vcl;
        items = drawItems;
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
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        TreeNode o = items.get(i);
        ViewHolderRoster holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.roster_item, null);
            holder = new ViewHolderRoster(vcl, convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolderRoster) convertView.getTag();
        }
        if (o instanceof Group) {
            holder.populateFromGroup((Group) o);
        } else if (o instanceof Contact) {
            holder.populateFromContact((Contact) o);
        }
        return convertView;
    }
}