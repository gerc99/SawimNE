package ru.sawim.models;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.text.TextLinkClick;
import ru.sawim.widget.MyTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.06.13
 * Time: 13:07
 * To change this template use File | Settings | File Templates.
 */
public class VirtualListAdapter extends BaseAdapter {

    private Context baseContext;
    private VirtualList list;
    private LayoutInflater inf;
    private List<VirtualListItem> items = new ArrayList<VirtualListItem>();
    private int selectedItem = -1;

    public VirtualListAdapter(Context context, VirtualList list) {
        this.baseContext = context;
        this.list = list;
        inf = LayoutInflater.from(baseContext);
    }

    public void refreshList(List<VirtualListItem> list) {
        items.clear();
        items.addAll(list);
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
    public VirtualListItem getItem(int i) {
        if (items.size() == 0) return null;
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void setSelectedItem(int position) {
        selectedItem = position;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        VirtualListItem element = getItem(i);
        if (convertView == null) {
            convertView = inf.inflate(R.layout.virtual_list_item, null);
            holder = new ViewHolder();
            holder.descriptionLayout = (LinearLayout) convertView.findViewById(R.id.descriptionLayout);
            holder.labelView = (TextView) convertView.findViewById(R.id.label);
            holder.descView = (MyTextView) holder.descriptionLayout.findViewById(R.id.description);
            holder.imageView = (ImageView) holder.descriptionLayout.findViewById(R.id.imageView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (element == null) return convertView;
        ((ViewGroup) convertView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        holder.labelView.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        holder.descView.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        holder.descView.setOnTextLinkClickListener(new TextLinkClick(null, null));

        holder.labelView.setVisibility(TextView.GONE);
        holder.descView.setVisibility(TextView.GONE);
        holder.imageView.setVisibility(ImageView.GONE);

        ViewGroup.MarginLayoutParams labelLayoutParams = (ViewGroup.MarginLayoutParams) holder.labelView.getLayoutParams();
        ViewGroup.MarginLayoutParams descLayoutParams = (ViewGroup.MarginLayoutParams) holder.descriptionLayout.getLayoutParams();
        if (labelLayoutParams != null) {
            labelLayoutParams.setMargins(element.getMarginLeft(), 0, 0, 0);
            holder.labelView.setLayoutParams(labelLayoutParams);
        }
        if (descLayoutParams != null) {
            descLayoutParams.setMargins(element.getMarginLeft(), 0, 0, 0);
            holder.descriptionLayout.setLayoutParams(descLayoutParams);
        }
        if (element.getLabel() != null) {
            holder.labelView.setVisibility(TextView.VISIBLE);
            if (element.getThemeTextLabel() > -1) {
                holder.labelView.setTextColor(Scheme.getColor(element.getThemeTextLabel()));
            }
            holder.labelView.setTextSize(SawimApplication.getFontSize());
            holder.labelView.setText(element.getLabel());
        }
        if (element.getDescStr() != null) {
            holder.descView.setVisibility(TextView.VISIBLE);
            if (element.getThemeTextDesc() > -1) {
                holder.descView.setTextColor(Scheme.getColor(element.getThemeTextDesc()));
            }
            holder.descView.setLinkTextColor(Scheme.getColor(Scheme.THEME_LINKS));
            holder.descView.setTextSize(SawimApplication.getFontSize());
            holder.descView.setText(element.getDescStr());
            holder.descView.repaint();
        }
        if (element.getImage() != null) {
            holder.imageView.setVisibility(ImageView.VISIBLE);
            holder.imageView.setImageDrawable(element.getImage());
            holder.imageView.setAdjustViewBounds(true);
        }
        LinearLayout activeItem = (LinearLayout) convertView;
        if (i == selectedItem && selectedItem != -1) {
            activeItem.setBackgroundColor(Scheme.getColor(Scheme.THEME_ITEM_SELECTED));
        } else {
            activeItem.setBackgroundColor(0);
        }
        return convertView;
    }

    private class ViewHolder {
        LinearLayout descriptionLayout;
        TextView labelView;
        MyTextView descView;
        ImageView imageView;
    }
}