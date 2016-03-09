package ru.sawim.models;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.text.OnTextLinkClick;
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
public class VirtualListAdapter extends RecyclerView.Adapter<VirtualListAdapter.ViewHolder> implements View.OnLongClickListener {

    private List<VirtualListItem> items = new ArrayList<>();
    private int selectedItem = -1;
    private View.OnClickListener itemClickListener;

    public VirtualListAdapter() {
        //setHasStableIds(true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.virtual_list_item, null));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        VirtualListItem element = getItem(position);
        LinearLayout activeItem = (LinearLayout) holder.itemView;
        if (element == null) return;
        activeItem.setTag(position);
        activeItem.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        activeItem.setOnLongClickListener(this);
        activeItem.setOnClickListener(itemClickListener);

        holder.labelView.setTextColor(Scheme.getColor(R.attr.text));
        holder.descView.setTextColor(Scheme.getColor(R.attr.text));
        holder.descView.setOnTextLinkClickListener(new OnTextLinkClick());

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
            holder.descView.setLinkTextColor(Scheme.getColor(R.attr.link));
            holder.descView.setTextSize(SawimApplication.getFontSize());
            holder.descView.setText(element.getDescStr());
            holder.descView.repaint();
        }
        if (element.getImage() != null) {
            holder.imageView.setVisibility(ImageView.VISIBLE);
            holder.imageView.setImageDrawable(element.getImage());
            holder.imageView.setAdjustViewBounds(true);
        }
        if (position == selectedItem && selectedItem != -1) {
            activeItem.setBackgroundColor(Scheme.getColor(R.attr.item_selected));
        } else {
            activeItem.setBackgroundColor(0);
        }
    }

    public void refreshList(List<VirtualListItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }


    public VirtualListItem getItem(int i) {
        if (items.isEmpty()) return null;
        return items.get(i);
    }

    public boolean isEnabled(int position) {
        return getItem(position).isItemSelectable();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setSelectedItem(int position) {
        selectedItem = position;
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    public void setOnItemClickListener(View.OnClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout descriptionLayout;
        TextView labelView;
        MyTextView descView;
        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);

            descriptionLayout = (LinearLayout) itemView.findViewById(R.id.descriptionLayout);
            labelView = (TextView) itemView.findViewById(R.id.label);
            descView = (MyTextView) descriptionLayout.findViewById(R.id.description);
            imageView = (ImageView) descriptionLayout.findViewById(R.id.imageView);
        }
    }
}