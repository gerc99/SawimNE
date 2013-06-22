package ru.sawim.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import ru.sawim.R;
import ru.sawim.models.form.Forms;
import ru.sawim.view.FormView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.06.13
 * Time: 21:32
 * To change this template use File | Settings | File Templates.
 */
public class FormActivity extends FragmentActivity {

    private static FormActivity instance;

    public static FormActivity getInstance() {
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.form_fragment);
        setTitle(Forms.getInstance().caption);
    }

    @Override
    public void onBackPressed() {
        FormView view = (FormView) getSupportFragmentManager().findFragmentById(R.id.form_fragment);
        if (view.onBackPressed())
            super.onBackPressed();
    }
}
