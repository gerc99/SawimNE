package ru.sawim.models;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import protocol.Protocol;
import protocol.XStatusInfo;
import ru.sawim.General;
import ru.sawim.R;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 19.05.13
 * Time: 15:44
 * To change this template use File | Settings | File Templates.
 */
public class XStatusesAdapter extends BaseAdapter {

    private Context baseContext;
    private XStatusInfo statusInfo;
    private int selectedItem;

    public XStatusesAdapter(Context context, Protocol p) {
        baseContext = context;
        statusInfo = p.getXStatusInfo();
    }

    @Override
    public int getCount() {
        return statusInfo.getXStatusCount() + 1;
    }

    @Override
    public Integer getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void setSelectedItem(int position) {
        selectedItem = position;
    }

    @Override
    public View getView(int position, View convView, ViewGroup viewGroup) {
        ItemWrapper wr;
        View row = convView;
        if (row == null) {
            LayoutInflater inf = LayoutInflater.from(baseContext);
            row = inf.inflate(R.layout.xstatus_item, null);
            wr = new ItemWrapper(row);
            row.setTag(wr);
        } else {
            wr = (ItemWrapper) row.getTag();
        }
        int item = getItem(position);
        wr.populateFrom(item);

        LinearLayout activeItem = (LinearLayout) row;
        if (position == selectedItem) {
            activeItem.setBackgroundColor(Color.BLUE);
            int top = (activeItem == null) ? 0 : activeItem.getTop();
            ((ListView) viewGroup).setSelectionFromTop(position, top);
        } else {
            activeItem.setBackgroundColor(Color.WHITE);
        }
        return row;
    }

    public class ItemWrapper {
        View item = null;
        private TextView itemXStatus = null;
        private ImageView itemImage = null;

        public ItemWrapper(View item) {
            this.item = item;
        }

        void populateFrom(int item) {
            --item;
            ImageView imageView = getItemImage();
            Icon ic = statusInfo.getIcon(item);
            getItemXStatus().setText(statusInfo.getName(item));
            if (ic != null) {
                imageView.setVisibility(ImageView.VISIBLE);
                imageView.setImageBitmap(ic.getImage());
            } else {
                imageView.setVisibility(ImageView.GONE);
            }
        }

        public TextView getItemXStatus() {
            if (itemXStatus == null) {
                itemXStatus = (TextView) item.findViewById(R.id.itemXStatus);
            }
            return itemXStatus;
        }

        public ImageView getItemImage() {
            if (itemImage == null) {
                itemImage = (ImageView) item.findViewById(R.id.second_image);
            }
            return itemImage;
        }
    }
}
