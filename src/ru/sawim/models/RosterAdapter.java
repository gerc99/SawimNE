package ru.sawim.models;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import DrawControls.tree.TreeNode;
import DrawControls.tree.VirtualContactList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.ui.base.Scheme;
import protocol.*;
import ru.sawim.General;
import ru.sawim.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 05.05.13
 * Time: 17:03
 * To change this template use File | Settings | File Templates.
 */
/*public class RosterAdapter extends BaseAdapter {

    private List<TreeNode> items = new ArrayList<TreeNode>();
    private ContactViewFactory cvf;
    private GroupViewFactory gvf;

    public final static int ITEM_CONTACT = 0;
    public final static int ITEM_GROUP = 1;
    public final static int ITEM_TYPECOUNT = 2;

    public RosterAdapter(LayoutInflater inf, VirtualContactList vcl) {
        gvf = new GroupViewFactory(inf);
        cvf = new ContactViewFactory(inf, vcl);
        items = vcl.drawItems;
    }

    //public void setItems(TreeNode items) {
    //    this.items.add(items);
    //}

    //public void clear() {
    //    items.clear();
    //}

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
        if (o instanceof Group) return ITEM_GROUP;
        if (o instanceof Contact) return ITEM_CONTACT;
        return -1;
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
    public View getView(int i, View convView, ViewGroup viewGroup) {
        TreeNode o = items.get(i);
        if (o instanceof Group) return gvf.getView(convView, (Group) o);
        if (o instanceof Contact) return cvf.getView(convView, (Contact) o);
        return null;
    }
}*/
public class RosterAdapter extends BaseAdapter {

    VirtualContactList vcl;
    private List<TreeNode> items = new ArrayList<TreeNode>();
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