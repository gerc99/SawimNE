package ru.sawim.models.list;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import protocol.Protocol;
import ru.sawim.activities.BaseActivity;
import ru.sawim.view.VirtualListView;

public class VirtualList {
    private VirtualListModel model;
    private String caption;
    private static VirtualList instance;
    private OnVirtualListListener virtualListListener;
    private OnBuildOptionsMenu buildOptionsMenu;
    private OnBuildContextMenu buildContextMenu;
    private OnClickListListener itemClickListListener;
    private Protocol protocol;

    public static VirtualList getInstance() {
        if (instance == null) {
            synchronized (VirtualList.class) {
                if (instance == null) {
                    instance = new VirtualList();
                }
            }
        }
        return instance;
    }

    public void show(BaseActivity activity) {
        VirtualListView.show(activity);
        updateModel();
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
        clearListeners();
        if (model != null) {
            model.clear();
            model = null;
        }
        caption = null;
        protocol = null;
    }

    public void setVirtualListListener(OnVirtualListListener l) {
        virtualListListener = l;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public interface OnClickListListener {
        void itemSelected(BaseActivity activity, int position);

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

        void setCurrentItemIndex(int index, boolean isSelected);
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

    public void setCurrentItemIndex(int currentItemIndex, boolean isSelected) {
        if (virtualListListener != null)
            virtualListListener.setCurrentItemIndex(currentItemIndex, isSelected);
    }

    public interface OnBuildOptionsMenu {
        void onCreateOptionsMenu(Menu menu);

        void onOptionsItemSelected(BaseActivity activity, MenuItem item);
    }

    public OnBuildOptionsMenu getBuildOptionsMenu() {
        return buildOptionsMenu;
    }

    public void setBuildOptionsMenu(OnBuildOptionsMenu listener) {
        buildOptionsMenu = listener;
    }

    public interface OnBuildContextMenu {
        void onCreateContextMenu(ContextMenu menu, int listItem);

        void onContextItemSelected(BaseActivity activity, int listItem, int itemMenuId);
    }

    public void setOnBuildContextMenu(OnBuildContextMenu l) {
        buildContextMenu = l;
    }

    public OnBuildContextMenu getBuildContextMenu() {
        return buildContextMenu;
    }
}