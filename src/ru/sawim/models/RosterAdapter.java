package ru.sawim.models;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
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
import protocol.mrim.Mrim;
import protocol.mrim.MrimPhoneContact;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import sawim.Options;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.modules.tracking.Tracking;

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
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (observer != null) {
            super.unregisterDataSetObserver(observer);
        }
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        TreeNode o = getItem(i);
        ViewHolderRoster holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.roster_item, null);
            holder = new ViewHolderRoster(vcl, convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolderRoster) convertView.getTag();
        }
        if ((type == OPEN_CHATS || type == ONLINE_CONTACTS) && o.isContact()) {
            holder.populateFromContact((Contact) o);
        } else {
            if (o != null)
                if (o.isGroup()) {
                    holder.populateFromGroup((Group) o);
                } else if (o.isContact()) {
                    holder.populateFromContact((Contact) o);
                }
        }
        return convertView;
    }

    static class ViewHolderRoster {

        View item = null;
        private TextView itemName = null;
        private TextView itemDescriptionText = null;
        private ImageView itemFirstImage = null;
        private ImageView itemSecondImage = null;
        private ImageView itemThirdImage;
        private ImageView itemFourthImage;
        private ImageView itemFifthImage;
        private VirtualContactList vcl;
        ImageList groupIcons = ImageList.createImageList("/gricons.png");

        public ViewHolderRoster(VirtualContactList vcl, View item) {
            this.vcl = vcl;
            this.item = item;
        }

        void populateFromGroup(Group g) {
            boolean isExpanded = g.isExpanded();
            TextView itemName = getItemName();
            itemName.setTextSize(General.getFontSize());
            itemName.setText(g.getText());
            itemName.setTextColor(Scheme.getColor(Scheme.THEME_GROUP));
            itemName.setTypeface(Typeface.DEFAULT);

            ImageView firstImage = getItemFirstImage();
            firstImage.setVisibility(ImageView.VISIBLE);
            firstImage.setImageBitmap((isExpanded) ? groupIcons.iconAt(1).getImage() : groupIcons.iconAt(0).getImage());

            getItemDescriptionText().setVisibility(TextView.GONE);
            getItemThirdImage().setVisibility(ImageView.GONE);
            getItemSecondImage().setVisibility(ImageView.GONE);
            getItemFifthRuleImage().setVisibility(ImageView.GONE);

            Icon messIcon = ChatHistory.instance.getUnreadMessageIcon(g.getContacts());
            ImageView messImage = getItemFourthRuleImage();
            if (isExpanded || messIcon == null) {
                messImage.setVisibility(ImageView.GONE);
            } else {
                messImage.setVisibility(ImageView.VISIBLE);
                messImage.setImageBitmap(messIcon.getImage());
            }
        }

        void populateFromContact(Contact item) {
            Protocol p = vcl.getProtocol(vcl.getCurrProtocol());
            TextView itemName = getItemName();
            itemName.setTextSize(General.getFontSize());
            if (item.subcontactsS() == 0)
                itemName.setText(item.getText());
            else
                itemName.setText(item.getText() + " (" + item.subcontactsS() + ")");
            itemName.setTextColor(Scheme.getColor(item.getTextTheme()));
            itemName.setTypeface(item.hasChat() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);

            TextView itemStausText = getItemDescriptionText();
            itemStausText.setTextSize(General.getFontSize() - 2);
            if (Options.getBoolean(Options.OPTION_SHOW_STATUS_LINE)) {
                itemStausText.setVisibility(TextView.VISIBLE);
                itemStausText.setText(vcl.getStatusMessage(item));
                itemStausText.setTextColor(Scheme.getColor(Scheme.THEME_CONTACT_STATUS));
            } else {
                itemStausText.setVisibility(TextView.GONE);
            }

            ImageView statusImage = getItemFirstImage();
            Icon icStatus = p.getStatusInfo().getIcon(item.getStatusIndex());
            if (item instanceof MrimPhoneContact)
                icStatus = Mrim.getPhoneContactIcon();
            if (icStatus == null) {
                statusImage.setVisibility(ImageView.GONE);
            } else {
                statusImage.setVisibility(ImageView.VISIBLE);
                statusImage.setImageBitmap(icStatus.getImage());
            }
            if (item.isTyping()) {
                statusImage.setImageBitmap(Message.msgIcons.iconAt(Message.ICON_TYPE).getImage());
            } else {
                Icon icMess = Message.msgIcons.iconAt(item.getUnreadMessageIcon());
                if (icMess != null)
                    statusImage.setImageBitmap(icMess.getImage());
            }

            ImageView xStatusImage = getItemSecondImage();
            if (item.getXStatusIndex() == XStatusInfo.XSTATUS_NONE) {
                xStatusImage.setVisibility(ImageView.GONE);
            } else {
                xStatusImage.setVisibility(ImageView.VISIBLE);
                xStatusImage.setImageBitmap(p.getXStatusInfo().getIcon(item.getXStatusIndex()).getImage());
            }

            if (!item.isTemp()) {
                Icon icAuth = item.authIcon.iconAt(0);
                ImageView thirdImage = getItemThirdImage();
                thirdImage.setVisibility(ImageView.VISIBLE);
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
                        thirdImage.setImageBitmap(item.serverListsIcons.iconAt(privacyList).getImage());
                    else
                        thirdImage.setVisibility(ImageView.GONE);
                } else {
                    thirdImage.setImageBitmap(icAuth.getImage());
                }
            }

            Icon icClient = (null != p.clientInfo) ? p.clientInfo.getIcon(item.clientIndex) : null;
            ImageView itemClientImage = getItemFourthRuleImage();
            if (icClient != null && !Options.getBoolean(Options.OPTION_HIDE_ICONS_CLIENTS)) {
                itemClientImage.setVisibility(ImageView.VISIBLE);
                itemClientImage.setImageBitmap(icClient.getImage());
            } else {
                itemClientImage.setVisibility(ImageView.GONE);
            }

            ImageView itemFifthRuleImage = getItemFifthRuleImage();
            String id = item.getUserId();
            if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE) {
                itemFifthRuleImage.setVisibility(ImageView.VISIBLE);
                itemFifthRuleImage.setImageBitmap(Tracking.getTrackIcon(id).getImage());
            } else {
                itemFifthRuleImage.setVisibility(ImageView.GONE);
            }
        }

        public TextView getItemName() {
            if (itemName == null)
                itemName = (TextView) item.findViewById(R.id.item_name);
            return itemName;
        }

        public TextView getItemDescriptionText() {
            if (itemDescriptionText == null)
                itemDescriptionText = (TextView) item.findViewById(R.id.item_description);
            return itemDescriptionText;
        }

        public ImageView getItemFirstImage() {
            if (itemFirstImage == null)
                itemFirstImage = (ImageView) item.findViewById(R.id.first_image);
            return itemFirstImage;
        }

        public ImageView getItemSecondImage() {
            if (itemSecondImage == null)
                itemSecondImage = (ImageView) item.findViewById(R.id.second_image);
            return itemSecondImage;
        }

        public ImageView getItemThirdImage() {
            if (itemThirdImage == null)
                itemThirdImage = (ImageView) item.findViewById(R.id.third_image);
            return itemThirdImage;
        }

        public ImageView getItemFourthRuleImage() {
            if (itemFourthImage == null)
                itemFourthImage = (ImageView) item.findViewById(R.id.fourth_rule_image);
            return itemFourthImage;
        }

        public ImageView getItemFifthRuleImage() {
            if (itemFifthImage == null)
                itemFifthImage = (ImageView) item.findViewById(R.id.fifth_rule_image);
            return itemFifthImage;
        }
    }
}