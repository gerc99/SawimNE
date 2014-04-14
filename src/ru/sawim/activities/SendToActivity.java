package ru.sawim.activities;

import android.content.Intent;
import android.os.Bundle;
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
public class SendToActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        Intent intent = new Intent(this, SawimActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
