package ru.sawim.view.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 20.12.13
 * Time: 17:48
 * To change this template use File | Settings | File Templates.
 */
public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private SeekBar seekbar;
    private int progress;
    private int max = 100;
    private int min;

    private TextView title;
    private TextView summary;

    private boolean discard;

    /**
     * Perform inflation from XML and apply a class-specific base style.
     *
     * @param context  The Context this is associated with, through which it can
     *                 access the current theme, resources, {@link android.content.SharedPreferences},
     *                 etc.
     * @param attrs    The attributes of the XML tag that is inflating the preference.
     * @param defStyle The default style to apply to this preference. If 0, no style
     *                 will be applied (beyond what is included in the theme). This
     *                 may either be an attribute resource, whose value will be
     *                 retrieved from the current theme, or an explicit style
     *                 resource.
     * @see #SeekBarPreference(Context, AttributeSet)
     */
    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Constructor that is called when inflating a Preference from XML.
     *
     * @param context The Context this is associated with, through which it can
     *                access the current theme, resources, {@link android.content.SharedPreferences},
     *                etc.
     * @param attrs   The attributes of the XML tag that is inflating the
     *                preference.
     * @see #SeekBarPreference(Context, AttributeSet, int)
     */
    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor to create a Preference.
     *
     * @param context The Context in which to store Preference values.
     */
    public SeekBarPreference(Context context) {
        super(context);
    }

    /**
     * Create progress bar and other view contents.
     */
    protected View onCreateView(ViewGroup p) {

        final Context ctx = getContext();

        LinearLayout layout = new LinearLayout(ctx);
        layout.setId(android.R.id.widget_frame);
        layout.setOrientation(LinearLayout.VERTICAL);
        setPadding(layout, 15, 10, 15, 10);

        title = new TextView(ctx);
        int textColor = title.getCurrentTextColor();
        title.setId(android.R.id.title);
        title.setSingleLine();
        title.setTextAppearance(ctx, android.R.style.TextAppearance_Large);
        title.setTextColor(textColor);
        layout.addView(title);

        seekbar = new SeekBar(ctx);
        seekbar.setId(android.R.id.progress);
        seekbar.setMax(max);
        seekbar.setOnSeekBarChangeListener(this);
        layout.addView(seekbar);

        summary = new TextView(ctx);
        summary.setId(android.R.id.summary);
        summary.setSingleLine();
        summary.setTextAppearance(ctx, android.R.style.TextAppearance_Small);
        summary.setTextColor(textColor);
        layout.addView(summary);

        return layout;
    }

    public void setPadding(View view, int left, int top, int right, int bottom) {
        view.setPadding(px(left), px(top), px(right), px(bottom));
    }

    public int px(float dips) {
        return (int) (dips * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    public void setTitleText(CharSequence t) {
        title.setText(t);
    }

    public void setTitleTextSize(int i) {
        title.setTextSize(i);
    }

    /**
     * Binds the created View to the data for this Preference.
     */
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        if (seekbar != null)
            seekbar.setProgress(progress);
    }

    /**
     * <p>Set the current progress to the specified value. Does not do anything
     * if the progress bar is in indeterminate mode.</p>
     *
     * @param pcnt the new progress, between 0 and {@link SeekBar#getMax()}
     */
    public void setProgress(int pcnt) {
        if (progress != pcnt) {
            persistInt(progress = pcnt);

            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    /**
     * <p>Get the progress bar's current level of progress. Return 0 when the
     * progress bar is in indeterminate mode.</p>
     *
     * @return the current progress, between 0 and {@link SeekBar#getMax()}
     */
    public int getProgress() {
        return progress;
    }

    public void setMin(int min) {
        this.min = min;
    }

    /**
     * Set the max value for the <code>SeekBar</code> object.
     *
     * @param max max value
     */
    public void setMax(int max) {
        this.max = max;
        if (seekbar != null)
            seekbar.setMax(max);
    }

    /**
     * Get the underlying <code>SeekBar</code> object.
     *
     * @return <code>SeekBar</code> object
     */
    public SeekBar getSeekBar() {
        return seekbar;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, progress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setProgress(restoreValue ? getPersistedInt(progress) : (Integer) defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldDisableDependents() {
        return progress == 0 || super.shouldDisableDependents();
    }

    /**
     * Set the progress of the preference.
     */
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress <= min) {
            progress = min;
        }
        discard = !callChangeListener(progress);
    }

    /**
     * {@inheritDoc}
     */
    public void onStartTrackingTouch(SeekBar seekBar) {
        discard = false;
    }

    /**
     * {@inheritDoc}
     */
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (discard)
            seekBar.setProgress(progress);
        else {
            setProgress(seekBar.getProgress());

            OnPreferenceChangeListener listener = getOnPreferenceChangeListener();
            if (listener instanceof AbstractSeekBarListener)
                setSummary(((AbstractSeekBarListener) listener).toSummary(seekBar.getProgress()));
        }
    }

    /**
     * Abstract seek bar summary updater.
     *
     * @see #setSummary(String)
     */
    private static abstract class AbstractSeekBarListener implements Preference.OnPreferenceChangeListener {

        SeekBarPreference pref;

        /**
         * Construct a change lsitener for the specified widget.
         */
        public AbstractSeekBarListener(SeekBarPreference pref) {
            this.pref = pref;
        }

        /**
         * Sets the summary string directly into the text view
         * to avoid {@link SeekBarPreference#notifyChanged()} call
         * which was interrupting in the seek bar's thumb movement.
         */
        protected final void setSummary(String text) {
            if (pref.summary != null)
                pref.summary.setText(text);
        }

        /**
         * Convert integer progress to summary string.
         *
         * @param newValue should be an Integer instance
         */
        protected abstract String toSummary(Object newValue);

        /**
         * Preference change callback.
         */
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            updateSummary(newValue);

            return true;
        }

        /**
         * Update the summary text.
         */
        protected void updateSummary(Object newValue) {
            pref.setSummary(newValue == null ? "" : String.valueOf(newValue));
        }

    }
}

