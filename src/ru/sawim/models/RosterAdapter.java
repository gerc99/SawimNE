package ru.sawim.models;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import protocol.*;
import ru.sawim.*;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.message.Message;
import ru.sawim.comm.JLocale;
import ru.sawim.icons.Icon;
import ru.sawim.icons.ImageCache;
import ru.sawim.io.FileSystem;
import ru.sawim.roster.ProtocolBranch;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeNode;
import ru.sawim.widget.MyImageButton;
import ru.sawim.widget.roster.RosterItemView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 05.05.13
 * Time: 17:03
 * To change this template use File | Settings | File Templates.
 */
public class RosterAdapter extends BaseAdapter implements View.OnClickListener{

    private static final int ITEM_PROTOCOL = 0;
    private static final int ITEM_GROUP = 1;
    private static final int ITEM_CONTACT = 2;
    private static final int ITEM_LAYER = 3;
    private static final int ITEM_TYPECOUNT = 4;
    private int type;
    private List<TreeNode> items = new ArrayList<>();
    private List<Group> updateQueue = new CopyOnWriteArrayList<>();
    private List<Contact> originalContactList = new ArrayList<>();

    File avatarsFolder;

    {
        avatarsFolder = FileSystem.openDir(FileSystem.AVATARS);
    }

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
                int count = RosterHelper.getInstance().getProtocolCount();
                for (int i = 0; i < count; ++i) {
                    Protocol p = RosterHelper.getInstance().getProtocol(i);
                    List<Contact> allItems = p.getContactItems();
                    for (Contact allItem : allItems) {
                        originalContactList.add(allItem);
                    }
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
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getViewTypeCount() {
        return ITEM_TYPECOUNT;
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

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
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
    public boolean isEnabled(int position) {
        TreeNode node = items.get(position);
        if (node.getType() == TreeNode.LAYER) return false;
        return super.isEnabled(position);
    }

    public void putIntoQueue(Group g) {
        if (type == RosterHelper.ACTIVE_CONTACTS) return;
        if (-1 == updateQueue.indexOf(g)) {
            updateQueue.add(g);
        }
    }

    public void refreshList() {
        RosterHelper roster = RosterHelper.getInstance();
        final int count = roster.getProtocolCount();
        items.clear();
        if (type == RosterHelper.ACTIVE_CONTACTS) {
            for (int i = 0; i < roster.getProtocolCount(); ++i) {
                ChatHistory.instance.addLayerToListOfChats(roster.getProtocol(i), items, i);
            }
        } else {
            if (count > 1) {
                for (int i = 0; i < count; ++i) {
                    Protocol p = roster.getProtocol(i);
                    if (p == null) return;
                    /*while (!updateQueue.isEmpty()) {
                        Group group = (Group) updateQueue.firstElement();
                        updateQueue.removeElementAt(0);
                        synchronized (p.getRosterLockObject()) {
                            roster.updateGroup(group);
                        }
                    }*/
                    ProtocolBranch root = p.getProtocolBranch(i);
                    items.add(root);
                    if (!root.isExpanded()) continue;
                    buildRoster(p);
                }
            } else {
                buildRoster(roster.getProtocol(0));
            }
        }
        notifyDataSetChanged();
    }

    private void buildRoster(Protocol p) {
        if (RosterHelper.getInstance().useGroups) {
            rebuildFlatItemsWG(p, items);
        } else {
            rebuildFlatItemsWOG(p, items);
        }
    }

    public void rebuildFlatItemsWG(Protocol p, List<TreeNode> list) {
        boolean hideOffline = RosterHelper.getInstance().getCurrPage() == RosterHelper.ONLINE_CONTACTS;
        int contactCounter;
        int onlineContactCounter;
        boolean all = !hideOffline;
        List<Group> groups = p.getGroupItems();
        for (Group group : groups) {
            contactCounter = 0;
            onlineContactCounter = 0;
            Group newGroup = copyGroupWithoutContacts(group);
            list.add(newGroup);
            List<Contact> contacts = group.getContacts();
            int contactsSize = contacts.size();
            for (Contact contact : contacts) {
                if (all || contact.isVisibleInContactList()) {
                    if (newGroup.isExpanded()) {
                        list.add(contact);
                    }
                    contactCounter++;
                }
                if (contact.isOnline())
                    ++onlineContactCounter;
            }
            if (hideOffline && (0 == contactCounter)) {
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
            if (all || contact.isVisibleInContactList()) {
                if (group.isExpanded()) {
                    list.add(contact);
                }
                contactCounter++;
            }
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
        boolean hideOffline = RosterHelper.getInstance().getCurrPage() == RosterHelper.ONLINE_CONTACTS;
        boolean all = !hideOffline;
        List<Contact> contacts = p.getContactItems();
        for (Contact contact : contacts) {
            if (all || contact.isVisibleInContactList()) {
                list.add(contact);
            }
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

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (observer != null) {
            super.unregisterDataSetObserver(observer);
        }
    }

    void populateFromProtocol(RosterItemView rosterItemView, ProtocolBranch o) {
        rosterItemView.itemNameColor = Scheme.getColor(Scheme.THEME_GROUP);
        rosterItemView.itemNameFont = Typeface.DEFAULT;
        rosterItemView.itemName = o.getText();

        rosterItemView.itemSecondImage = new Icon(o.isExpanded() ?
                SawimResources.groupDownIcon : SawimResources.groupRightIcons).getImage().getBitmap();

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

        BitmapDrawable messIcon = ChatHistory.instance.getUnreadMessageIcon(o.getProtocol());
        if (!o.isExpanded() && messIcon != null)
            rosterItemView.itemFifthImage = messIcon.getBitmap();
    }

    void populateFromGroup(RosterItemView rosterItemView, Group g) {
        Group group = g;
        g = RosterHelper.getGroupById(RosterHelper.getInstance().getProtocol(g).getGroupItems(), g.getGroupId());
        if (g == null) g = group;
        rosterItemView.itemNameColor = Scheme.getColor(Scheme.THEME_GROUP);
        rosterItemView.itemNameFont = Typeface.DEFAULT;
        rosterItemView.itemName = g.getText();

        rosterItemView.itemFirstImage = new Icon(g.isExpanded() ?
                SawimResources.groupDownIcon : SawimResources.groupRightIcons).getImage().getBitmap();

        BitmapDrawable messIcon = ChatHistory.instance.getUnreadMessageIcon(g.getContacts());
        if (!g.isExpanded() && messIcon != null)
            rosterItemView.itemFifthImage = messIcon.getBitmap();
    }

    void populateFromContact(final RosterItemView rosterItemView, RosterHelper roster, Protocol p, Contact item) {
        if (p == null || item == null) return;
        rosterItemView.itemNameColor = Scheme.getColor(item.getTextTheme());
        rosterItemView.itemNameFont = item.hasChat() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        rosterItemView.itemName = (item.subcontactsS() == 0) ?
                    item.getText() : item.getText() + " (" + item.subcontactsS() + ")";
        if (SawimApplication.showStatusLine) {
            String statusMessage = roster.getStatusMessage(p, item);
            rosterItemView.itemDescColor = Scheme.getColor(Scheme.THEME_CONTACT_STATUS);
            rosterItemView.itemDesc = statusMessage;
        }
        if (Options.getBoolean(JLocale.getString(R.string.pref_users_avatars))) {
            Bitmap avatar = ImageCache.getInstance().get(avatarsFolder,
                    SawimApplication.getExecutor(), item.avatarHash, SawimResources.DEFAULT_AVATAR, new ImageCache.OnImageLoadListener() {
                        @Override
                        public void onLoad() {
                            rosterItemView.post(new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                }
                            });
                        }
                    });
            rosterItemView.itemFirstImage = avatar;
        }
        Icon icStatus = item.getLeftIcon(p);
        if (icStatus != null)
            rosterItemView.itemSecondImage = icStatus.getImage().getBitmap();
        if (item.isTyping()) {
            rosterItemView.itemSecondImage = Message.getIcon(Message.ICON_TYPE).getBitmap();
        } else {
            BitmapDrawable icMess = ChatHistory.instance.getUnreadMessageIcon(item);
            if (icMess != null)
                rosterItemView.itemSecondImage = icMess.getBitmap();
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

    public BitmapDrawable getImageChat(Chat chat, boolean showMess) {
        if (chat.getContact().isTyping()) {
            return Message.getIcon(Message.ICON_TYPE);
        } else {
            Icon icStatus = chat.getProtocol().getStatusInfo().getIcon(chat.getContact().getStatusIndex());
            BitmapDrawable icMess = Message.getIcon(chat.getNewMessageIcon());
            return icMess == null || !showMess ? icStatus.getImage() : icMess;
        }
    }

    void setShowDivider(RosterItemView rosterItemView, boolean value) {
        rosterItemView.isShowDivider = value;
    }

    @Override
    public View getView(int i, View convertView, final ViewGroup viewGroup) {
        final TreeNode item = getItem(i);
        int itemViewType = getItemViewType(i);
        if (type == RosterHelper.ACTIVE_CONTACTS) {
            if (convertView == null) {
                convertView = new RosterItemView(SawimApplication.getInstance().getBaseContext());
            }
            RosterItemView rosterItemView = (RosterItemView) convertView;
            rosterItemView.setNull();
            if (item != null) {
                if (itemViewType == ITEM_LAYER) {
                    rosterItemView.addLayer(item.getText());
                }
                if (itemViewType == ITEM_CONTACT) {
                    Contact contact = (Contact) item;
                    populateFromContact(rosterItemView, RosterHelper.getInstance(), contact.getProtocol(), contact);
                }
                setShowDivider(rosterItemView, getItemViewType(i + 1) == ITEM_CONTACT);
            }
            rosterItemView.repaint();
        } else {
            if (itemViewType == ITEM_PROTOCOL) {
                if (convertView == null) {
                    Context context = viewGroup.getContext();
                    convertView = new LinearLayout(context);
                    RosterItemView rosterItemView = new RosterItemView(context);
                    MyImageButton imageButton = new MyImageButton(context);
                    ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleInverse);
                    LinearLayout.LayoutParams progressLinearLayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    progressLinearLayout.gravity = Gravity.RIGHT;
                    progressLinearLayout.weight = 4;
                    progressBar.setLayoutParams(progressLinearLayout);
                    progressBar.setMax(100);
                    LinearLayout.LayoutParams buttonLinearLayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    buttonLinearLayout.gravity = Gravity.RIGHT;
                    buttonLinearLayout.weight = 4;
                    imageButton.setLayoutParams(buttonLinearLayout);
                    LinearLayout.LayoutParams rosterLinearLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    rosterLinearLayout.gravity = Gravity.LEFT;
                    rosterLinearLayout.weight = 1;
                    convertView.setBackgroundColor(Scheme.getColor(Scheme.THEME_ITEM_SELECTED));
                    rosterItemView.setLayoutParams(rosterLinearLayout);
                    imageButton.setImageDrawable(SawimResources.MENU_ICON);
                    ((ViewGroup) convertView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                    ((ViewGroup) convertView).addView(rosterItemView);
                    ((ViewGroup) convertView).addView(progressBar);
                    ((ViewGroup) convertView).addView(imageButton);
                }
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
                return convertView;
            } else if (itemViewType == ITEM_GROUP) {
                if (convertView == null) {
                    convertView = new RosterItemView(SawimApplication.getInstance().getBaseContext());
                }
                RosterItemView rosterItemView = (RosterItemView) convertView;
                rosterItemView.setNull();
                if (item != null) {
                    populateFromGroup(rosterItemView, (Group) item);
                    setShowDivider(rosterItemView, true);
                }
                rosterItemView.repaint();
            } else if (itemViewType == ITEM_CONTACT) {
                if (convertView == null) {
                    convertView = new RosterItemView(SawimApplication.getInstance().getBaseContext());
                }
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
        return convertView;
    }

    @Override
    public void onClick(View v) {
        RosterHelper.getInstance().showProtocolMenu((BaseActivity) v.getContext(), ((ProtocolBranch) v.getTag()).getProtocol());
    }
}