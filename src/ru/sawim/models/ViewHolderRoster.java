package ru.sawim.models;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import DrawControls.tree.VirtualContactList;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.ui.base.Scheme;
import protocol.*;
import ru.sawim.General;
import ru.sawim.R;

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
    private TextView itemStatus = null;
    private ImageView itemStatusImage = null;
    private ImageView itemXStatusImage = null;
    private ImageView itemAuthImage;
    private ImageView itemClientImage;
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

        ImageView statusImage = getItemFirstImage();
        statusImage.setVisibility(ImageView.VISIBLE);
        ImageList groupIcons = ImageList.createImageList("/gricons.png");
        statusImage.setImageBitmap(General.iconToBitmap((isExpanded) ? groupIcons.iconAt(1) : groupIcons.iconAt(0)));

        getItemDescriptionText().setVisibility(TextView.GONE);
        getItemThirdImage().setVisibility(ImageView.GONE);
        getItemSecondImage().setVisibility(ImageView.GONE);

        Icon messIcon = ChatHistory.instance.getUnreadMessageIcon(g.getContacts());
        ImageView messImage = getItemFourthRuleImage();
        if (isExpanded || messIcon == null) {
            messImage.setVisibility(ImageView.GONE);
        } else {
            messImage.setVisibility(ImageView.VISIBLE);
            messImage.setImageBitmap(General.iconToBitmap((messIcon)));
        }
    }

    void populateFromContact(Contact item) {
        Protocol p = vcl.getModel().getProtocol(vcl.getCurrProtocol());
        TextView itemName = getItemName();
        if (item.subcontactsS() == 0)
            itemName.setText(item.getText());
        else
            itemName.setText(item.getText() + " (" + item.subcontactsS() + ")");
        itemName.setTextColor(General.getColor(item.getTextTheme()));

        TextView itemStausText = getItemDescriptionText();
        itemStausText.setVisibility(TextView.VISIBLE);
        itemStausText.setText(vcl.getStatusMessage(item));
        itemStausText.setTextColor(General.getColor(Scheme.THEME_CONTACT_STATUS));

        ImageView statusImage = getItemFirstImage();
        Icon icStatus = p.getStatusInfo().getIcon(item.getStatusIndex());
        if (icStatus == null) {
            statusImage.setVisibility(ImageView.GONE);
        } else {
            statusImage.setVisibility(ImageView.VISIBLE);
            statusImage.setImageBitmap(General.iconToBitmap(icStatus));
        }
        if (item.isTyping()) {
            statusImage.setImageBitmap(General.iconToBitmap(Message.msgIcons.iconAt(Message.ICON_TYPE)));
        } else {
            Icon icMess = Message.msgIcons.iconAt(item.getUnreadMessageIcon());
            if (icMess != null)
                statusImage.setImageBitmap(General.iconToBitmap(icMess));
        }

        ImageView xStatusImage = getItemSecondImage();
        if (item.getXStatusIndex() == XStatusInfo.XSTATUS_NONE) {
            xStatusImage.setVisibility(ImageView.GONE);
        } else {
            xStatusImage.setVisibility(ImageView.VISIBLE);
            xStatusImage.setImageBitmap(General.iconToBitmap(p.getXStatusInfo().getIcon(item.getXStatusIndex())));
        }

        if (!item.isTemp()) {
            Icon icAuth = item.authIcon.iconAt(0);
            ImageView imageCl = getItemThirdImage();
            if (!item.isAuth()) {
                imageCl.setVisibility(ImageView.VISIBLE);
                imageCl.setImageBitmap(General.iconToBitmap(icAuth));
            } else {
                imageCl.setVisibility(ImageView.GONE);
            }
        }

        ClientInfo info = (null != p) ? p.clientInfo : null;
        Icon icClient = (null != info) ? info.getIcon(item.clientIndex) : null;
        ImageView itemClientImage = getItemFourthRuleImage();
        if (icClient != null) {
            itemClientImage.setVisibility(ImageView.VISIBLE);
            itemClientImage.setImageBitmap(General.iconToBitmap(icClient));
        } else {
            itemClientImage.setVisibility(ImageView.GONE);
        }
    }

    public TextView getItemName() {
        if (itemName == null) {
            itemName = (TextView) item.findViewById(R.id.item_name);
        }
        return itemName;
    }

    public TextView getItemDescriptionText() {
        if (itemStatus == null) {
            itemStatus = (TextView) item.findViewById(R.id.item_description);
        }
        return itemStatus;
    }

    public ImageView getItemFirstImage() {
        if (itemStatusImage == null) {
            itemStatusImage = (ImageView) item.findViewById(R.id.first_image);
        }
        return itemStatusImage;
    }

    public ImageView getItemSecondImage() {
        if (itemXStatusImage == null) {
            itemXStatusImage = (ImageView) item.findViewById(R.id.second_image);
        }
        return itemXStatusImage;
    }

    public ImageView getItemThirdImage() {
        if (itemAuthImage == null) {
            itemAuthImage = (ImageView) item.findViewById(R.id.third_image);
        }
        return itemAuthImage;
    }

    public ImageView getItemFourthRuleImage() {
        if (itemClientImage == null) {
            itemClientImage = (ImageView) item.findViewById(R.id.fourth_rule_image);
        }
        return itemClientImage;
    }
}
