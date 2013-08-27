package ru.sawim.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.form.VirtualListItem;
import ru.sawim.Scheme;
import ru.sawim.models.list.VirtualList;
import ru.sawim.R;
import ru.sawim.models.VirtualListAdapter;
import ru.sawim.models.list.VirtualListModel;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.06.13
 * Time: 13:22
 * To change this template use File | Settings | File Templates.
 */
public class VirtualListView extends SawimFragment implements VirtualList.OnVirtualListListener, VirtualListModel.OnAddListListener {

    public static final String TAG = "VirtualListView";
    private VirtualListAdapter adapter;
    private VirtualList list = VirtualList.getInstance();
    private ListView lv;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        list.getModel().setAddListListener(this);
        list.setVirtualListListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        list.setVirtualListListener(null);
        list.getModel().setAddListListener(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.virtual_list, container, false);
        v.findViewById(R.id.layout).setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity currentActivity = getActivity();
        currentActivity.setTitle(list.getCaption());
        adapter = new VirtualListAdapter(currentActivity, list.getModel().elements);
        lv = (ListView) currentActivity.findViewById(R.id.list_view);
        lv.setCacheColorHint(0x00000000);
        lv.setAdapter(adapter);
        lv.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        lv.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (list.getClickListListener() != null)
                    list.getClickListListener().itemSelected(position);
            }
        });
        currentActivity.registerForContextMenu(lv);
        lv.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
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
            list.getBuildContextMenu().onContextItemSelected(menuInfo.position, item.getItemId());
        return super.onContextItemSelected(item);
    }

    @Override
    public void addList(final VirtualListItem item) {
        SawimActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                list.getModel().elements.add(item);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void clearList() {
        SawimActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                list.getModel().elements.clear();
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void removeFirstText() {
        SawimActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (list.getModel().elements.size() > 0)
                    list.getModel().elements.remove(0);
                adapter.notifyDataSetChanged();
            }
        });
    }

    public static void show() {
        if (SawimActivity.getInstance().getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            SawimActivity.getInstance().setContentView(R.layout.intercalation_layout);
        VirtualListView newFragment = new VirtualListView();
        FragmentTransaction transaction = SawimActivity.getInstance().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment, VirtualListView.TAG);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void update() {
        SawimActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void back() {
        if (SawimActivity.getInstance().getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            SawimActivity.getInstance().recreateActivity();
        else
            getFragmentManager().popBackStack();
    }

    public boolean hasBack() {
        if (list.getClickListListener() == null) return true;
        return list.getClickListListener().back();
    }

    public void onCreateOptionsMenu(Menu menu) {
        if (list.getBuildOptionsMenu() != null)
            list.getBuildOptionsMenu().onCreateOptionsMenu(menu);
    }

    public void onOptionsItemSelect(MenuItem item) {
        if (list.getBuildOptionsMenu() != null)
            list.getBuildOptionsMenu().onOptionsItemSelected(item);
    }

    @Override
    public int getCurrItem() {
        return lv.getFirstVisiblePosition();
    }

    @Override
    public void setCurrentItemIndex(final int i, final boolean isSelected) {
        SawimActivity.getInstance().runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 if (isSelected)
                    adapter.setSelectedItem(i);
                 lv.setSelection(i);
             }
         });
    }
}