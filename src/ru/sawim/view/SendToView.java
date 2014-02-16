package ru.sawim.view;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.SawimApplication;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.models.RosterAdapter;
import ru.sawim.widget.IconTabPageIndicator;
import sawim.ExternalApi;
import sawim.FileTransfer;
import sawim.chat.ChatHistory;
import sawim.modules.DebugLog;
import sawim.roster.RosterHelper;
import sawim.roster.TreeNode;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 26.08.13
 * Time: 18:58
 * To change this template use File | Settings | File Templates.
 */
public class SendToView extends Fragment implements AdapterView.OnItemClickListener {

    public static final String TAG = "SendToView";
    private RosterAdapter allRosterAdapter;
    private RosterHelper roster;
    private IconTabPageIndicator horizontalScrollView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        roster = RosterHelper.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        horizontalScrollView = new IconTabPageIndicator(getActivity());
        ListView allListView = new ListView(getActivity());
        allListView.setCacheColorHint(0x00000000);
        allListView.setScrollingCacheEnabled(false);
        allListView.setAnimationCacheEnabled(false);
        allListView.setDivider(null);
        allListView.setDividerHeight(0);
        allListView.setOnItemClickListener(this);

        allRosterAdapter = new RosterAdapter(getActivity());
        allRosterAdapter.setType(RosterHelper.ALL_CONTACTS);
        allListView.setAdapter(allRosterAdapter);

        if (!Scheme.isSystemBackground())
            allListView.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        return allListView;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        TreeNode item = (TreeNode) allRosterAdapter.getItem(position);
        if (item.isContact()) {
            Protocol p = roster.getCurrentProtocol();
            Contact c = ((Contact) item);
            Intent intent = getActivity().getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            String type = intent.getType();
            if (type.equals("text/plain")) {
                String sharingText = intent.getStringExtra(Intent.EXTRA_TEXT);
                ChatView newFragment = new ChatView();
                c.activate(p);
                newFragment.initChat(p, c);
                if (sharingText != null)
                    newFragment.setSharingText(sharingText);
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, newFragment, ChatView.TAG);
                transaction.addToBackStack(null);
                transaction.commit();
            } else {
                Uri data = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (data == null) return;
                try {
                    InputStream is = getActivity().getContentResolver().openInputStream(data);
                    FileTransfer fileTransfer = new FileTransfer(p, c);
                    fileTransfer.setFinish(true);
                    fileTransfer.onFileSelect(is, ExternalApi.getFileName(data, getActivity()));
                } catch (FileNotFoundException e) {
                    DebugLog.panic("onFileSelect", e);
                }
            }
        } else if (item.isGroup()) {
            Group group = (Group) item;
            group.setExpandFlag(!group.isExpanded());
        }
        update();
    }

    @Override
    public void onResume() {
        super.onResume();
        initBar();
        update();
    }

    private void initBar() {
        boolean isShowTabs = roster.getProtocolCount() > 1;
        SawimApplication.getActionBar().setDisplayShowTitleEnabled(!isShowTabs);
        SawimApplication.getActionBar().setDisplayShowHomeEnabled(!isShowTabs);
        SawimApplication.getActionBar().setDisplayUseLogoEnabled(!isShowTabs);
        SawimApplication.getActionBar().setDisplayHomeAsUpEnabled(!isShowTabs);
        SawimApplication.getActionBar().setDisplayShowCustomEnabled(isShowTabs);
        getActivity().setTitle(R.string.app_name);
        SawimApplication.getActionBar().setCustomView(horizontalScrollView);
    }

    private void updateBarProtocols() {
        final int protocolCount = roster.getProtocolCount();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                horizontalScrollView.removeAllTabs();
                if (protocolCount > 1) {
                    horizontalScrollView.setOnTabSelectedListener(new IconTabPageIndicator.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(int position) {
                            roster.setCurrentItemProtocol(position);
                            update();
                            Toast toast = Toast.makeText(getActivity(), roster.getProtocol(position).getUserId(), Toast.LENGTH_LONG);
                            toast.setDuration(100);
                            toast.show();
                        }
                    });
                    for (int i = 0; i < protocolCount; ++i) {
                        Protocol protocol = roster.getProtocol(i);
                        Drawable icon = protocol.getCurrentStatusIcon().getImage();
                        Drawable messageIcon = ChatHistory.instance.getUnreadMessageIcon(protocol);
                        if (null != messageIcon)
                            icon = messageIcon;
                        horizontalScrollView.addTab(i, icon);
                    }
                    horizontalScrollView.setCurrentItem(roster.getCurrentItemProtocol());
                }
            }
        });

    }

    private void update() {
        if (roster == null) return;
        if (roster.getProtocolCount() == 0) return;
        roster.updateOptions();
        updateBarProtocols();
        if (allRosterAdapter != null)
            allRosterAdapter.refreshList();
    }

}
