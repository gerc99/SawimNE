package ru.sawim.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;

import sawim.ui.text.TextList;
import ru.sawim.R;
import ru.sawim.models.VirtualListAdapter;


/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.06.13
 * Time: 13:22
 * To change this template use File | Settings | File Templates.
 */
public class VirtualListView extends Fragment implements TextList.OnUpdateList {

    private VirtualListAdapter adapter;
    private ListView lv;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        TextList.getInstance().setUpdateFormListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        TextList.getInstance().setUpdateFormListener(null);
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
        final TextList model = TextList.getInstance();
        currentActivity.setTitle(model.getCaption());
        adapter = new VirtualListAdapter(currentActivity, model.getModel().elements);
        lv = (ListView)currentActivity.findViewById(R.id.list_view);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (model.getItemSelectedListener() != null)
                model.getItemSelectedListener().itemSelected(position);
            }
        });
        currentActivity.registerForContextMenu(lv);
        lv.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        TextList model = TextList.getInstance();
        if (model.getBuildContextMenu() != null)
            model.getBuildContextMenu().onCreateContextMenu(menu, contextMenuInfo.position);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (menuInfo == null)
            menuInfo = contextMenuInfo;
        TextList model = TextList.getInstance();
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
        if (TextList.getInstance().getItemSelectedListener() == null) return true;
        return TextList.getInstance().getItemSelectedListener().back();
    }

    public void onCreateOptionsMenu(Menu menu) {
        if (TextList.getInstance().getBuildOptionsMenu() != null)
            TextList.getInstance().getBuildOptionsMenu().onCreateOptionsMenu(menu);
    }
    public void onOptionsItemSelected(FragmentActivity activity, MenuItem item) {
        if (TextList.getInstance().getBuildOptionsMenu() != null)
            TextList.getInstance().getBuildOptionsMenu().onOptionsItemSelected(activity, item);
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
