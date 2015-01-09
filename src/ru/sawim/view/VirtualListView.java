package ru.sawim.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.VirtualListAdapter;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.widget.MyListView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.06.13
 * Time: 13:22
 * To change this template use File | Settings | File Templates.
 */
public class VirtualListView extends SawimFragment implements VirtualList.OnVirtualListListener {

    public static final String TAG = VirtualListView.class.getSimpleName();
    private VirtualList list = VirtualList.getInstance();
    private MyListView lv;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;

    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        list.setVirtualListListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (getFragmentManager().getBackStackEntryCount() <= 1) {
            list.clearAll();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.virtual_list, container, false);
        if (!Scheme.isSystemBackground())
            v.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity currentActivity = getActivity();
        currentActivity.setTitle(list.getCaption());
        final VirtualListAdapter adapter = new VirtualListAdapter(currentActivity, list);
        lv = (MyListView) currentActivity.findViewById(R.id.list_view);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                for (int i = 0; i < list.getModel().elements.size(); ++i) {
                    VirtualListItem item = adapter.getItem(i);
                    if (item.getGroupListListener() != null) {
                        item.getGroupListListener().select();
                        return;
                    }
                }
                if (list.getClickListListener() != null)
                    list.getClickListListener().itemSelected((BaseActivity) getActivity(), position);
            }
        });
        currentActivity.registerForContextMenu(lv);
        lv.setOnCreateContextMenuListener(this);
        update();
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (list.getBuildContextMenu() != null)
            list.getBuildContextMenu().onCreateContextMenu(menu, contextMenuInfo.position);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (menuInfo == null)
            menuInfo = contextMenuInfo;
        if (list.getBuildContextMenu() != null)
            list.getBuildContextMenu().onContextItemSelected((BaseActivity) getActivity(), menuInfo.position, item.getItemId());
        return super.onContextItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseActivity) getActivity()).resetBar(list.getCaption());
        ((BaseActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public static void show(BaseActivity activity) {
        show(activity, true);
    }

    public static void show(BaseActivity activity, boolean addToBackStack) {
        if (SawimApplication.isManyPane())
            activity.setContentView(R.layout.main);
        VirtualListView newFragment = new VirtualListView();
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment, VirtualListView.TAG);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    private VirtualListAdapter getVirtualListAdapter() {
        return (VirtualListAdapter) lv.getAdapter();
    }

    @Override
    public void update() {
        if (list.getModel() == null || getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getVirtualListAdapter().refreshList(list.getModel().elements);
                getVirtualListAdapter().notifyDataSetInvalidated();
            }
        });
    }

    @Override
    public void back() {
        if (SawimApplication.isManyPane())
            ((SawimActivity) getActivity()).recreateActivity();
        else
            getActivity().getSupportFragmentManager().popBackStack();
        getActivity().supportInvalidateOptionsMenu();
    }

    public boolean hasBack() {
        return list.getClickListListener() == null || list.getClickListListener().back();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (BaseActivity.getExternalApi().onActivityResult(requestCode, resultCode, data)) return;
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onPrepareOptionsMenu_(Menu menu) {
        menu.clear();
        if (list.getBuildOptionsMenu() != null)
            list.getBuildOptionsMenu().onCreateOptionsMenu(menu);
    }

    public void onOptionsItemSelected_(MenuItem item) {
        if (list.getBuildOptionsMenu() != null)
            list.getBuildOptionsMenu().onOptionsItemSelected((BaseActivity) getActivity(), item);
    }

    @Override
    public int getCurrItem() {
        return lv.getFirstVisiblePosition();
    }

    @Override
    public void setCurrentItemIndex(final int i, final boolean isSelected) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isSelected)
                    getVirtualListAdapter().setSelectedItem(i);
                lv.setSelection(i);
            }
        });
    }
}