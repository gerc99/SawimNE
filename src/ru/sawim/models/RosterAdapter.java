package ru.sawim.models;

import DrawControls.icons.Icon;
import DrawControls.tree.TreeNode;
import DrawControls.tree.VirtualContactList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import protocol.Contact;
import protocol.Group;
import ru.sawim.R;
import ru.sawim.Scheme;
import sawim.chat.message.Message;

import java.util.List;
import java.util.zip.Inflater;

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
    public static final int OPEN_CHATS = 2;
    private VirtualContactList vcl;
    private List<TreeNode> items;
    private LayoutInflater mInflater;
    private int type;

    public RosterAdapter(LayoutInflater inf, VirtualContactList vcl, List<TreeNode> drawItems, int type) {
        mInflater = inf;
        this.vcl = vcl;
        items = drawItems;
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

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        TreeNode o = getItem(i);
        if (type == ONLINE_CONTACTS && o.isContact()) {
            Contact c = (Contact) o;
            ViewHolderRoster holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.roster_item, null);
                holder = new ViewHolderRoster(vcl, convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolderRoster) convertView.getTag();
            }
            holder.populateFromContact(c);
        } else if (type == OPEN_CHATS) {
            ItemWrapper wr;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.roster_chats, null);
                wr = new ItemWrapper(convertView);
                convertView.setTag(wr);
            } else {
                wr = (ItemWrapper) convertView.getTag();
            }
            if (o.isContact())
                wr.populateFrom((Contact) o);
        } else {
            ViewHolderRoster holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.roster_item, null);
                holder = new ViewHolderRoster(vcl, convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolderRoster) convertView.getTag();
            }
            if (o != null)
                if (o.isGroup()) {
                    holder.populateFromGroup((Group) o);
                } else if (o.isContact()) {
                    holder.populateFromContact((Contact) o);
                }
        }
        return convertView;
    }

    static class ItemWrapper {
        View item = null;
        private TextView itemName = null;
        private ImageView itemImage = null;

        public ItemWrapper(View item) {
            this.item = item;
        }

        void populateFrom(Contact item) {
            TextView itemName = getItemName();
            itemName.setText(item.getName());
            itemName.setTextColor(Scheme.getColor(Scheme.THEME_CONTACT_WITH_CHAT));
            Icon icMess = Message.msgIcons.iconAt(item.getUnreadMessageIcon());
            Icon icStatus = item.
                    getProtocol().
                    getStatusInfo().
                    getIcon(
                            item.getStatusIndex());
            ImageView itemImage = getItemImage();
            if (icMess == null) {
                if (icStatus == null) {
                    itemImage.setVisibility(ImageView.GONE);
                } else {
                    itemImage.setVisibility(ImageView.VISIBLE);
                    itemImage.setImageBitmap(icStatus.getImage());
                }
            } else {
                itemImage.setVisibility(ImageView.VISIBLE);
                itemImage.setImageBitmap(icMess.getImage());
            }
        }

        public ImageView getItemImage() {
            if (itemImage == null) {
                itemImage = (ImageView) item.findViewById(R.id.image);
            }
            return itemImage;
        }

        public TextView getItemName() {
            if (itemName == null) {
                itemName = (TextView) item.findViewById(R.id.item_name);
            }
            return itemName;
        }
    }
}