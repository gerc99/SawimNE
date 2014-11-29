package ru.sawim.widget.roster;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import ru.sawim.Scheme;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 15.11.13
 * Time: 21:12
 * To change this template use File | Settings | File Templates.
 */
public class RosterViewRoot extends LinearLayout {
    public RosterViewRoot(Context context, View progressBar, View rosterView) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        if (!Scheme.isSystemBackground()) {
            setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        }
        addViewInLayout(progressBar, 0, progressBar.getLayoutParams(), true);
        addViewInLayout(rosterView, 1, rosterView.getLayoutParams(), true);
    }
}
