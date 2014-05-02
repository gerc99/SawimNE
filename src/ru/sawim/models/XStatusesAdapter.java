package ru.sawim.models;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import protocol.Protocol;
import protocol.XStatusInfo;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.icons.Icon;

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
            row = inf.inflate(R.layout.status_item, null);
            wr = new ItemWrapper(row);
            row.setTag(wr);
        } else {
            wr = (ItemWrapper) row.getTag();
        }
        int item = getItem(position);
        LinearLayout activeItem = (LinearLayout) row;
        if (item == selectedItem) {
            activeItem.setBackgroundColor(Scheme.getColor(Scheme.THEME_ITEM_SELECTED));
        } else {
            activeItem.setBackgroundColor(0);
        }
        wr.populateFrom(item);
        return row;
    }

    public class ItemWrapper {
        View item = null;
        private TextView itemXStatus = null;
        private ImageView itemImage = null;

        public ItemWrapper(View item) {
            this.item = item;
            itemImage = (ImageView) item.findViewById(R.id.status_image);
            itemXStatus = (TextView) item.findViewById(R.id.status);
        }

        void populateFrom(int item) {
            Icon ic = statusInfo.getIcon(item - 1);
            itemXStatus.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
            itemXStatus.setText(statusInfo.getName(item - 1));
            if (ic != null) {
                itemImage.setVisibility(ImageView.VISIBLE);
                itemImage.setImageDrawable(ic.getImage());
            } else {
                itemImage.setVisibility(ImageView.GONE);
            }
            if (item == selectedItem) {
                itemXStatus.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                itemXStatus.setTypeface(Typeface.DEFAULT);
            }
        }
    }
}
