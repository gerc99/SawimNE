package ru.sawim.widget.roster;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import ru.sawim.widget.MyListView;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 15.11.13
 * Time: 21:12
 * To change this template use File | Settings | File Templates.
 */
public class RosterViewRoot extends FrameLayout {

    private static final int PROGRESS_BAR_INDEX = 0;
    private static final int VIEW_PAGER_INDEX = 1;
    private static final int FAB_INDEX = 2;

    public RosterViewRoot(Context context, ProgressBar progressBar, View rosterView, View fab) {
        super(context);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addViewInLayout(progressBar, PROGRESS_BAR_INDEX, progressBar.getLayoutParams(), true);
        addViewInLayout(rosterView, VIEW_PAGER_INDEX, rosterView.getLayoutParams(), true);
        addViewInLayout(fab, FAB_INDEX, fab.getLayoutParams(), true);
    }

    public RosterViewRoot(Context context, View rosterView) {
        super(context);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addViewInLayout(new View(getContext()), PROGRESS_BAR_INDEX, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT), true);
        addViewInLayout(rosterView, VIEW_PAGER_INDEX, rosterView.getLayoutParams(), true);
    }

    public ProgressBar getProgressBar() {
        return (ProgressBar) getChildAt(PROGRESS_BAR_INDEX);
    }

    public MyListView getMyListView() {
        return (MyListView) getChildAt(VIEW_PAGER_INDEX);
    }

    public View getFab() {
        return getChildAt(FAB_INDEX);
    }
}
