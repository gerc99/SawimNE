package ru.sawim.models;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import DrawControls.tree.VirtualContactList;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import protocol.mrim.Mrim;
import protocol.mrim.MrimPhoneContact;
import sawim.Options;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import ru.sawim.Scheme;
import protocol.*;
import ru.sawim.General;
import ru.sawim.R;
import sawim.modules.tracking.Tracking;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 13.06.13
 * Time: 22:23
 * To change this template use File | Settings | File Templates.
 */
public class ViewHolderRoster {

    View item = null;
    private TextView itemName = null;
    private TextView itemDescriptionText = null;
    private ImageView itemFirstImage = null;
    private ImageView itemSecondImage = null;
    private ImageView itemThirdImage;
    private ImageView itemFourthImage;
    private ImageView itemFifthImage;
    private VirtualContactList vcl;

    public ViewHolderRoster(VirtualContactList vcl, View item) {
        this.vcl= vcl;
        this.item = item;
    }

    void populateFromGroup(Group g) {
        boolean isExpanded = g.isExpanded();
        TextView itemName = getItemName();
        itemName.setText(g.getText());
        itemName.setTextColor(General.getColor(Scheme.THEME_GROUP));
        itemName.setTypeface(Typeface.DEFAULT);

        ImageView firstImage = getItemFirstImage();
        firstImage.setVisibility(ImageView.VISIBLE);
        ImageList groupIcons = ImageList.createImageList("/gricons.png");
        firstImage.setImageBitmap((isExpanded) ? groupIcons.iconAt(1).getImage() : groupIcons.iconAt(0).getImage());

        getItemDescriptionText().setVisibility(TextView.GONE);
        getItemThirdImage().setVisibility(ImageView.GONE);
        getItemSecondImage().setVisibility(ImageView.GONE);

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
        if (item.subcontactsS() == 0)
            itemName.setText(item.getText());
        else
            itemName.setText(item.getText() + " (" + item.subcontactsS() + ")");
        itemName.setTextColor(General.getColor(item.getTextTheme()));
        itemName.setTypeface(item.hasChat() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);

        TextView itemStausText = getItemDescriptionText();
        if (Options.getBoolean(Options.OPTION_SHOW_STATUS_LINE)) {
            itemStausText.setVisibility(TextView.VISIBLE);
            itemStausText.setText(vcl.getStatusMessage(item));
            itemStausText.setTextColor(General.getColor(Scheme.THEME_CONTACT_STATUS));
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

        ClientInfo info = (null != p) ? p.clientInfo : null;
        Icon icClient = (null != info) ? info.getIcon(item.clientIndex) : null;
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
        if (itemName == null) {
            itemName = (TextView) item.findViewById(R.id.item_name);
        }
        return itemName;
    }

    public TextView getItemDescriptionText() {
        if (itemDescriptionText == null) {
            itemDescriptionText = (TextView) item.findViewById(R.id.item_description);
        }
        return itemDescriptionText;
    }

    public ImageView getItemFirstImage() {
        if (itemFirstImage == null) {
            itemFirstImage = (ImageView) item.findViewById(R.id.first_image);
        }
        return itemFirstImage;
    }

    public ImageView getItemSecondImage() {
        if (itemSecondImage == null) {
            itemSecondImage = (ImageView) item.findViewById(R.id.second_image);
        }
        return itemSecondImage;
    }

    public ImageView getItemThirdImage() {
        if (itemThirdImage == null) {
            itemThirdImage = (ImageView) item.findViewById(R.id.third_image);
        }
        return itemThirdImage;
    }

    public ImageView getItemFourthRuleImage() {
        if (itemFourthImage == null) {
            itemFourthImage = (ImageView) item.findViewById(R.id.fourth_rule_image);
        }
        return itemFourthImage;
    }

    public ImageView getItemFifthRuleImage() {
        if (itemFifthImage == null) {
            itemFifthImage = (ImageView) item.findViewById(R.id.fifth_rule_image);
        }
        return itemFifthImage;
    }
}