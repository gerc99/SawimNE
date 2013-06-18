package sawim.ui.text;


import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.activities.VirtualListActivity;
import ru.sawim.activities.SawimActivity;

public class TextList {
    protected TextListModel model;
    private String caption;
    private static TextList instance = new TextList();
    private OnUpdateList updateFormListener;
    private OnBuildOptionsMenu buildOptionsMenu;
    private OnBuildContextMenu buildContextMenu;
    private ItemSelectedListener itemSelectedListener;

    public static TextList getInstance() {
        return instance;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return caption;
    }

    public void setModel(TextListModel model) {
        this.model = model;
        updateModel();
    }

    public TextListModel getModel() {
        return model;
    }

    public void clearAll() {
        updateFormListener = null;
        buildOptionsMenu = null;
        buildContextMenu = null;
        itemSelectedListener = null;
    }

    public void setUpdateFormListener(OnUpdateList l) {
        updateFormListener = l;
    }

    public interface OnUpdateList {
        void updateForm();
        void back();
        void setCurrentItemIndex(int index);
    }
    public void updateModel() {
        if (updateFormListener != null)
            updateFormListener.updateForm();
    }
    public void back() {
        if (updateFormListener != null)
            updateFormListener.back();
    }
    public void setCurrentItemIndex(int currentItemIndex) {
        if (updateFormListener != null)
            updateFormListener.setCurrentItemIndex(currentItemIndex);
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

    public interface ItemSelectedListener {
        void itemSelected(int position);
        boolean back();
    }
    public ItemSelectedListener getItemSelectedListener() {
        return itemSelectedListener;
    }
    public void setItemSelectedListener(ItemSelectedListener itemSelectedListener) {
        this.itemSelectedListener = itemSelectedListener;
    }

    public void show() {
        SawimActivity.getInstance().startActivity(new Intent(SawimActivity.getInstance(), VirtualListActivity.class));
    }

    public void restore() {
        //updateModel();
    }

    public int getCurrItem() {
        return 0;
    }

}

