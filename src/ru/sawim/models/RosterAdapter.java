package ru.sawim.models;

import DrawControls.icons.Icon;
import DrawControls.tree.TreeNode;
import DrawControls.tree.VirtualContactList;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.XStatusInfo;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.modules.tracking.Tracking;

import java.util.ArrayList;
import java.util.List;

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
    private VirtualContactList vcl;
    private List<TreeNode> items = new ArrayList<TreeNode>();
    private LayoutInflater mInflater;
    private int type;

    public RosterAdapter(LayoutInflater inf, VirtualContactList vcl, List<TreeNode> items, int type) {
        mInflater = inf;
        this.vcl = vcl;
        this.items = items;
        this.type = type;
    }

    public void refreshList(List<TreeNode> newItems) {
        items = newItems;
        notifyDataSetChanged();
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
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (observer != null) {
            super.unregisterDataSetObserver(observer);
        }
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ViewHolderRoster holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.roster_item, null);
            holder = new ViewHolderRoster(vcl, convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolderRoster) convertView.getTag();
        }
        Protocol protocol = vcl.getCurrentProtocol();
        TreeNode o = getItem(i);
        if (o != null)
            if (type == ONLINE_CONTACTS && o.isContact()) {
                holder.populateFromContact(protocol, (Contact) o);
            } else {
                if (o.isGroup()) {
                    holder.populateFromGroup((Group) o);
                } else if (o.isContact()) {
                    holder.populateFromContact(protocol, (Contact) o);
                }
            }
        return convertView;
    }

    static class ViewHolderRoster {
        final View item;
        private final TextView itemName;
        private final TextView itemDescriptionText;
        private final ImageView itemFirstImage;
        private final ImageView itemSecondImage;
        private final ImageView itemThirdImage;
        private final ImageView itemFourthImage;
        private final ImageView itemFifthImage;
        private final VirtualContactList vcl;

        public ViewHolderRoster(VirtualContactList vcl, View item) {
            this.vcl = vcl;
            this.item = item;
            itemName = (TextView) item.findViewById(R.id.item_name);
            itemDescriptionText = (TextView) item.findViewById(R.id.item_description);
            itemFirstImage = (ImageView) item.findViewById(R.id.image);
            itemSecondImage = (ImageView) item.findViewById(R.id.second_image);
            itemThirdImage = (ImageView) item.findViewById(R.id.third_image);
            itemFourthImage = (ImageView) item.findViewById(R.id.fourth_rule_image);
            itemFifthImage = (ImageView) item.findViewById(R.id.fifth_rule_image);
        }

        void populateFromGroup(Group g) {
            itemName.setTextSize(General.getFontSize());
            itemName.setText(g.getText());
            itemName.setTextColor(Scheme.getColor(Scheme.THEME_GROUP));
            itemName.setTypeface(Typeface.DEFAULT);

            Icon icGroup = g.getLeftIcon();
            if (icGroup == null)
                itemFirstImage.setVisibility(ImageView.GONE);
            else {
                itemFirstImage.setVisibility(ImageView.VISIBLE);
                itemFirstImage.setImageBitmap(icGroup.getImage());
            }

            itemDescriptionText.setVisibility(TextView.GONE);
            itemThirdImage.setVisibility(ImageView.GONE);
            itemSecondImage.setVisibility(ImageView.GONE);
            itemFifthImage.setVisibility(ImageView.GONE);

            Icon messIcon = ChatHistory.instance.getUnreadMessageIcon(g.getContacts());
            ImageView messImage = itemFourthImage;
            if (g.isExpanded() || messIcon == null) {
                messImage.setVisibility(ImageView.GONE);
            } else {
                messImage.setVisibility(ImageView.VISIBLE);
                messImage.setImageBitmap(messIcon.getImage());
            }
        }

        void populateFromContact(Protocol p, Contact item) {
            itemName.setTextSize(General.getFontSize());
            if (item.subcontactsS() == 0)
                itemName.setText(item.getText());
            else
                itemName.setText(item.getText() + " (" + item.subcontactsS() + ")");
            itemName.setTextColor(Scheme.getColor(item.getTextTheme()));
            itemName.setTypeface(item.hasChat() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);

            itemDescriptionText.setTextSize(General.getFontSize() - 2);
            if (General.showStatusLine) {
                itemDescriptionText.setVisibility(TextView.VISIBLE);
                itemDescriptionText.setText(vcl.getStatusMessage(item));
                itemDescriptionText.setTextColor(Scheme.getColor(Scheme.THEME_CONTACT_STATUS));
            } else {
                itemDescriptionText.setVisibility(TextView.GONE);
            }

            Icon icStatus = item.getLeftIcon();
            if (icStatus == null) {
                itemFirstImage.setVisibility(ImageView.GONE);
            } else {
                itemFirstImage.setVisibility(ImageView.VISIBLE);
                itemFirstImage.setImageBitmap(icStatus.getImage());
            }
            if (item.isTyping()) {
                itemFirstImage.setImageBitmap(Message.msgIcons.iconAt(Message.ICON_TYPE).getImage());
            } else {
                Icon icMess = Message.msgIcons.iconAt(item.getUnreadMessageIcon());
                if (icMess != null)
                    itemFirstImage.setImageBitmap(icMess.getImage());
            }

            if (item.getXStatusIndex() == XStatusInfo.XSTATUS_NONE) {
                itemSecondImage.setVisibility(ImageView.GONE);
            } else {
                itemSecondImage.setVisibility(ImageView.VISIBLE);
                itemSecondImage.setImageBitmap(p.getXStatusInfo().getIcon(item.getXStatusIndex()).getImage());
            }

            if (!item.isTemp()) {
                Icon icAuth = item.authIcon.iconAt(0);
                itemThirdImage.setVisibility(ImageView.VISIBLE);
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
                        itemThirdImage.setImageBitmap(item.serverListsIcons.iconAt(privacyList).getImage());
                    else
                        itemThirdImage.setVisibility(ImageView.GONE);
                } else {
                    itemThirdImage.setImageBitmap(icAuth.getImage());
                }
            }

            Icon icClient = (null != p.clientInfo) ? p.clientInfo.getIcon(item.clientIndex) : null;
            if (icClient != null && !General.hideIconsClient) {
                itemFourthImage.setVisibility(ImageView.VISIBLE);
                itemFourthImage.setImageBitmap(icClient.getImage());
            } else {
                itemFourthImage.setVisibility(ImageView.GONE);
            }

            String id = item.getUserId();
            if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE) {
                itemFifthImage.setVisibility(ImageView.VISIBLE);
                itemFifthImage.setImageBitmap(Tracking.getTrackIcon(id).getImage());
            } else {
                itemFifthImage.setVisibility(ImageView.GONE);
            }
        }
    }
}