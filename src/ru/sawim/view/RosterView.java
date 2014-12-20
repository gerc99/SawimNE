package ru.sawim.view;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.widget.*;
import com.viewpagerindicator.TabPageIndicator;
import ru.sawim.activities.SawimActivity;
import ru.sawim.listener.OnUpdateRoster;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.*;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.models.CustomPagerAdapter;
import ru.sawim.models.RosterAdapter;
import ru.sawim.modules.FileTransfer;
import ru.sawim.roster.ProtocolBranch;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeBranch;
import ru.sawim.roster.TreeNode;
import ru.sawim.widget.MyImageButton;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.roster.RosterViewRoot;

import java.util.ArrayList;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 01.04.13
 * Time: 19:58
 * To change this template use File | Settings | File Templates.
 */
public class RosterView extends Fragment implements ListView.OnItemClickListener, OnUpdateRoster, Handler.Callback {

    public static final String TAG = RosterView.class.getSimpleName();

    private static final int UPDATE_PROGRESS_BAR = 0;
    private static final int UPDATE_ROSTER = 1;
    private static final int PUT_INTO_QUEUE = 2;

    public static final int MODE_DEFAULT = 0;
    public static final int MODE_SHARE = 1;
    public static final int MODE_SHARE_TEXT = 2;
    private int mode;

    private LinearLayout rosterBarLayout;
    private LinearLayout barLinearLayout;
    private RosterViewRoot rosterViewLayout;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;
    private Handler handler;
    private MyImageButton chatsImage;
    private TabPageIndicator tabPageIndicator;
    private ViewPager viewPager;
    private CustomPagerAdapter pagerAdapter;
    private ProgressBar progressBar;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        handler = new Handler(this);
        rosterBarLayout = new LinearLayout(activity);
        barLinearLayout = new LinearLayout(activity);
        chatsImage = new MyImageButton(activity);

        progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.getProgressDrawable().setBounds(progressBar.getProgressDrawable().getBounds());
        LinearLayout.LayoutParams ProgressBarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ProgressBarLP.setMargins(30, 0, 30, 1);
        progressBar.setLayoutParams(ProgressBarLP);
        progressBar.setVisibility(View.GONE);

        viewPager = new ViewPager(activity);
        tabPageIndicator = new TabPageIndicator(getActivity());
        ViewPager.LayoutParams viewPagerLayoutParams = new ViewPager.LayoutParams();
        viewPagerLayoutParams.height = ViewPager.LayoutParams.WRAP_CONTENT;
        viewPagerLayoutParams.width = ViewPager.LayoutParams.MATCH_PARENT;
        viewPager.setLayoutParams(viewPagerLayoutParams);
        tabPageIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position != RosterHelper.getInstance().getCurrPage()) {
                    getRosterAdapter().setType(position);
                    RosterHelper.getInstance().setCurrPage(position);
                    update();
                    getActivity().supportInvalidateOptionsMenu();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        rosterViewLayout = new RosterViewRoot(activity, progressBar, viewPager);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentActivity activity = getActivity();
        MyListView allListView = new MyListView(activity);
        MyListView onlineListView = new MyListView(activity);
        MyListView activeListView = new MyListView(activity);
        RosterAdapter allRosterAdapter = new RosterAdapter();
        RosterAdapter onlineRosterAdapter = new RosterAdapter();
        RosterAdapter activeRosterAdapter = new RosterAdapter();
        allListView.setAdapter(allRosterAdapter);
        onlineListView.setAdapter(onlineRosterAdapter);
        activeListView.setAdapter(activeRosterAdapter);
        allListView.setTag(activity.getResources().getString(R.string.all_contacts));
        onlineListView.setTag(activity.getResources().getString(R.string.online_contacts));
        activeListView.setTag(activity.getResources().getString(R.string.active_contacts));
        activity.registerForContextMenu(allListView);
        activity.registerForContextMenu(onlineListView);
        activity.registerForContextMenu(activeListView);
        allListView.setOnCreateContextMenuListener(this);
        onlineListView.setOnCreateContextMenuListener(this);
        activeListView.setOnCreateContextMenuListener(this);
        allListView.setOnItemClickListener(this);
        onlineListView.setOnItemClickListener(this);
        activeListView.setOnItemClickListener(this);

        List<View> pages = new ArrayList<View>();
        pages.add(allListView);
        pages.add(onlineListView);
        pages.add(activeListView);
        pagerAdapter = new CustomPagerAdapter(pages);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rosterViewLayout.getParent() != null)
            ((ViewGroup) rosterViewLayout.getParent()).removeView(rosterViewLayout);

        viewPager.setAdapter(pagerAdapter);
        tabPageIndicator.setViewPager(viewPager);
        tabPageIndicator.setCurrentItem(RosterHelper.getInstance().getCurrPage());
        return rosterViewLayout;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        contextMenuInfo = null;
        handler = null;
        viewPager = null;
        pagerAdapter = null;
        tabPageIndicator = null;
        rosterViewLayout = null;
        rosterBarLayout = null;
        barLinearLayout = null;
        chatsImage = null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE_PROGRESS_BAR:
                updateProgressBarSync();
                break;
            case UPDATE_ROSTER:
                updateRosterSync();
                break;
            case PUT_INTO_QUEUE:
                if (getRosterAdapter() != null) {
                    getRosterAdapter().putIntoQueue((Group) msg.obj);
                }
                break;
        }
        return false;
    }

    private int oldProgressBarPercent;
    private void updateProgressBarSync() {
        final Protocol p = RosterHelper.getInstance().getProtocol(0);
        if (RosterHelper.getInstance().getProtocolCount() == 1 && p != null) {
            byte percent = p.getConnectingProgress();
            if (oldProgressBarPercent != percent) {
                oldProgressBarPercent = percent;
                BaseActivity activity = (BaseActivity) getActivity();
                if (100 != percent) {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    progressBar.setProgress(percent);
                } else {
                    progressBar.setVisibility(ProgressBar.GONE);
                }
                if (100 == percent || 0 == percent) {
                    activity.supportInvalidateOptionsMenu();
                }
            }
        }
    }

    private void updateRosterSync() {
        updateChatImage();
        RosterHelper.getInstance().updateOptions();
        if (getRosterAdapter() != null) {
            getRosterAdapter().refreshList();
        }
    }

    @Override
    public void updateProgressBar() {
        if (handler == null) return;
        handler.sendEmptyMessage(UPDATE_PROGRESS_BAR);
    }

    @Override
    public void updateRoster() {
        if (handler == null) return;
        handler.sendEmptyMessage(UPDATE_ROSTER);
    }

    @Override
    public void putIntoQueue(final Group g) {
        if (handler == null) return;
        handler.sendMessage(Message.obtain(handler, PUT_INTO_QUEUE, g));
    }

    public void update() {
        updateRosterSync();
        updateProgressBarSync();
    }

    private void updateChatImage() {
        if (chatsImage == null) return;
        Drawable icMess = ChatHistory.instance.getLastUnreadMessageIcon();
        if (icMess == null) {
            chatsImage.setVisibility(View.GONE);
        } else {
            chatsImage.setVisibility(View.VISIBLE);
            chatsImage.setImageDrawable(icMess);
        }
    }

    private void initBar() {
        ActionBar actionBar = ((BaseActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        chatsImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Chat current = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
                if (current == null) return;
                if (0 < current.getAuthRequestCounter()) {
                    ChatView.showAuthorizationDialog((BaseActivity) getActivity(), current);
                } else {
                    openChat(current.getProtocol(), current.getContact(), null);
                    update();
                }
            }
        });
        Toolbar.LayoutParams barLinearLayoutLP = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams spinnerLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams chatsImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        chatsImageLP.gravity = Gravity.RIGHT;
        chatsImageLP.weight = 4;
        spinnerLP.weight = 1;
        barLinearLayout.removeAllViews();
        if (SawimApplication.isManyPane()) {
            LinearLayout.LayoutParams rosterBarLayoutLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            rosterBarLayoutLP.weight = 2;
            rosterBarLayout.setLayoutParams(rosterBarLayoutLP);
            rosterBarLayout.removeAllViews();
            if (RosterHelper.getInstance().getProtocolCount() > 0)
                rosterBarLayout.addView(tabPageIndicator);
            rosterBarLayout.addView(chatsImage);
            barLinearLayout.addView(rosterBarLayout);
            ChatView chatView = (ChatView) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatView.removeTitleBar();
            barLinearLayout.addView(chatView.getTitleBar());
            actionBar.setCustomView(barLinearLayout);
        } else {
            if (RosterHelper.getInstance().getProtocolCount() > 0)
                barLinearLayout.addView(tabPageIndicator);
            barLinearLayout.addView(chatsImage);
            actionBar.setCustomView(barLinearLayout);
        }
        barLinearLayout.setLayoutParams(barLinearLayoutLP);
        tabPageIndicator.setLayoutParams(spinnerLP);
        chatsImage.setLayoutParams(chatsImageLP);
    }

    @Override
    public void onPause() {
        super.onPause();
        RosterHelper.getInstance().setOnUpdateRoster(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        resume();
        if (Scheme.isChangeTheme(Scheme.getThemeId())) {
            ((SawimActivity) getActivity()).recreateActivity();
        }
    }

    public void resume() {
        initBar();
        getRosterAdapter().setType(RosterHelper.getInstance().getCurrPage());

        if (RosterHelper.getInstance().getProtocolCount() > 0) {
            RosterHelper.getInstance().setOnUpdateRoster(this);
            update();
            getActivity().supportInvalidateOptionsMenu();
            if (SawimApplication.returnFromAcc) {
                SawimApplication.returnFromAcc = false;
                if (getRosterAdapter().getCount() == 0)
                    Toast.makeText(getActivity(), R.string.press_menu_for_connect, Toast.LENGTH_LONG).show();
                if (pagerAdapter != null)
                    pagerAdapter.notifyDataSetChanged();
            }
        } else {
            if (!SawimApplication.isManyPane()) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                StartWindowView newFragment = new StartWindowView();
                transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
                transaction.commit();
                getActivity().supportInvalidateOptionsMenu();
            }
        }
    }

    public RosterAdapter getRosterAdapter() {
        if (viewPager == null) return null;
        return (RosterAdapter) ((MyListView) pagerAdapter.instantiateItem(viewPager, viewPager.getCurrentItem())).getAdapter();
    }

    private void openChat(Protocol p, Contact c, String sharingText) {
        c.activate((BaseActivity) getActivity(), p);
        if (SawimApplication.isManyPane()) {
            ChatView chatViewTablet = (ChatView) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatViewTablet.pause(chatViewTablet.getCurrentChat());
            if (c != null) {
                chatViewTablet.openChat(p, c);
                chatViewTablet.setSharingText(sharingText);
                chatViewTablet.resume(chatViewTablet.getCurrentChat());
            }
        } else {
            ChatView chatView = new ChatView();
            chatView.initChat(p, c);
            chatView.setSharingText(sharingText);
            getFragmentManager().popBackStack();
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, chatView, ChatView.TAG);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void sharing(Protocol p, Contact c) {
        Intent intent = getActivity().getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        String type = intent.getType();
        if (type.equals("text/plain")) {
            String subjectText = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String sharingText = intent.getStringExtra(Intent.EXTRA_TEXT);
            openChat(p, c, subjectText == null ? sharingText : subjectText + "\n" + sharingText);
        } else {
            Uri data = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (data == null) return;
            FileTransfer fileTransfer = new FileTransfer(p, c);
            fileTransfer.onFileSelect((BaseActivity) getActivity(), data);
            Toast.makeText(getActivity(), R.string.sending_file, Toast.LENGTH_LONG).show();
        }
        setMode(MODE_DEFAULT);
        getActivity().setIntent(null);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (getMode() == MODE_SHARE || getMode() == MODE_SHARE_TEXT) {
            if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
                Object o = getRosterAdapter().getItem(position);
                if (o instanceof Chat) {
                    Chat chat = (Chat) o;
                    sharing(chat.getProtocol(), chat.getContact());
                }
            } else {
                TreeNode item = (TreeNode) getRosterAdapter().getItem(position);
                if (item.getType() == TreeNode.CONTACT) {
                    sharing(((Contact) item).getProtocol(), (Contact) item);
                }
            }
        } else {
            if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
                Object o = getRosterAdapter().getItem(position);
                if (o instanceof Chat) {
                    Chat chat = (Chat) o;
                    openChat(chat.getProtocol(), chat.getContact(), null);
                    if (SawimApplication.isManyPane())
                        update();
                }
            } else {
                TreeNode item = (TreeNode) getRosterAdapter().getItem(position);
                if (item.getType() == TreeNode.CONTACT) {
                    openChat(((Contact) item).getProtocol(), ((Contact) item), null);
                    if (SawimApplication.isManyPane())
                        update();
                }
            }
        }
        if (RosterHelper.getInstance().getCurrPage() != RosterHelper.ACTIVE_CONTACTS) {
            TreeNode item = (TreeNode) getRosterAdapter().getItem(position);
            if (item.getType() == TreeNode.PROTOCOL || item.getType() == TreeNode.GROUP) {
                TreeBranch group = (TreeBranch) item;
                group.setExpandFlag(!group.isExpanded());
                update();
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
            Object o = getRosterAdapter().getItem(contextMenuInfo.position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                new ContactMenu(chat.getProtocol(), chat.getContact()).getContextMenu(menu);
            }
        } else {
            TreeNode node = (TreeNode) getRosterAdapter().getItem(contextMenuInfo.position);
            if (node.getType() == TreeNode.PROTOCOL) {
                RosterHelper.getInstance().showProtocolMenu((BaseActivity) getActivity(), ((ProtocolBranch) node).getProtocol());
            } else if (node.getType() == TreeNode.GROUP) {
                Protocol p = RosterHelper.getInstance().getProtocol((Group) node);
                if (p.isConnected()) {
                    new ManageContactListForm(p, (Group) node).showMenu((BaseActivity) getActivity());
                }
            } else if (node.getType() == TreeNode.CONTACT) {
                new ContactMenu(((Contact) node).getProtocol(), (Contact) node).getContextMenu(menu);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (menuInfo == null)
            menuInfo = contextMenuInfo;
        if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
            Object o = getRosterAdapter().getItem(menuInfo.position);
            if (o instanceof Chat) {
                Chat chat = (Chat) o;
                contactMenuItemSelected(chat.getContact(), item);
            }
        } else {
            TreeNode node = (TreeNode) getRosterAdapter().getItem(menuInfo.position);
            if (node == null) return false;
            if (node.getType() == TreeNode.CONTACT) {
                contactMenuItemSelected((Contact) node, item);
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    private void contactMenuItemSelected(final Contact c, final android.view.MenuItem item) {
        new ContactMenu(c.getProtocol(), c).doAction((BaseActivity) getActivity(), item.getItemId());
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}
