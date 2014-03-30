package ru.sawim.widget.roster;

import android.content.Context;
import android.widget.LinearLayout;
import ru.sawim.Scheme;
import ru.sawim.widget.MyListView;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 15.11.13
 * Time: 21:12
 * To change this template use File | Settings | File Templates.
 */
public class RosterViewRoot extends LinearLayout {
    public RosterViewRoot(Context context, MyListView rosterListView) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        if (!Scheme.isSystemBackground()) {
            setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        }
        addViewInLayout(rosterListView, 0, rosterListView.getLayoutParams(), true);
    }
}
