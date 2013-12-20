package ru.sawim.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.form.Forms;
import ru.sawim.view.preference.IconPreferenceScreen;
import ru.sawim.view.preference.PreferenceFragment;
import ru.sawim.view.preference.SeekBarPreference;
import ru.sawim.widget.MySpinner;
import ru.sawim.widget.Util;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.06.13
 * Time: 21:30
 * To change this template use File | Settings | File Templates.
 */
public class FormView extends PreferenceFragment implements Forms.OnUpdateForm {

    public static final String TAG = "FormView";
    PreferenceScreen rootScreen;

    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        Forms.getInstance().setUpdateFormListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Forms.getInstance().setUpdateFormListener(null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(Forms.getInstance().getCaption());

        rootScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(rootScreen);

        buildList();
        getActivity().supportInvalidateOptionsMenu();
    }

    public static void show() {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SawimActivity.resetBar();
                if (General.currentActivity.getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    General.currentActivity.setContentView(R.layout.intercalation_layout);
                FormView newFragment = new FormView();
                FragmentTransaction transaction = General.currentActivity.getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, newFragment, FormView.TAG);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });
    }

    @Override
    public void updateForm() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buildList();
            }
        });
    }

    @Override
    public void back() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (General.currentActivity.getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    ((SawimActivity) General.currentActivity).recreateActivity();
                else
                    getFragmentManager().popBackStack();
                hideKeyboard();
                General.currentActivity.supportInvalidateOptionsMenu();
            }
        });
    }

    private void hideKeyboard() {
        if (General.currentActivity.getCurrentFocus() != null)
            ((InputMethodManager) General.currentActivity.getSystemService("input_method")).hideSoftInputFromWindow(General.currentActivity.getCurrentFocus().getWindowToken(), 0);
    }

    public boolean hasBack() {
        hideKeyboard();
        if (Forms.getInstance().getBackPressedListener() == null) return true;
        return Forms.getInstance().getBackPressedListener().back();
    }

    private void buildList() {
        rootScreen.removeAll();
        List<Forms.Control> controls = Forms.getInstance().controls;
        for (int position = 0; position < controls.size(); ++position) {
            final Forms.Control c = controls.get(position);
            if (Forms.CONTROL_TEXT == c.type) {
                PreferenceCategory preferenceCategory = new PreferenceCategory(getActivity());
                preferenceCategory.setKey("pc" + position);
                preferenceCategory.setTitle(getText(c));
                //preferenceCategory.setSummary("Description of category 1");
                rootScreen.addPreference(preferenceCategory);
            } else if (Forms.CONTROL_INPUT == c.type) {
                EditTextPreference editTextPreference = new EditTextPreference(getActivity());
                editTextPreference.setKey("et" + position);
                editTextPreference.setTitle(getText(c));
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
                checkBoxPreference.setKey("cb" + position);
                checkBoxPreference.setTitle(getText(c));
                //checkBoxPreference.setSummary(c.description);
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
                listPreference.setKey("l" + position);
                listPreference.setTitle(getText(c));
                listPreference.setEntries(c.items);
                listPreference.setEntryValues(c.items);
                listPreference.setValueIndex(c.current);
                listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String textValue = newValue.toString();
                        ListPreference listPreference = (ListPreference) preference;
                        int index = listPreference.findIndexOfValue(textValue);
                        c.current = index;
                        Forms.getInstance().controlUpdated(c);
                        return true;
                    }
                });
                rootScreen.addPreference(listPreference);
            } else if (Forms.CONTROL_GAUGE == c.type) {
                SeekBarPreference seekBarPreference = new SeekBarPreference(getActivity());
                seekBarPreference.setTitle(getText(c));
                seekBarPreference.setKey("sb" + position);
                seekBarPreference.setDefaultValue(c.level);
                seekBarPreference.setMax(60);
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
                seekBarPreference.setKey("sbf" + position);
                seekBarPreference.setTitle(c.description + "(" + c.level + ")");
                seekBarPreference.setDefaultValue(c.level);
                seekBarPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        c.level = Integer.parseInt(newValue.toString());
                        seekBarPreference.setTitleTextSize(c.level);
                        seekBarPreference.setTitleText(c.description + "(" + c.level + ")");
                        Forms.getInstance().controlUpdated(c);
                        return true;
                    }
                });
                rootScreen.addPreference(seekBarPreference);
            } else if (Forms.CONTROL_IMAGE == c.type) {
                IconPreferenceScreen iconPreferenceScreen = new IconPreferenceScreen(getActivity());
                iconPreferenceScreen.setIcon(c.image);
                rootScreen.addPreference(iconPreferenceScreen);
            } else if (Forms.CONTROL_LINK == c.type) {
                PreferenceCategory preferenceCategory = new PreferenceCategory(getActivity());
                preferenceCategory.setKey("pcl" + position);
                preferenceCategory.setTitle(getText(c));
                //preferenceCategory.setSummary("Description of category 1");
                rootScreen.addPreference(preferenceCategory);
            }
        }
    }

    private String getText(Forms.Control c) {
        return c.label == null ? c.description : c.label;
    }
}