package ru.sawim.view.widgets;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import sawim.Options;
import ru.sawim.Scheme;
import protocol.jabber.Jabber;
import protocol.jabber.JabberContact;
import protocol.jabber.JabberServiceContact;
import ru.sawim.General;
import ru.sawim.R;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 20.05.13
 * Time: 22:24
 * To change this template use File | Settings | File Templates.
 */
public class MucUserViewFactory {

    private Context baseContext;
    private Jabber protocol;
    private static ImageList affiliationIcons = ImageList.createImageList("/jabber-affiliations.png");

    public MucUserViewFactory(Context context, Jabber p) {
        baseContext = context;
        protocol = p;
    }

    public View getView(View convView, JabberContact.SubContact contact) {
        ItemWrapper wr;
        if (convView == null) {
            LayoutInflater inf = LayoutInflater.from(baseContext);
            convView = inf.inflate(R.layout.muc_users_item, null);
            wr = new ItemWrapper(convView);
            convView.setTag(wr);
        } else {
            wr = (ItemWrapper) convView.getTag();
        }
        wr.populateFrom(contact);
        return convView;
    }

    public class ItemWrapper {
        View item = null;
        private TextView itemName = null;
        private ImageView itemStatusImage = null;
        private ImageView itemAffilationImage = null;
        private ImageView itemClientImage = null;

        public ItemWrapper(View item) {
            this.item = item;
        }

        void populateFrom(Object o) {
            JabberContact.SubContact c = (JabberContact.SubContact) o;
            TextView itemName = getItemName();
            itemName.setText(c.resource);
            itemName.setTextColor(General.getColor(Scheme.THEME_TEXT));
            getItemStatusImage().setImageBitmap(protocol.getStatusInfo().getIcon(c.status).getImage());
            getItemAffilationImage().setImageBitmap(affiliationIcons.iconAt(JabberServiceContact.getAffiliationName(c.priorityA)).getImage());
            Icon ic = protocol.clientInfo.getIcon(c.client);
            ImageView itemClientImage = getItemClientImage();
            if (ic != null && !Options.getBoolean(Options.OPTION_HIDE_ICONS_CLIENTS)) {
                itemClientImage.setVisibility(ImageView.VISIBLE);
                itemClientImage.setImageBitmap(ic.getImage());
            } else {
                itemClientImage.setVisibility(ImageView.GONE);
            }
        }

        public ImageView getItemStatusImage() {
            if (itemStatusImage == null) {
                itemStatusImage = (ImageView) item.findViewById(R.id.first_image);
            }
            return itemStatusImage;
        }

        public ImageView getItemAffilationImage() {
            if (itemAffilationImage == null) {
                itemAffilationImage = (ImageView) item.findViewById(R.id.affilationImage);
            }
            return itemAffilationImage;
        }

        public TextView getItemName() {
            if (itemName == null) {
                itemName = (TextView) item.findViewById(R.id.item_name);
            }
            return itemName;
        }

        public ImageView getItemClientImage() {
            if (itemClientImage == null) {
                itemClientImage = (ImageView) item.findViewById(R.id.fourth_rule_image);
            }
            return itemClientImage;
        }
    }
}
