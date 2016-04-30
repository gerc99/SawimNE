package ru.sawim.view;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import protocol.Contact;
import protocol.ContactMenu;
import protocol.Group;
import protocol.Protocol;
import protocol.xmpp.OnIqReceived;
import protocol.xmpp.Presences;
import protocol.xmpp.ServiceDiscovery;
import protocol.xmpp.XmlConstants;
import protocol.xmpp.XmlNode;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppConnection;
import protocol.xmpp.XmppServiceContact;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.models.RosterAdapter;
import ru.sawim.models.SearchConferenceAdapter;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.ProtocolBranch;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeNode;
import ru.sawim.widget.MyListView;

/**
 * Created by gerc on 31.12.2015.
 */
public class SearchConferenceFragment extends SawimFragment
        implements MenuItemCompat.OnActionExpandListener, View.OnClickListener {

    public static final String TAG = SearchConferenceFragment.class.getSimpleName();

    MenuItem searchMenuItem;

    ProgressBar progressBar;
    MyListView listView;
    TextView errorTextView;
    SearchConferenceAdapter adapter;

    public SearchConferenceFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().supportInvalidateOptionsMenu();
        setHasOptionsMenu(true);

        ActionBar actionBar = ((BaseActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_conf, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActionBar actionBar = ((BaseActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(null);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
        }

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        errorTextView = (TextView) view.findViewById(R.id.error_textView);
        errorTextView.setVisibility(View.GONE);
        listView = (MyListView) view.findViewById(R.id.list_view);
        listView.setVisibility(View.GONE);

        adapter = new SearchConferenceAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new MyListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String jid = adapter.getItem(position).desc;
                if (jid.contains("@")) {
                    Xmpp xmpp = ((Xmpp) RosterHelper.getInstance().getProtocol(0));
                    Contact c = xmpp.createTempContact(jid, true);
                    xmpp.addContact(c);
                    Presences.sendPresence(xmpp.getConnection(), (XmppServiceContact) c);
                    Toast.makeText(SawimApplication.getContext(), R.string.added, Toast.LENGTH_SHORT).show();
                }
            }
        });

        search(SawimApplication.DEFAULT_CONFERENCE_SERVER);
    }

    private void search(String server) {
        final XmppConnection connection = ((Xmpp) RosterHelper.getInstance().getProtocol(0)).getConnection();
        if (connection == null) return;
        connection.requestIq(server, XmlConstants.DISCO_ITEMS, new OnIqReceived() {
            @Override
            public void onIqReceived(XmlNode iq) {
                XmlNode iqQuery = iq.childAt(0);
                String from = StringConvertor.notNull(iq.getAttribute(XmlConstants.S_FROM));
                byte iqType = connection.getIqType(iq);
                String xmlns = iqQuery.getXmlns();
                if (XmlConstants.IQ_TYPE_ERROR == iqType) {
                    XmlNode errorNode = iq.getFirstNode(XmlConstants.S_ERROR);
                    iq.removeNode(XmlConstants.S_ERROR);

                    if (null == errorNode) {
                        DebugLog.println("Error without description is stupid");
                    } else {
                        DebugLog.systemPrintln(
                                "[INFO-JABBER] <IQ> error received: " +
                                        "Code=" + errorNode.getAttribute(XmlConstants.S_CODE) + " " +
                                        "Value=" + connection.getError(errorNode));
                    }
                    errorTextView.setVisibility(View.VISIBLE);
                    errorTextView.setText(connection.getError(errorNode));
                    return;
                }
                final List<SearchConferenceAdapter.Item> items = new ArrayList<>();
                while (0 < iqQuery.childrenCount()) {
                    XmlNode xmlNode = iqQuery.popChildNode();
                    String name = xmlNode.getAttribute(XmlNode.S_NAME);
                    String jid = xmlNode.getAttribute(XmlNode.S_JID);
                    SearchConferenceAdapter.Item item = new SearchConferenceAdapter.Item();
                    item.label = name;
                    item.desc = jid;
                    items.add(item);
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                        adapter.addData(items);
                    }
                });
            }
        });
    }

    @Override
    public boolean hasBack() {
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (SawimApplication.isManyPane())
            ((SawimActivity) getActivity()).recreateActivity();
    }

    @Override
    public void onPrepareOptionsMenu_(Menu menu) {
        getActivity().getMenuInflater().inflate(R.menu.menu_search_contact, menu);

        searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setOnQueryTextListener(new OnQueryTextListener(getAdapter()));
        searchView.setQueryHint(JLocale.getString(R.string.find_conference));
        searchView.setIconifiedByDefault(false);

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, this);
    }

    private static class OnQueryTextListener implements SearchView.OnQueryTextListener {

        SearchConferenceAdapter adapter;

        OnQueryTextListener(SearchConferenceAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            String query = newText.toLowerCase();
            boolean isFind = adapter.filterData(query);
            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }
    };

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        // Do something when collapsed
        getAdapter().filterData(null);
        return true; // Return true to collapse action view
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        // Do something when expanded
        return true; // Return true to expand action view
    }


    @Override
    public void onClick(View v) {

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MyListView.RecyclerContextMenuInfo contextMenuInfo = (MyListView.RecyclerContextMenuInfo) menuInfo;

    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        MyListView.RecyclerContextMenuInfo menuInfo = (MyListView.RecyclerContextMenuInfo) item.getMenuInfo();

        return super.onContextItemSelected(item);
    }

    public SearchConferenceAdapter getAdapter() {
        return adapter;
    }
}
