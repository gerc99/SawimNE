package ru.sawim.activities;

import android.os.Bundle;
import ru.sawim.Container;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.listener.OnCreateListener;

public class EmptyActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Container.get(OnCreateListener.class).onCreate(this);
        Container.remove(OnCreateListener.class);
    }
}
