package ru.sawim.models;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.comm.JLocale;
import ru.sawim.widget.LabelView;
import ru.sawim.widget.Util;

/**
 * Created by admin on 20.04.14.
 */
public class RosterModsAdapter extends BaseAdapter {

    String[] items = {JLocale.getString(R.string.all_contacts), JLocale.getString(R.string.online_contacts), JLocale.getString(R.string.active_contacts)};

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public String getItem(int position) {
        return items[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new LabelView(parent.getContext());
            int paddingH = Util.dipToPixels(convertView.getContext(), 15);
            convertView.setPadding(paddingH, paddingH, paddingH, paddingH);
        }
        LabelView labelView = (LabelView) convertView;
        labelView.setTextColor(Util.isNeedToFixSpinnerAdapter() ? 0xFF000000 : Scheme.getColor(Scheme.THEME_TEXT));
        labelView.setTextSize(SawimApplication.getFontSize());
        labelView.setText(getItem(position));
        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new LabelView(parent.getContext());
            int paddingH = Util.dipToPixels(convertView.getContext(), 10);
            convertView.setPadding(paddingH, 0, paddingH, 0);
        }
        LabelView labelView = (LabelView) convertView;
        labelView.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        labelView.setTextSize(SawimApplication.getFontSize());
        labelView.setText(getItem(position));
        return convertView;
    }
}
