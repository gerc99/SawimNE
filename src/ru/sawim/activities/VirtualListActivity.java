package ru.sawim.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import sawim.ui.text.VirtualList;
import ru.sawim.R;
import ru.sawim.view.VirtualListView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.06.13
 * Time: 12:41
 * To change this template use File | Settings | File Templates.
 */
public class VirtualListActivity extends FragmentActivity {

    private static VirtualListActivity instance;

    public static VirtualListActivity getInstance() {
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.virtual_list_fragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        VirtualListView view = (VirtualListView) getSupportFragmentManager().findFragmentById(R.id.virtual_list_fragment);
        view.onCreateOptionsMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        VirtualListView view = (VirtualListView) getSupportFragmentManager().findFragmentById(R.id.virtual_list_fragment);
        view.onOptionsItemSelected(this, item);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        VirtualListView view = (VirtualListView) getSupportFragmentManager().findFragmentById(R.id.virtual_list_fragment);
        if (view.onBackPressed())
            super.onBackPressed();
        VirtualList.getInstance().clearAll();
    }

}
