package ru.sawim.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.ui.activity.BaseActivity;
import ru.sawim.ui.activity.SawimActivity;
import ru.sawim.ui.adapter.VirtualListAdapter;
import ru.sawim.ui.adapter.list.VirtualList;
import ru.sawim.ui.adapter.list.VirtualListItem;
import ru.sawim.ui.widget.MyListView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.06.13
 * Time: 13:22
 * To change this template use File | Settings | File Templates.
 */
public class VirtualListFragment extends SawimFragment implements VirtualList.OnVirtualListListener {

    public static final String TAG = VirtualListFragment.class.getSimpleName();
    private VirtualList list = VirtualList.getInstance();
    private RecyclerView lv;
    private MyListView.RecyclerContextMenuInfo contextMenuInfo;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        list.setVirtualListListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (getFragmentManager().getBackStackEntryCount() <= 1) {
            list.clearAll();
        }
        unregisterForContextMenu(lv);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.virtual_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity currentActivity = getActivity();
        currentActivity.setTitle(list.getCaption());
        final VirtualListAdapter adapter = new VirtualListAdapter();
        lv = (RecyclerView) currentActivity.findViewById(R.id.list_view);
        lv.setAdapter(adapter);
        adapter.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (int) v.getTag();
                VirtualListItem item = adapter.getItem(position);
                if (item.getGroupListListener() != null) {
                    item.getGroupListListener().select();
                    return;
                }
                if (list.getClickListListener() != null)
                    list.getClickListListener().itemSelected((BaseActivity) getActivity(), position);
            }
        });
        registerForContextMenu(lv);
        update();
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextMenuInfo = (MyListView.RecyclerContextMenuInfo) menuInfo;
        if (list.getBuildContextMenu() != null)
            list.getBuildContextMenu().onCreateContextMenu(menu, contextMenuInfo.position);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        MyListView.RecyclerContextMenuInfo menuInfo = (MyListView.RecyclerContextMenuInfo) item.getMenuInfo();
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
        VirtualListFragment newFragment = new VirtualListFragment();
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment, VirtualListFragment.TAG);
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
        if (getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (list.getModel() == null || getVirtualListAdapter() == null) return;
                getVirtualListAdapter().refreshList(list.getModel().elements);
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
        return ((LinearLayoutManager) lv.getLayoutManager()).findFirstVisibleItemPosition();
    }

    @Override
    public void setCurrentItemIndex(final int i, final boolean isSelected) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isSelected)
                    getVirtualListAdapter().setSelectedItem(i);
                lv.getLayoutManager().scrollToPosition(i);
            }
        });
    }
}