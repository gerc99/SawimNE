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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
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
import ru.sawim.widget.SlidingTabLayout;
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
public class RosterView extends SawimFragment implements ListView.OnItemClickListener, OnUpdateRoster, Handler.Callback {

    public static final String TAG = RosterView.class.getSimpleName();

    private static final int UPDATE_PROGRESS_BAR = 0;
    private static final int UPDATE_ROSTER = 1;
    private static final int PUT_INTO_QUEUE = 2;

    public static final int MODE_DEFAULT = 0;
    public static final int MODE_SHARE = 1;
    public static final int MODE_SHARE_TEXT = 2;
    private int mode;

    private RosterViewRoot rosterViewLayout;
    private AdapterView.AdapterContextMenuInfo contextMenuInfo;
    private Handler handler;
    private MyImageButton chatsImage;
    private SlidingTabLayout slidingTabLayout;
    private EditText queryEditText;
    private boolean isSearchMode;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        handler = new Handler(this);
        chatsImage = new MyImageButton(activity);
        queryEditText = new EditText(activity);
        queryEditText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        queryEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.subSequence(0, s.length()).toString();
                if (isSearchMode) {
                    String query = text.toLowerCase();
                    getRosterAdapter().filterData(query);
                }
            }
        });

        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.getProgressDrawable().setBounds(progressBar.getProgressDrawable().getBounds());
        LinearLayout.LayoutParams ProgressBarLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ProgressBarLP.setMargins(30, 0, 30, 1);
        progressBar.setLayoutParams(ProgressBarLP);
        progressBar.setVisibility(View.GONE);

        ViewPager viewPager = new ViewPager(activity);
        slidingTabLayout = new SlidingTabLayout(getActivity());
        ViewPager.LayoutParams viewPagerLayoutParams = new ViewPager.LayoutParams();
        viewPagerLayoutParams.height = ViewPager.LayoutParams.WRAP_CONTENT;
        viewPagerLayoutParams.width = ViewPager.LayoutParams.MATCH_PARENT;
        viewPager.setLayoutParams(viewPagerLayoutParams);
        slidingTabLayout.setDistributeEvenly(true); // To make the Tabs Fixed set this true, This makes the tabs Space Evenly in Available width
        // Setting Custom Color for the Scroll bar indicator of the Tab View
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.tabsScrollColor);
            }
        });
        slidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

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

        List<View> pages = new ArrayList<>();
        pages.add(allListView);
        pages.add(onlineListView);
        pages.add(activeListView);
        CustomPagerAdapter pagerAdapter = new CustomPagerAdapter(pages);
        rosterViewLayout.getViewPager().setAdapter(pagerAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rosterViewLayout.getParent() != null)
            ((ViewGroup) rosterViewLayout.getParent()).removeView(rosterViewLayout);
        rosterViewLayout.getViewPager().setCurrentItem(RosterHelper.getInstance().getCurrPage());
        slidingTabLayout.setViewPager(rosterViewLayout.getViewPager());

        return rosterViewLayout;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        contextMenuInfo = null;
        handler = null;
        slidingTabLayout = null;
        rosterViewLayout = null;
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

    public void enableSearchMode() {
        isSearchMode = true;
        initBar();
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
                    rosterViewLayout.getProgressBar().setVisibility(ProgressBar.VISIBLE);
                    rosterViewLayout.getProgressBar().setProgress(percent);
                } else {
                    rosterViewLayout.getProgressBar().setVisibility(ProgressBar.GONE);
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
        LinearLayout barLinearLayout = new LinearLayout(getActivity());
        if (SawimApplication.isManyPane()) {
            LinearLayout rosterBarLayout = new LinearLayout(getActivity());
            LinearLayout.LayoutParams rosterBarLayoutLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            rosterBarLayoutLP.weight = 2;
            rosterBarLayout.setLayoutParams(rosterBarLayoutLP);
            rosterBarLayout.removeAllViews();
            if (isSearchMode) {
                barLinearLayout.addView(queryEditText);
            } else {
                if (RosterHelper.getInstance().getProtocolCount() > 0) {
                    if (slidingTabLayout.getParent() != null)
                        ((ViewGroup) slidingTabLayout.getParent()).removeView(slidingTabLayout);
                    rosterBarLayout.addView(slidingTabLayout);
                }
            }
            if (chatsImage.getParent() != null)
                ((ViewGroup) chatsImage.getParent()).removeView(chatsImage);
            rosterBarLayout.addView(chatsImage);
            barLinearLayout.addView(rosterBarLayout);
            ChatView chatView = (ChatView) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatView.removeTitleBar();
            barLinearLayout.addView(chatView.getTitleBar());
            actionBar.setCustomView(barLinearLayout);
        } else {
            if (isSearchMode) {
                if (queryEditText.getParent() != null)
                    ((ViewGroup) queryEditText.getParent()).removeView(queryEditText);
                barLinearLayout.addView(queryEditText);
            } else {
                if (RosterHelper.getInstance().getProtocolCount() > 0) {
                    if (slidingTabLayout.getParent() != null)
                        ((ViewGroup) slidingTabLayout.getParent()).removeView(slidingTabLayout);
                    barLinearLayout.addView(slidingTabLayout);
                }
            }
            if (chatsImage.getParent() != null)
                ((ViewGroup) chatsImage.getParent()).removeView(chatsImage);
            barLinearLayout.addView(chatsImage);
            actionBar.setCustomView(barLinearLayout);
        }
        barLinearLayout.setLayoutParams(barLinearLayoutLP);
        slidingTabLayout.setLayoutParams(spinnerLP);
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
        if (isSearchMode) {
            getRosterAdapter().filterData(queryEditText.getText().toString());
        }
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
                if (getPagerAdapter() != null)
                    getPagerAdapter().notifyDataSetChanged();
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
        if (rosterViewLayout.getViewPager() == null) return null;
        return (RosterAdapter) getListView().getAdapter();
    }

    public MyListView getListView() {
        if (rosterViewLayout.getViewPager() == null) return null;
        return (MyListView) getPagerAdapter().instantiateItem(rosterViewLayout.getViewPager(), rosterViewLayout.getViewPager().getCurrentItem());
    }

    public CustomPagerAdapter getPagerAdapter() {
        return (CustomPagerAdapter) rosterViewLayout.getViewPager().getAdapter();
    }

    private void openChat(Protocol p, Contact c, String sharingText) {
        c.activate((BaseActivity) getActivity(), p);
        if (SawimApplication.isManyPane()) {
            ChatView chatViewTablet = (ChatView) getActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment);
            chatViewTablet.pause(chatViewTablet.getCurrentChat());
            chatViewTablet.openChat(p, c);
            chatViewTablet.setSharingText(sharingText);
            chatViewTablet.resume(chatViewTablet.getCurrentChat());
            update();
        } else {
            ChatView chatView = ChatView.newInstance(p.getUserId(), c.getUserId());
            chatView.setSharingText(sharingText);
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
        TreeNode treeNode = getRosterAdapter().getItem(position);
        if (getMode() == MODE_SHARE || getMode() == MODE_SHARE_TEXT) {
            if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
                if (treeNode.getType() == TreeNode.CONTACT) {
                    Contact contact = (Contact) treeNode;
                    sharing(contact.getProtocol(), contact);
                }
            } else {
                if (treeNode.getType() == TreeNode.CONTACT) {
                    sharing(((Contact) treeNode).getProtocol(), (Contact) treeNode);
                }
            }
        } else {
            if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
                if (treeNode.getType() == TreeNode.CONTACT) {
                    Contact contact = (Contact) treeNode;
                    openChat(contact.getProtocol(), contact, null);
                }
            } else {
                if (treeNode.getType() == TreeNode.CONTACT) {
                    openChat(((Contact) treeNode).getProtocol(), ((Contact) treeNode), null);
                }
            }
        }
        if (RosterHelper.getInstance().getCurrPage() != RosterHelper.ACTIVE_CONTACTS) {
            if (treeNode.getType() == TreeNode.PROTOCOL) {
                ProtocolBranch group = (ProtocolBranch) treeNode;
                RosterHelper roster = RosterHelper.getInstance();
                final int count = roster.getProtocolCount();
                int currProtocol = 0;
                for (int i = 0; i < count; ++i) {
                    Protocol p = roster.getProtocol(i);
                    if (p == null) return;
                    ProtocolBranch root = p.getProtocolBranch(i);
                    if (root.getGroupId() != group.getGroupId()) {
                        root.setExpandFlag(false);
                    } else {
                        currProtocol = i;
                    }
                }
                group.setExpandFlag(!group.isExpanded());
                update();
                getListView().smoothScrollToPosition(currProtocol);
            } else if (treeNode.getType() == TreeNode.GROUP) {
                TreeBranch group = treeNode.getType() == TreeNode.PROTOCOL ?
                        (TreeBranch) treeNode :
                        RosterHelper.getGroupById(RosterHelper.getInstance().getProtocol(treeNode).getGroupItems(), treeNode.getGroupId());
                if (group == null) group = (TreeBranch) treeNode;
                group.setExpandFlag(!group.isExpanded());
                update();
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        TreeNode treeNode = getRosterAdapter().getItem(contextMenuInfo.position);
        if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
            if (treeNode.getType() == TreeNode.CONTACT) {
                Contact contact = (Contact) treeNode;
                new ContactMenu(contact.getProtocol(), contact).getContextMenu(menu);
            }
        } else {
            if (treeNode.getType() == TreeNode.PROTOCOL) {
                RosterHelper.getInstance().showProtocolMenu((BaseActivity) getActivity(), ((ProtocolBranch) treeNode).getProtocol());
            } else if (treeNode.getType() == TreeNode.GROUP) {
                Protocol p = RosterHelper.getInstance().getProtocol((Group) treeNode);
                if (p.isConnected()) {
                    new ManageContactListForm(p, (Group) treeNode).showMenu((BaseActivity) getActivity());
                }
            } else if (treeNode.getType() == TreeNode.CONTACT) {
                new ContactMenu(((Contact) treeNode).getProtocol(), (Contact) treeNode).getContextMenu(menu);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(final android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (menuInfo == null)
            menuInfo = contextMenuInfo;
        TreeNode treeNode = getRosterAdapter().getItem(menuInfo.position);
        if (treeNode == null) return false;
        if (RosterHelper.getInstance().getCurrPage() == RosterHelper.ACTIVE_CONTACTS) {
            if (treeNode.getType() == TreeNode.CONTACT) {
                Contact contact = (Contact) treeNode;
                contactMenuItemSelected(contact, item);
            }
        } else {
            if (treeNode.getType() == TreeNode.CONTACT) {
                contactMenuItemSelected((Contact) treeNode, item);
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

    @Override
    public boolean hasBack() {
        if (isSearchMode) {
            isSearchMode = false;
            getRosterAdapter().filterData(null);
            initBar();
            return false;
        }
        return true;
    }
}
