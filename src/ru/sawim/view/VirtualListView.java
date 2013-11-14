package ru.sawim.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import ru.sawim.General;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.Scheme;
import ru.sawim.models.list.VirtualList;
import ru.sawim.R;
import ru.sawim.models.VirtualListAdapter;
import ru.sawim.widget.MyListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.06.13
 * Time: 13:22
 * To change this template use File | Settings | File Templates.
 */
public class VirtualListView extends SawimFragment implements VirtualList.OnVirtualListListener{

    public static final String TAG = "VirtualListView";
    private VirtualListAdapter adapter;
    private VirtualList list = VirtualList.getInstance();
    private MyListView lv;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;
    public List<VirtualListItem> elements;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        list.setVirtualListListener(this);
        elements = new ArrayList<VirtualListItem>();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        list.setVirtualListListener(null);
        elements.clear();
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
        adapter = new VirtualListAdapter(currentActivity, elements);
        lv = (MyListView) currentActivity.findViewById(R.id.list_view);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                for (int i = 0; i < list.getModel().elements.size(); ++i) {
                    VirtualListItem item = elements.get(i);
                    if (item.getGroupListListener() != null) {
                        item.getGroupListListener().select();

                        return;
                    }
                }
                if (list.getClickListListener() != null)
                    list.getClickListListener().itemSelected(position);
            }
        });
        currentActivity.registerForContextMenu(lv);
        lv.setOnCreateContextMenuListener(this);
        update();
        getActivity().supportInvalidateOptionsMenu();
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

    public static void show() {
        VirtualListView newFragment = new VirtualListView();
        FragmentTransaction transaction = General.currentActivity.getSupportFragmentManager().beginTransaction();
        SawimActivity.resetBar();
        if (General.currentActivity.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null) {
            General.currentActivity.getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment).getView().setVisibility(View.GONE);
            transaction.replace(R.id.roster_fragment, newFragment, VirtualListView.TAG);
        } else {
            transaction.replace(R.id.fragment_container, newFragment, VirtualListView.TAG);
        }
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void update() {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                elements.clear();
                for (int i = 0; i < list.getModel().elements.size(); ++i) {
                    elements.add(list.getModel().elements.get(i));
                }
                adapter.notifyDataSetInvalidated();
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void back() {
        General.currentActivity.getSupportFragmentManager().popBackStack();
        if (General.currentActivity.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            General.currentActivity.getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment).getView().setVisibility(View.VISIBLE);
    }

    public boolean hasBack() {
        if (list.getClickListListener() == null) return true;
        return list.getClickListListener().back();
    }

    public void onPrepareOptionsMenu_(Menu menu) {
        menu.clear();
        if (list.getBuildOptionsMenu() != null)
            list.getBuildOptionsMenu().onCreateOptionsMenu(menu);
        super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected_(MenuItem item) {
        if (list.getBuildOptionsMenu() != null)
            list.getBuildOptionsMenu().onOptionsItemSelected(item);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public int getCurrItem() {
        return lv.getFirstVisiblePosition();
    }

    @Override
    public void setCurrentItemIndex(final int i, final boolean isSelected) {
        General.currentActivity.runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 if (isSelected)
                    adapter.setSelectedItem(i);
                 lv.setSelection(i);
             }
         });
    }
}