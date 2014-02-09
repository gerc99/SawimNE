package ru.sawim.view.preference;

import android.app.Activity;
import android.os.Bundle;
import android.preference.*;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import ru.sawim.SawimApplication;
import ru.sawim.R;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.form.Forms;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.06.13
 * Time: 21:30
 * To change this template use File | Settings | File Templates.
 */
public class PreferenceFormView extends PreferenceFragment {

    public static final String TAG = "PreferenceFormView";
    PreferenceScreen rootScreen;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(Forms.getInstance().getCaption());
        SawimApplication.getActionBar().setDisplayHomeAsUpEnabled(true);
        rootScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(rootScreen);
        buildList();
        getActivity().supportInvalidateOptionsMenu();
    }

    public static void show() {
        SawimApplication.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SawimActivity.resetBar();
                if (SawimApplication.getCurrentActivity().getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    SawimApplication.getCurrentActivity().setContentView(R.layout.intercalation_layout);
                PreferenceFormView newFragment = new PreferenceFormView();
                FragmentTransaction transaction = SawimApplication.getCurrentActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, newFragment, PreferenceFormView.TAG);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });
    }

    private void hideKeyboard() {
        if (SawimApplication.getCurrentActivity().getCurrentFocus() != null)
            ((InputMethodManager) SawimApplication.getCurrentActivity().getSystemService("input_method")).hideSoftInputFromWindow(SawimApplication.getCurrentActivity().getCurrentFocus().getWindowToken(), 0);
    }

    public boolean hasBack() {
        hideKeyboard();
        return Forms.getInstance().getBackPressedListener() == null || Forms.getInstance().getBackPressedListener().back();
    }

    private void buildList() {
        rootScreen.removeAll();
        List<Forms.Control> controls = Forms.getInstance().controls;
        for (int position = 0; position < controls.size(); ++position) {
            final Forms.Control c = controls.get(position);
            if (Forms.CONTROL_TEXT == c.type) {
                PreferenceCategory preferenceCategory = new PreferenceCategory(getActivity());
                preferenceCategory.setKey("" + c.id);
                preferenceCategory.setPersistent(false);
                preferenceCategory.setTitle(getText(c));
                rootScreen.addPreference(preferenceCategory);
            } else if (Forms.CONTROL_INPUT == c.type) {
                EditTextPreference editTextPreference = new EditTextPreference(getActivity());
                editTextPreference.setKey("" + c.id);
                editTextPreference.setPersistent(false);
                editTextPreference.setTitle(getText(c));
                editTextPreference.setSummary(getText(c));
                editTextPreference.setText(c.text);
                editTextPreference.getEditText().addTextChangedListener(new TextWatcher() {

                    public void afterTextChanged(Editable s) {
                    }

                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        c.text = s.toString();
                        Forms.getInstance().controlUpdated(c);
                    }
                });
                rootScreen.addPreference(editTextPreference);
            } else if (Forms.CONTROL_CHECKBOX == c.type) {
                CheckBoxPreference checkBoxPreference = new CheckBoxPreference(getActivity());
                checkBoxPreference.setKey("" + c.id);
                checkBoxPreference.setPersistent(false);
                checkBoxPreference.setTitle(getText(c));
                checkBoxPreference.setSummary(getText(c));
                checkBoxPreference.setChecked(c.selected);
                checkBoxPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        c.selected = !c.selected;
                        Forms.getInstance().controlUpdated(c);
                        return true;
                    }
                });
                rootScreen.addPreference(checkBoxPreference);
            } else if (Forms.CONTROL_SELECT == c.type) {
                ListPreference listPreference = new ListPreference(getActivity());
                listPreference.setKey("" + c.id);
                listPreference.setPersistent(false);
                listPreference.setTitle(getText(c));
                listPreference.setEntries(c.items);
                listPreference.setEntryValues(c.items);
                listPreference.setSummary(c.items[c.current]);
                listPreference.setValueIndex(c.current);
                listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String textValue = newValue.toString();
                        ListPreference listPreference = (ListPreference) preference;
                        int index = listPreference.findIndexOfValue(textValue);
                        c.current = index;
                        Forms.getInstance().controlUpdated(c);
                        buildList();
                        return true;
                    }
                });
                rootScreen.addPreference(listPreference);
            } else if (Forms.CONTROL_GAUGE == c.type) {
                SeekBarPreference seekBarPreference = new SeekBarPreference(getActivity());
                seekBarPreference.setKey("" + c.id);
                seekBarPreference.setTitle(getText(c));
                seekBarPreference.setDefaultValue(c.level);
                seekBarPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        c.level = Integer.parseInt(newValue.toString());
                        Forms.getInstance().controlUpdated(c);
                        return true;
                    }
                });
                rootScreen.addPreference(seekBarPreference);
            } else if (Forms.CONTROL_GAUGE_FONT == c.type) {
                final SeekBarPreference seekBarPreference = new SeekBarPreference(getActivity());
                seekBarPreference.setKey("" + c.id);
                seekBarPreference.setTitle(c.description + "(" + c.level + ")");
                seekBarPreference.setMax(60);
                seekBarPreference.setDefaultValue(c.level);
                seekBarPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        int value = Integer.parseInt(newValue.toString());
                        if (value <= 7) {
                            c.level = 7;
                        } else c.level = Integer.parseInt(newValue.toString());
                        seekBarPreference.getSeekBar().setProgress(c.level);
                        seekBarPreference.setTitleTextSize(c.level);
                        seekBarPreference.setTitleText(c.description + "(" + c.level + ")");
                        Forms.getInstance().controlUpdated(c);
                        if (SawimApplication.getInstance().getConfigurationChanged() != null)
                            SawimApplication.getInstance().getConfigurationChanged().onConfigurationChanged();
                        return true;
                    }
                });
                rootScreen.addPreference(seekBarPreference);
            } else if (Forms.CONTROL_IMAGE == c.type) {
                IconPreferenceScreen iconPreferenceScreen = new IconPreferenceScreen(getActivity());
                iconPreferenceScreen.setKey("" + c.id);
                iconPreferenceScreen.setPersistent(false);
                iconPreferenceScreen.setText(getText(c));
                iconPreferenceScreen.setIcon(c.image);
                rootScreen.addPreference(iconPreferenceScreen);
            } else if (Forms.CONTROL_LINK == c.type) {
                PreferenceCategory preferenceCategory = new PreferenceCategory(getActivity());
                preferenceCategory.setKey("" + c.id);
                preferenceCategory.setPersistent(false);
                preferenceCategory.setTitle(getText(c));
                rootScreen.addPreference(preferenceCategory);
            }
        }
    }

    private String getText(Forms.Control c) {
        String s = "";
        if (c.label != null) s = c.label;
        if (c.label != null && c.description != null) s += "\n";
        if (c.description != null) s += c.description;
        return s;
    }
}