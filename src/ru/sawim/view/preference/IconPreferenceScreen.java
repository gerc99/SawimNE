package ru.sawim.view.preference;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 20.12.13
 * Time: 18:55
 * To change this template use File | Settings | File Templates.
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import ru.sawim.R;

public class IconPreferenceScreen extends Preference {

    private Drawable mIcon;
    private CharSequence text;

    public IconPreferenceScreen(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_icon);
    }

    public IconPreferenceScreen(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        setLayoutResource(R.layout.preference_icon);
    }

    public IconPreferenceScreen(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_icon);
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        TextView textView = (TextView) view.findViewById(R.id.textView);
        if (textView != null && text != null) {
            textView.setText(text);
        }
        if (imageView != null && mIcon != null) {
            imageView.setImageDrawable(mIcon);
        }
    }

    public void setIcon(Drawable icon) {
        mIcon = icon;
        notifyChanged();
    }

    public void setText(CharSequence t) {
        text = t;
    }

    public Drawable getIcon() {
        return mIcon;
    }
}
