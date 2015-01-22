package ru.sawim.view;

import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 25.08.13
 * Time: 19:45
 * To change this template use File | Settings | File Templates.
 */
public abstract class SawimFragment extends Fragment {

    public abstract boolean hasBack();

    public void onPrepareOptionsMenu_(Menu menu) {
    }

    public void onOptionsItemSelected_(MenuItem item) {

    }
}
