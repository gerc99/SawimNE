package ru.sawim.activities;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.view.SendToView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 26.08.13
 * Time: 18:54
 * To change this template use File | Settings | File Templates.
 */
public class SendToActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        General.currentActivity = this;
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.intercalation_layout);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            SendToView rosterView = new SendToView();
            rosterView.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, rosterView, SendToView.TAG).commit();
        }
    }
}
