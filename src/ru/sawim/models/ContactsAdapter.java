package ru.sawim.models;

import DrawControls.icons.Icon;
import DrawControls.tree.VirtualContactList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.ui.base.Scheme;
import protocol.Contact;
import ru.sawim.General;
import ru.sawim.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 12.05.13
 * Time: 19:10
 * To change this template use File | Settings | File Templates.
 */
public class ContactsAdapter extends BaseAdapter {

    private List<Contact> items = new ArrayList<Contact>();

    public static final int ALL_CONTACTS = 0;
    public static final int ONLINE_CONTACTS = 1;
    public static final int OPEN_CHATS = 2;
    private int type;
    private final LayoutInflater inf;
    private VirtualContactList vcl;

    public ContactsAdapter(LayoutInflater inf, VirtualContactList vcl, int type) {
        this.inf = inf;
        this.vcl= vcl;
        this.type = type;
    }

    @Override
    public int getCount() {
        if (type == OPEN_CHATS) {
            return ChatHistory.instance.historyTable.size();
        }
        return items.size();
    }

    public void setItems(Contact items) {
        this.items.add(items);
    }

    public void clear() {
        items.clear();
    }

    @Override
    public Contact getItem(int i) {
        if (type == OPEN_CHATS) {
            return ChatHistory.instance.chatAt(i).getContact();
        }
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (type == ONLINE_CONTACTS) {
            Contact c = items.get(i);
            ViewHolderRoster holder;
            if (convertView == null) {
                convertView = inf.inflate(R.layout.roster_item, null);
                holder = new ViewHolderRoster(vcl, convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolderRoster) convertView.getTag();
            }
            holder.populateFromContact(c);
            return convertView;
        } else if (type == OPEN_CHATS) {
            ItemWrapper wr;
            if (convertView == null) {
                convertView = inf.inflate(R.layout.roster_chats, null);
                wr = new ItemWrapper(convertView);
                convertView.setTag(wr);
            } else {
                wr = (ItemWrapper) convertView.getTag();
            }
            wr.populateFrom(ChatHistory.instance.contactAt(i));
            return convertView;
        }
        return null;
    }

    public static class ItemWrapper {
        View item = null;
        private TextView itemName = null;
        private ImageView itemImage = null;

        public ItemWrapper(View item) {
            this.item = item;
        }

        void populateFrom(Contact item) {
            TextView itemName = getItemName();
            itemName.setText(item.getName());
            itemName.setTextColor(General.getColor(Scheme.THEME_CONTACT_WITH_CHAT));
            Icon icMess = Message.msgIcons.iconAt(item.getUnreadMessageIcon());
            if (icMess == null) {
                getItemImage().setImageBitmap(General.iconToBitmap(item.getProtocol().getStatusInfo().getIcon(item.getStatusIndex())));
            } else {
                getItemImage().setImageBitmap(General.iconToBitmap(icMess));
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
