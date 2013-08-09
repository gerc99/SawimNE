package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.widget.*;
import sawim.roster.TreeNode;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.XStatusInfo;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.roster.Roster;
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
    public static final int ACTIVE_CONTACTS = 2;
    private final Roster roster;
    private List<TreeNode> items = new ArrayList<TreeNode>();
    private final Context mInflater;
    private int type;

    public RosterAdapter(Context inf, Roster vcl, List<TreeNode> items, int type) {
        mInflater = inf;
        this.roster = vcl;
        this.items = items;
        this.type = type;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        if (type == ACTIVE_CONTACTS)
            return ChatHistory.instance.getTotal();
        return items.size();
    }

    @Override
    public TreeNode getItem(int i) {
        if (type == ACTIVE_CONTACTS)
            return ChatHistory.instance.chatAt(i).getContact();
        if ((items.size() > i) && (i >= 0))
            return items.get(i);
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void buildFlatItems(List<TreeNode> items) {
        Protocol p = roster.getCurrentProtocol();
        if (p == null) return;
    //    synchronized (p.getRosterLockObject()) {
            if (roster.useGroups) {
                roster.rebuildFlatItemsWG(p, items);
            } else {
                roster.rebuildFlatItemsWOG(p, items);
            }
            notifyDataSetChanged();
    //    }
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
            convertView = new RosterItemView(mInflater);
            holder = new ViewHolderRoster(roster, (RosterItemView) convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolderRoster) convertView.getTag();
        }
        Protocol protocol = roster.getCurrentProtocol();
        TreeNode o = getItem(i);
        if (o != null)
            if (type != ALL_CONTACTS && o.isContact()) {
                if (type == ACTIVE_CONTACTS)
                    holder.populateFromContact(((Contact) o).getProtocol(), (Contact) o);
                else
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
        private final Roster vcl;

        /*public ViewHolderRoster(Roster vcl, View item) {
            this.vcl = vcl;
            this.item = item;
            itemName = (TextView) item.findViewById(R.id.item_name);
            itemDescriptionText = (TextView) item.findViewById(R.id.item_description);
            itemFirstImage = (ImageView) item.findViewById(R.id.first_image);
            itemSecondImage = (ImageView) item.findViewById(R.id.second_image);
            itemThirdImage = (ImageView) item.findViewById(R.id.third_image);
            itemFourthImage = (ImageView) item.findViewById(R.id.fourth_rule_image);
            itemFifthImage = (ImageView) item.findViewById(R.id.fifth_rule_image);
        }*/
        public ViewHolderRoster(Roster vcl, RosterItemView item) {
            this.vcl = vcl;
            this.item = item;
            itemName = item.itemName;
            itemDescriptionText = item.itemDescriptionText;
            itemFirstImage = item.itemFirstImage;
            itemSecondImage = item.itemSecondImage;
            itemThirdImage = item.itemThirdImage;
            itemFourthImage = item.itemFourthImage;
            itemFifthImage = item.itemFifthImage;
        }

        void populateFromGroup(Group g) {
            itemName.setTextSize(General.getFontSize());
            itemName.setText(g.getText());
            itemName.setTextColor(Scheme.getColor(Scheme.THEME_GROUP));
            itemName.setTypeface(Typeface.DEFAULT);

            Icon icGroup = g.getLeftIcon(null);
            if (icGroup == null)
                itemFirstImage.setVisibility(ImageView.GONE);
            else {
                itemFirstImage.setVisibility(ImageView.VISIBLE);
                itemFirstImage.setImageDrawable(icGroup.getImage());
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
                messImage.setImageDrawable(messIcon.getImage());
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

            Icon icStatus = item.getLeftIcon(p);
            if (icStatus == null) {
                itemFirstImage.setVisibility(ImageView.GONE);
            } else {
                itemFirstImage.setVisibility(ImageView.VISIBLE);
                itemFirstImage.setImageDrawable(icStatus.getImage());
            }
            if (item.isTyping()) {
                itemFirstImage.setImageDrawable(Message.msgIcons.iconAt(Message.ICON_TYPE).getImage());
            } else {
                Icon icMess = Message.msgIcons.iconAt(item.getUnreadMessageIcon());
                if (icMess != null)
                    itemFirstImage.setImageDrawable(icMess.getImage());
            }

            if (item.getXStatusIndex() == XStatusInfo.XSTATUS_NONE) {
                itemSecondImage.setVisibility(ImageView.GONE);
            } else {
                itemSecondImage.setVisibility(ImageView.VISIBLE);
                itemSecondImage.setImageDrawable(p.getXStatusInfo().getIcon(item.getXStatusIndex()).getImage());
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
                        itemThirdImage.setImageDrawable(item.serverListsIcons.iconAt(privacyList).getImage());
                    else
                        itemThirdImage.setVisibility(ImageView.GONE);
                } else {
                    itemThirdImage.setImageDrawable(icAuth.getImage());
                }
            }

            Icon icClient = (null != p.clientInfo) ? p.clientInfo.getIcon(item.clientIndex) : null;
            if (icClient != null && !General.hideIconsClient) {
                itemFourthImage.setVisibility(ImageView.VISIBLE);
                itemFourthImage.setImageDrawable(icClient.getImage());
            } else {
                itemFourthImage.setVisibility(ImageView.GONE);
            }

            String id = item.getUserId();
            if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE) {
                itemFifthImage.setVisibility(ImageView.VISIBLE);
                itemFifthImage.setImageDrawable(Tracking.getTrackIcon(id).getImage());
            } else {
                itemFifthImage.setVisibility(ImageView.GONE);
            }
        }
    }
}