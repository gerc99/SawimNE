package ru.sawim.models.list;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.SawimApplication;
import ru.sawim.activities.VirtualListActivity;
import ru.sawim.activities.SawimActivity;

public class VirtualList {
    protected VirtualListModel model;
    private String caption;
    private static VirtualList instance = new VirtualList();
    private OnVirtualListListener virtualListListener;
    private OnBuildOptionsMenu buildOptionsMenu;
    private OnBuildContextMenu buildContextMenu;
    private OnClickListListener itemClickListListener;

    public static VirtualList getInstance() {
        return instance;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return caption;
    }

    public void setModel(VirtualListModel model) {
        this.model = model;
    }

    public VirtualListModel getModel() {
        return model;
    }

    public void clearListeners() {
        virtualListListener = null;
        buildOptionsMenu = null;
        buildContextMenu = null;
        itemClickListListener = null;
    }

    public void clearAll() {
        model.clear();
        clearListeners();
    }

    public void setVirtualListListener(OnVirtualListListener l) {
        virtualListListener = l;
    }
    public interface OnClickListListener {
        void itemSelected(int position);
        boolean back();
    }
    public OnClickListListener getClickListListener() {
        return itemClickListListener;
    }
    public void setClickListListener(OnClickListListener itemClickListListener) {
        this.itemClickListListener = itemClickListListener;
    }

    public interface OnVirtualListListener {
        void update();
        void back();
        int getCurrItem();
        void setCurrentItemIndex(int index);
    }
    public void updateModel() {
        if (virtualListListener != null)
            virtualListListener.update();
    }
    public void back() {
        if (virtualListListener != null)
            virtualListListener.back();
    }
    public int getCurrItem() {
        if (virtualListListener != null)
            return virtualListListener.getCurrItem();
        return 0;
    }
    public void setCurrentItemIndex(int currentItemIndex) {
        if (virtualListListener != null)
            virtualListListener.setCurrentItemIndex(currentItemIndex);
    }

    public interface OnBuildOptionsMenu {
        void onCreateOptionsMenu(Menu menu);
        void onOptionsItemSelected(FragmentActivity activity, MenuItem item);
    }
    public OnBuildOptionsMenu getBuildOptionsMenu() {
        return buildOptionsMenu;
    }
    public void setBuildOptionsMenu(OnBuildOptionsMenu buildOptionsMenu) {
        this.buildOptionsMenu = buildOptionsMenu;
    }

    public interface OnBuildContextMenu {
        void onCreateContextMenu(ContextMenu menu, int listItem);
        void onContextItemSelected(int listItem, int itemMenuId);
    }
    public void setOnBuildContextMenu(OnBuildContextMenu l) {
        buildContextMenu = l;
    }
    public OnBuildContextMenu getBuildContextMenu() {
        return buildContextMenu;
    }

    public void show() {
        SawimActivity.getInstance().startActivity(new Intent(SawimActivity.getInstance(), VirtualListActivity.class));
    }

    public void showCrashLog() {
        SawimApplication.getInstance().startActivity(new Intent(SawimApplication.getInstance(), VirtualListActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}