package ru.sawim.view.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.comm.JLocale;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 07.07.13
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */
public class MyMenu extends BaseAdapter {

    private List<MyMenuItem> myMenuItems = new ArrayList<MyMenuItem>();

    public void add(String name, int id) {
        MyMenuItem myMenuItem = new MyMenuItem();
        myMenuItem.addItem(name, id);
        myMenuItems.add(myMenuItem);
    }

    public void add(int name, int id) {
        MyMenuItem myMenuItem = new MyMenuItem();
        myMenuItem.addItem(JLocale.getString(name), id);
        myMenuItems.add(myMenuItem);
    }

    @Override
    public int getCount() {
        return myMenuItems.size();
    }

    @Override
    public MyMenuItem getItem(int i) {
        return myMenuItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View row = convertView;
        ItemWrapper wr;
        if (row == null) {
            LayoutInflater inf = LayoutInflater.from(viewGroup.getContext());
            row = inf.inflate(R.layout.menu_item, null);
            wr = new ItemWrapper(row);
            row.setTag(wr);
        } else {
            wr = (ItemWrapper) row.getTag();
        }
        wr.text = (TextView) row.findViewById(R.id.menuTextView);
        wr.text.setTextSize(SawimApplication.getFontSize());
        wr.text.setText(getItem(i).nameItem);
        return row;
    }

    private class ItemWrapper {
        final View item;
        TextView text;

        public ItemWrapper(View item) {
            this.item = item;
        }
    }
}
