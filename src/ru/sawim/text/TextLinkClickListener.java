package ru.sawim.text;

import android.view.View;
import ru.sawim.activities.BaseActivity;

/**
 * Created by Gerc on 13.01.14.
 */
public interface TextLinkClickListener {
    public void onTextLinkClick(View textView, String clickedString, boolean isLongTap);
}
