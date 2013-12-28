package ru.sawim.view.preference;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.preference.Preference;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 27.12.13
 * Time: 20:14
 * To change this template use File | Settings | File Templates.
 */
public class EditTextPreference extends Preference {

    private String editText;
    private TextWatcher textWatcher;

    public EditTextPreference(Context context) {
        super(context);
    }

    public EditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setText(String text) {
        this.editText = text;
    }

    public String getText() {
        return editText;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        ((ViewGroup)view).removeAllViews();
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText editTextView = new EditText(getContext());
        editTextView.setVisibility(View.GONE);
        TextView textView = new TextView(getContext());
        textView.setId(android.R.id.title);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextAppearance(getContext(), android.R.style.TextAppearance_DeviceDefault_Medium);
        textView.setTextColor(textView.getCurrentTextColor());
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextView.setVisibility(View.VISIBLE);
                editTextView.requestFocus();
                showKeyboard(editTextView);
            }
        });
        editTextView.setText(getText());
        editTextView.addTextChangedListener(textWatcher);
        textView.setText(getSummary());
        layout.addView(textView);
        layout.addView(editTextView);
        ((ViewGroup)view).addView(layout);
    }

    private void showKeyboard(View view) {
        Configuration conf = Resources.getSystem().getConfiguration();
        if (conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO) {
            InputMethodManager keyboard = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        this.textWatcher = textWatcher;
    }
}
