package ru.sawim.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;

import ru.sawim.General;
import sawim.ui.base.Scheme;
import sawim.ui.text.VirtualList;
import ru.sawim.R;
import ru.sawim.models.VirtualListAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.06.13
 * Time: 13:22
 * To change this template use File | Settings | File Templates.
 */
public class VirtualListView extends Fragment implements VirtualList.OnUpdateList {

    private VirtualListAdapter adapter;
    private ListView lv;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        VirtualList.getInstance().setUpdateFormListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        VirtualList.getInstance().setUpdateFormListener(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.virtual_list, container, false);
        v.findViewById(R.id.layout).setBackgroundColor(General.getColor(Scheme.THEME_BACKGROUND));
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity currentActivity = getActivity();
        final VirtualList model = VirtualList.getInstance();
        currentActivity.setTitle(model.getCaption());
        adapter = new VirtualListAdapter(currentActivity, model.getModel().elements);
        lv = (ListView)currentActivity.findViewById(R.id.list_view);
        lv.setAdapter(adapter);
        lv.setBackgroundColor(General.getColor(Scheme.THEME_BACKGROUND));
        lv.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (model.getClickListListener() != null)
                model.getClickListListener().itemSelected(position);
            }
        });
        currentActivity.registerForContextMenu(lv);
        lv.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        VirtualList model = VirtualList.getInstance();
        if (model.getBuildContextMenu() != null)
            model.getBuildContextMenu().onCreateContextMenu(menu, contextMenuInfo.position);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (menuInfo == null)
            menuInfo = contextMenuInfo;
        VirtualList model = VirtualList.getInstance();
        if (model.getBuildContextMenu() != null)
            model.getBuildContextMenu().onContextItemSelected(menuInfo.position, item.getItemId());
        return super.onContextItemSelected(item);
    }

    @Override
    public void updateForm() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void back() {
        getActivity().finish();
    }
    public boolean onBackPressed() {
        if (VirtualList.getInstance().getClickListListener() == null) return true;
        return VirtualList.getInstance().getClickListListener().back();
    }

    public void onCreateOptionsMenu(Menu menu) {
        if (VirtualList.getInstance().getBuildOptionsMenu() != null)
            VirtualList.getInstance().getBuildOptionsMenu().onCreateOptionsMenu(menu);
    }
    public void onOptionsItemSelected(FragmentActivity activity, MenuItem item) {
        if (VirtualList.getInstance().getBuildOptionsMenu() != null)
            VirtualList.getInstance().getBuildOptionsMenu().onOptionsItemSelected(activity, item);
    }

    @Override
    public int getCurrItem() {
        return lv.getFirstVisiblePosition();
    }

    @Override
    public void setCurrentItemIndex(final int i) {
         getActivity().runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 lv.setSelection(i);
             }
         });
    }
}
