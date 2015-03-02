package ru.sawim.widget.roster;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import ru.sawim.Scheme;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 15.11.13
 * Time: 21:12
 * To change this template use File | Settings | File Templates.
 */
public class RosterViewRoot extends LinearLayout {

    private static final int PROGRESS_BAR_INDEX = 0;
    private static final int VIEW_PAGER_INDEX = 1;

    public RosterViewRoot(Context context, ProgressBar progressBar, ViewPager rosterView) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        if (!Scheme.isSystemBackground()) {
            setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        }
        addViewInLayout(progressBar, PROGRESS_BAR_INDEX, progressBar.getLayoutParams(), true);
        addViewInLayout(rosterView, VIEW_PAGER_INDEX, rosterView.getLayoutParams(), true);
    }

    public ProgressBar getProgressBar() {
        return (ProgressBar) getChildAt(PROGRESS_BAR_INDEX);
    }

    public ViewPager getViewPager() {
        return (ViewPager) getChildAt(VIEW_PAGER_INDEX);
    }
}
