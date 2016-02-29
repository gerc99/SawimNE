package ru.sawim.models;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.sawim.R;

/**
 * Created by gerc on 28.02.2016.
 */
public class SearchConferenceAdapter extends RecyclerView.Adapter<SearchConferenceAdapter.ViewHolder> {

    List<Item> originalContactList = new ArrayList<>();
    List<Item> items = new ArrayList<>();

    public boolean filterData(String query) {
        items.clear();
        boolean isFound = false;
        if (query == null || query.isEmpty()) {
            items.addAll(originalContactList);
            notifyDataSetChanged();
        } else {
            query = query.toLowerCase();
            for (Item item : originalContactList) {
                boolean isSearch = item.label.toLowerCase().contains(query) || item.desc.toLowerCase().contains(query);
                if (isSearch) {
                    items.add(item);
                }
            }
            isFound = !items.isEmpty();
            notifyDataSetChanged();
        }
        return isFound;
    }

    public void addData(List<Item> newItems) {
        items.clear();
        items.addAll(newItems);
        originalContactList.clear();
        originalContactList.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public SearchConferenceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_conference, parent, false));
    }

    @Override
    public void onBindViewHolder(SearchConferenceAdapter.ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.labelTextView.setText(item.label);
        holder.descTextView.setText(item.desc);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Item getItem(int position) {
        return items.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView labelTextView;
        TextView descTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            labelTextView = (TextView) itemView.findViewById(R.id.label_textView);
            descTextView = (TextView) itemView.findViewById(R.id.desc_textView);
        }
    }

    public static class Item {
        public String label;
        public String desc;
    }
}
