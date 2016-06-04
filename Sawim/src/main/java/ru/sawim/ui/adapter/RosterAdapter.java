package ru.sawim.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import protocol.*;
import ru.sawim.*;
import ru.sawim.ui.activity.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.message.Message;
import ru.sawim.comm.JLocale;
import ru.sawim.icons.Icon;
import ru.sawim.icons.AvatarCache;
import ru.sawim.roster.ProtocolBranch;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeNode;
import ru.sawim.ui.widget.MyImageButton;
import ru.sawim.ui.widget.roster.RosterItemView;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 05.05.13
 * Time: 17:03
 * To change this template use File | Settings | File Templates.
 */
public class RosterAdapter extends RecyclerView.Adapter<RosterAdapter.ViewHolder> implements View.OnClickListener {

    private static final int ITEM_PROTOCOL = 0;
    private static final int ITEM_GROUP = 1;
    private static final int ITEM_CONTACT = 2;
    private static final int ITEM_LAYER = 3;
    private int type;
    private List<TreeNode> items = new ArrayList<>();
    private List<Group> updateQueue = new CopyOnWriteArrayList<>();
    private List<Contact> originalContactList = new ArrayList<>();

    public void setType(int type) {
        this.type = type;
    }

    public boolean filterData(String query) {
        items.clear();
        boolean isFound = false;
        if (query == null || query.isEmpty()) {
            originalContactList.clear();
            refreshList();
        } else {
            query = query.toLowerCase();
            if (originalContactList.isEmpty()) {
                Protocol p = RosterHelper.getInstance().getProtocol();
                for (Contact contact : p.getContactItems().values()) {
                    originalContactList.add(contact);
                }
            }
            for (Contact contact : originalContactList) {
                boolean isSearch = contact.getText().toLowerCase().contains(query);
                if (isSearch) {
                    items.add(contact);
                }
            }
            isFound = !items.isEmpty();
            notifyDataSetChanged();
        }
        return isFound;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int itemViewType) {
        View convertView = null;
        Context context = parent.getContext();
        if (type == RosterHelper.ACTIVE_CONTACTS) {
            convertView = new RosterItemView(SawimApplication.getInstance().getBaseContext());
        }
        if (itemViewType == ITEM_PROTOCOL) {
            convertView = new LinearLayout(context);
            RosterItemView rosterItemView = new RosterItemView(context);
            MyImageButton imageButton = new MyImageButton(context);
            ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleInverse);
            LinearLayout.LayoutParams progressLinearLayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            progressLinearLayout.gravity = Gravity.RIGHT;
            progressBar.setLayoutParams(progressLinearLayout);
            progressBar.setMax(100);
            LinearLayout.LayoutParams buttonLinearLayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            buttonLinearLayout.gravity = Gravity.RIGHT;
            imageButton.setLayoutParams(buttonLinearLayout);
            LinearLayout.LayoutParams rosterLinearLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            rosterLinearLayout.gravity = Gravity.LEFT;
            rosterLinearLayout.weight = 1;
            convertView.setBackgroundColor(Scheme.getColor(R.attr.item_selected));
            rosterItemView.setLayoutParams(rosterLinearLayout);
            imageButton.setImageDrawable(SawimResources.MENU_ICON);
            ((ViewGroup) convertView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            ((ViewGroup) convertView).addView(rosterItemView);
            ((ViewGroup) convertView).addView(progressBar);
            ((ViewGroup) convertView).addView(imageButton);
        }
        if (itemViewType == ITEM_GROUP) {
            convertView = new RosterItemView(SawimApplication.getInstance().getBaseContext());
        }
        if (itemViewType == ITEM_CONTACT) {
            convertView = new RosterItemView(SawimApplication.getInstance().getBaseContext());
        }
        return new ViewHolder(convertView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final TreeNode item = getItem(position);
        int itemViewType = getItemViewType(position);
        View convertView = holder.itemView;
        convertView.setTag(position);
        if (type == RosterHelper.ACTIVE_CONTACTS) {
            RosterItemView rosterItemView = (RosterItemView) holder.itemView;
            rosterItemView.setNull();
            if (item != null) {
                if (itemViewType == ITEM_LAYER) {
                    rosterItemView.addLayer(item.getText());
                }
                if (itemViewType == ITEM_CONTACT) {
                    Contact contact = (Contact) item;
                    populateFromContact(rosterItemView, RosterHelper.getInstance(), contact.getProtocol(), contact);
                }
                setShowDivider(rosterItemView, getItemViewType(position + 1) == ITEM_CONTACT);
            }
            rosterItemView.repaint();
        } else {
            if (itemViewType == ITEM_PROTOCOL) {
                RosterItemView rosterItemView = (RosterItemView) ((ViewGroup) convertView).getChildAt(0);
                ProgressBar progressBar = (ProgressBar) ((ViewGroup) convertView).getChildAt(1);
                MyImageButton imageButton = (MyImageButton) ((ViewGroup) convertView).getChildAt(2);
                rosterItemView.setNull();
                if (item != null) {
                    progressBar.setVisibility(((ProtocolBranch) item).getProtocol().getConnectingProgress() != 100 ? View.VISIBLE : View.GONE);
                    imageButton.setTag(item);
                    imageButton.setOnClickListener(this);
                    populateFromProtocol(rosterItemView, (ProtocolBranch) item);
                    setShowDivider(rosterItemView, true);
                }
                rosterItemView.repaint();
            } else if (itemViewType == ITEM_GROUP) {
                RosterItemView rosterItemView = (RosterItemView) convertView;
                rosterItemView.setNull();
                if (item != null) {
                    populateFromGroup(rosterItemView, (Group) item);
                    setShowDivider(rosterItemView, true);
                }
                rosterItemView.repaint();
            } else if (itemViewType == ITEM_CONTACT) {
                RosterItemView rosterItemView = (RosterItemView) convertView;
                rosterItemView.setNull();
                if (item != null) {
                    Contact contact = (Contact) item;
                    populateFromContact(rosterItemView, RosterHelper.getInstance(), contact.getProtocol(), contact);
                    setShowDivider(rosterItemView, true);
                }
                rosterItemView.repaint();
            }
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        View convertView = holder.itemView;
        convertView.setOnClickListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemViewType(int position) {
        TreeNode node = getItem(position);
        if (node != null) {
            if (node.getType() == TreeNode.LAYER)
                return ITEM_LAYER;
            if (node.getType() == TreeNode.PROTOCOL)
                return ITEM_PROTOCOL;
            if (node.getType() == TreeNode.GROUP)
                return ITEM_GROUP;
            if (node.getType() == TreeNode.CONTACT)
                return ITEM_CONTACT;
        }
        return -1;
    }

    public TreeNode getItem(int i) {
        if (items.size() > i && i >= 0)
            return items.get(i);
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean isEnabled(int position) {
        TreeNode node = items.get(position);
        if (node.getType() == TreeNode.LAYER) return false;
        return true;
    }

    public void putIntoQueue(Group g) {
        if (type == RosterHelper.ACTIVE_CONTACTS) return;
        if (-1 == updateQueue.indexOf(g)) {
            updateQueue.add(g);
        }
    }

    public void refreshList() {
        RosterHelper roster = RosterHelper.getInstance();
        items.clear();
        if (type == RosterHelper.ACTIVE_CONTACTS) {
            Protocol p = roster.getProtocol();
            ChatHistory.instance.addLayerToListOfChats(p, items);
            ChatHistory.instance.sort();
        } else {
            buildRoster(roster.getProtocol());
        }
        notifyDataSetChanged();
    }

    private void buildRoster(Protocol p) {
        if (p == null) return;
        if (RosterHelper.getInstance().useGroups) {
            rebuildFlatItemsWG(p, items);
        } else {
            rebuildFlatItemsWOG(p, items);
        }
    }

    public void rebuildFlatItemsWG(Protocol p, List<TreeNode> list) {
        int contactCounter;
        int onlineContactCounter;
        Enumeration<Group> e = p.getGroupItems().elements();
        while (e.hasMoreElements()) {
            Group group = e.nextElement();
            contactCounter = 0;
            onlineContactCounter = 0;
            Group newGroup = copyGroupWithoutContacts(group);
            list.add(newGroup);
            List<Contact> contacts = group.getContacts();
            int contactsSize = contacts.size();
            for (Contact contact : contacts) {
                //if (contact.isVisibleInContactList()) {
                    if (newGroup.isExpanded()) {
                        list.add(contact);
                    }
                    contactCounter++;
                //}
                if (contact.isOnline())
                    ++onlineContactCounter;
            }
            if (0 == contactCounter) {
                list.remove(list.size() - 1);
            }
            group.updateGroupData(contactsSize, onlineContactCounter);
        }

        Group group = p.getNotInListGroup();
        list.add(group);
        List<Contact> contacts = group.getContacts();
        contactCounter = 0;
        onlineContactCounter = 0;
        int contactsSize = contacts.size();
        for (Contact contact : contacts) {
            //if (contact.isVisibleInContactList()) {
                if (group.isExpanded()) {
                    list.add(contact);
                }
                contactCounter++;
            //}
            if (contact.isOnline())
                ++onlineContactCounter;
        }
        if (0 == contactCounter) {
            list.remove(list.size() - 1);
        }
        group.updateGroupData(contactsSize, onlineContactCounter);
        RosterHelper.sort(list, p.getGroupItems());
    }

    public void rebuildFlatItemsWOG(Protocol p, List<TreeNode> list) {
        ConcurrentHashMap<String, Contact> contacts = p.getContactItems();
        for (Contact contact : contacts.values()) {
            //if (contact.isVisibleInContactList()) {
                list.add(contact);
            //}
        }
        RosterHelper.sort(list, null);
    }

    public static Group copyGroupWithoutContacts(Group g) {
        Group newGroup = new Group(g.getText());
        newGroup.setGroupId(g.getGroupId());
        newGroup.setMode(g.getMode());
        newGroup.setExpandFlag(g.isExpanded());
        return newGroup;
    }

    void populateFromProtocol(RosterItemView rosterItemView, ProtocolBranch o) {
        rosterItemView.itemNameColor = Scheme.getColor(R.attr.group);
        rosterItemView.itemNameFont = Typeface.DEFAULT;
        rosterItemView.itemName = o.getText();

        rosterItemView.itemSecondImage = new Icon(o.isExpanded() ?
                SawimResources.groupDownIcon : SawimResources.groupRightIcons).getImage();

        Icon icGroup = o.getProtocol().getCurrentStatusIcon();
        if (icGroup != null)
            rosterItemView.itemThirdImage = icGroup.getImage().getBitmap();

        Profile profile = o.getProtocol().getProfile();
        if (profile != null) {
            if (profile.xstatusIndex != XStatusInfo.XSTATUS_NONE) {
                XStatusInfo xStatusInfo = o.getProtocol().getXStatusInfo();
                if (xStatusInfo != null) {
                    Icon xStatusIcon = xStatusInfo.getIcon(profile.xstatusIndex);
                    if (xStatusIcon != null)
                        rosterItemView.itemFourthImage = xStatusIcon.getImage().getBitmap();
                }
            }
        }

        Drawable messIcon = ChatHistory.instance.getUnreadMessageIcon();
        if (messIcon != null) {
            if (messIcon == SawimResources.PERSONAL_MESSAGE_ICON) {
                messIcon = messIcon.getConstantState().newDrawable();
                messIcon.setColorFilter(Scheme.getColor(R.attr.personal_unread_message), PorterDuff.Mode.MULTIPLY);
            } else {
                messIcon = messIcon.getConstantState().newDrawable();
                messIcon.setColorFilter(Scheme.getColor(R.attr.unread_message), PorterDuff.Mode.MULTIPLY);
            }
        }
        if (!o.isExpanded() && messIcon != null)
            rosterItemView.itemFifthImage = messIcon;
    }

    void populateFromGroup(RosterItemView rosterItemView, Group g) {
        Group group = g;
        g = RosterHelper.getInstance().getGroupWithContacts(g);
        if (g == null) g = group;
        rosterItemView.itemNameColor = Scheme.getColor(R.attr.group);
        rosterItemView.itemNameFont = Typeface.DEFAULT;
        rosterItemView.itemName = g.getText();

        rosterItemView.itemFirstImage = new Icon(g.isExpanded() ?
                SawimResources.groupDownIcon : SawimResources.groupRightIcons).getImage().getBitmap();

        Drawable messIcon = ChatHistory.instance.getUnreadMessageIcon(g.getContacts());
        if (messIcon != null) {
            if (messIcon == SawimResources.PERSONAL_MESSAGE_ICON) {
                messIcon = messIcon.getConstantState().newDrawable();
                messIcon.setColorFilter(Scheme.getColor(R.attr.personal_unread_message), PorterDuff.Mode.MULTIPLY);
            } else {
                messIcon = messIcon.getConstantState().newDrawable();
                messIcon.setColorFilter(Scheme.getColor(R.attr.unread_message), PorterDuff.Mode.MULTIPLY);
            }
        }
        if (!g.isExpanded() && messIcon != null)
            rosterItemView.itemFifthImage = messIcon;
    }

    void populateFromContact(final RosterItemView rosterItemView, RosterHelper roster, Protocol p, Contact item) {
        if (p == null || item == null) return;
        rosterItemView.itemNameColor = Scheme.getColor(item.getTextTheme());
        rosterItemView.itemNameFont = item.hasChat() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        rosterItemView.itemName = (item.subcontactsS() == 0) ?
                    item.getText() : item.getText() + " (" + item.subcontactsS() + ")";

        String statusMessage = roster.getStatusMessage(p, item);
        rosterItemView.itemDescColor = Scheme.getColor(R.attr.contact_status);
        rosterItemView.itemDesc = statusMessage;

        if (Options.getBoolean(JLocale.getString(R.string.pref_users_avatars))) {
             AvatarCache.getInstance().load(item.getUserId(), item.avatarHash, item.getText(), new AvatarCache.OnImageLoadListener() {
                @Override
                public void onLoad(Bitmap avatar) {
                    rosterItemView.itemFirstImage = avatar;
                    rosterItemView.repaint();
                }
            });
            rosterItemView.avatarBorderColor = Contact.getStatusColor(item.getStatusIndex());
        }
        //Icon icStatus = item.getLeftIcon(p);
        //if (icStatus != null)
        //    rosterItemView.itemSecondImage = icStatus.getImage().getBitmap();
        if (item.isTyping()) {
            rosterItemView.itemSecondImage = Message.getIcon(Message.ICON_TYPE);
        } else {
            Drawable icMess = ChatHistory.instance.getUnreadMessageIcon(item);
            if (icMess != null) {
                if (icMess == SawimResources.PERSONAL_MESSAGE_ICON) {
                    icMess = icMess.getConstantState().newDrawable();
                    icMess.setColorFilter(Scheme.getColor(R.attr.personal_unread_message), PorterDuff.Mode.MULTIPLY);
                } else {
                    icMess = icMess.getConstantState().newDrawable();
                    icMess.setColorFilter(Scheme.getColor(R.attr.unread_message), PorterDuff.Mode.MULTIPLY);
                }
                rosterItemView.itemSecondImage = icMess;
            }
        }

        if (item.getXStatusIndex() != XStatusInfo.XSTATUS_NONE) {
            XStatusInfo xStatusInfo = p.getXStatusInfo();
            if (xStatusInfo != null)
                rosterItemView.itemThirdImage = xStatusInfo.getIcon(item.getXStatusIndex()).getImage().getBitmap();
        }

        if (!item.isTemp()) {
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
                    rosterItemView.itemThirdImage = Contact.serverListsIcons.iconAt(privacyList).getImage().getBitmap();
            } else {
                rosterItemView.itemFourthImage = SawimResources.AUTH_ICON.getBitmap();
            }
        }

        Icon icClient = (null != p.clientInfo) ? p.clientInfo.getIcon(item.clientIndex) : null;
        if (icClient != null && !SawimApplication.hideIconsClient)
            rosterItemView.itemSixthImage = icClient.getImage().getBitmap();
    }

    public static Drawable getImageChat(Chat chat, boolean showMess) {
        if (chat.getContact().isTyping()) {
            return Message.getIcon(Message.ICON_TYPE);
        } else {
            Icon icStatus = RosterHelper.getInstance().getProtocol().getStatusInfo().getIcon(chat.getContact().getStatusIndex());
            Drawable icMess = Message.getIcon(chat.getNewMessageIcon());
            if (icMess != null) {
                if (icMess == SawimResources.PERSONAL_MESSAGE_ICON) {
                    icMess = icMess.getConstantState().newDrawable();
                    icMess.setColorFilter(Scheme.getColor(R.attr.personal_unread_message), PorterDuff.Mode.MULTIPLY);
                } else {
                    icMess = icMess.getConstantState().newDrawable();
                    icMess.setColorFilter(Scheme.getColor(R.attr.unread_message), PorterDuff.Mode.MULTIPLY);
                }
            }
            return icMess == null || !showMess ? icStatus.getImage() : icMess;
        }
    }

    void setShowDivider(RosterItemView rosterItemView, boolean value) {
        rosterItemView.isShowDivider = value;
    }


    @Override
    public void onClick(View v) {
        RosterHelper.getInstance().showProtocolMenu((BaseActivity) v.getContext(), ((ProtocolBranch) v.getTag()).getProtocol());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}