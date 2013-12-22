package ru.sawim.view.preference;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.sawim.R;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 22.12.13
 * Time: 0:42
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
        EditText editTextView = new EditText(getContext());
        TextView textView = new TextView(getContext());
        textView.setId(android.R.id.title);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setTextAppearance(getContext(), android.R.style.TextAppearance_DeviceDefault_Medium);
        textView.setTextColor(textView.getCurrentTextColor());
        editTextView.setText(getText());
        editTextView.addTextChangedListener(textWatcher);
        textView.setText(getSummary());
        layout.addView(textView);
        layout.addView(editTextView);
        ((ViewGroup)view).addView(layout);
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
          this.textWatcher = textWatcher;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.text = getText();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setText(myState.text);
    }

    private static class SavedState extends BaseSavedState {
        String text;

        public SavedState(Parcel source) {
            super(source);
            text = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(text);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
