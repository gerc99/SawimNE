package ru.sawim.view.preference;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.*;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.BaseActivity;
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

    public static final String TAG = PreferenceFormView.class.getSimpleName();
    private PreferenceScreen rootScreen;
    private static Forms forms;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(forms.getCaption());
        rootScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(rootScreen);
        buildList();
        getActivity().supportInvalidateOptionsMenu();
    }

    public static void show(BaseActivity activity, final Forms forms) {
        PreferenceFormView.forms = forms;
        if (SawimApplication.isManyPane())
            activity.setContentView(R.layout.main);
        PreferenceFormView newFragment = new PreferenceFormView();
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment, PreferenceFormView.TAG);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseActivity) getActivity()).resetBar(forms.getCaption());
        ((BaseActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void hideKeyboard() {
        if (getActivity().getCurrentFocus() != null)
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
    }

    public boolean hasBack() {
        hideKeyboard();
        return forms.getBackPressedListener() != null && forms.getBackPressedListener().back();
    }

    private void buildList() {
        rootScreen.removeAll();
        List<Forms.Control> controls = forms.controls;
        for (int position = 0; position < controls.size(); ++position) {
            final Forms.Control c = controls.get(position);
            switch (c.type) {
                case Forms.CONTROL_TEXT:
                    PreferenceCategory preferenceCategory = new PreferenceCategory(getActivity());
                    preferenceCategory.setKey(c.id);
                    preferenceCategory.setPersistent(false);
                    preferenceCategory.setTitle(getText(c));
                    rootScreen.addPreference(preferenceCategory);
                    break;
                case Forms.CONTROL_INPUT:
                    EditTextPreference editTextPreference = new EditTextPreference(getActivity());
                    editTextPreference.setKey(c.id);
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
                            forms.controlUpdated(c);
                        }
                    });
                    rootScreen.addPreference(editTextPreference);
                    break;
                case Forms.CONTROL_CHECKBOX:
                    CheckBoxPreference checkBoxPreference = new CheckBoxPreference(getActivity());
                    checkBoxPreference.setKey(c.id);
                    checkBoxPreference.setPersistent(false);
                    checkBoxPreference.setTitle(getText(c));
                    checkBoxPreference.setSummary(getText(c));
                    checkBoxPreference.setChecked(c.selected);
                    checkBoxPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference preference) {
                            c.selected = !c.selected;
                            forms.controlUpdated(c);
                            return true;
                        }
                    });
                    rootScreen.addPreference(checkBoxPreference);
                    break;
                case Forms.CONTROL_SELECT:
                    ListPreference listPreference = new ListPreference(getActivity());
                    listPreference.setKey(c.id);
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
                            forms.controlUpdated(c);
                            buildList();
                            return true;
                        }
                    });
                    rootScreen.addPreference(listPreference);
                    break;
                case Forms.CONTROL_GAUGE:
                    SeekBarPreference seekBarPreference = new SeekBarPreference(getActivity());
                    seekBarPreference.setKey(c.id);
                    seekBarPreference.setTitle(getText(c));
                    seekBarPreference.setDefaultValue(c.level);
                    seekBarPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            c.level = Integer.parseInt(newValue.toString());
                            forms.controlUpdated(c);
                            return true;
                        }
                    });
                    rootScreen.addPreference(seekBarPreference);
                    break;
                case Forms.CONTROL_GAUGE_FONT:
                    final SeekBarPreference fontSeekBarPreference = new SeekBarPreference(getActivity());
                    fontSeekBarPreference.setKey(c.id);
                    fontSeekBarPreference.setTitle(c.description + "(" + c.level + ")");
                    fontSeekBarPreference.setMax(60);
                    fontSeekBarPreference.setDefaultValue(c.level);
                    fontSeekBarPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            int value = Integer.parseInt(newValue.toString());
                            if (value <= 7) {
                                c.level = 7;
                            } else c.level = Integer.parseInt(newValue.toString());
                            fontSeekBarPreference.getSeekBar().setProgress(c.level);
                            fontSeekBarPreference.setTitleTextSize(c.level);
                            fontSeekBarPreference.setTitleText(c.description + "(" + c.level + ")");
                            forms.controlUpdated(c);
                            return true;
                        }
                    });
                    rootScreen.addPreference(fontSeekBarPreference);
                    break;
                case Forms.CONTROL_IMAGE:
                    IconPreferenceScreen iconPreferenceScreen = new IconPreferenceScreen(getActivity());
                    iconPreferenceScreen.setKey(c.id);
                    iconPreferenceScreen.setPersistent(false);
                    iconPreferenceScreen.setText(getText(c));
                    iconPreferenceScreen.setIcon(c.image);
                    rootScreen.addPreference(iconPreferenceScreen);
                    break;
                case Forms.CONTROL_LINK:
                    PreferenceCategory linkPreferenceCategory = new PreferenceCategory(getActivity());
                    linkPreferenceCategory.setKey(c.id);
                    linkPreferenceCategory.setPersistent(false);
                    linkPreferenceCategory.setTitle(getText(c));
                    rootScreen.addPreference(linkPreferenceCategory);
                    break;
                case Forms.CONTROL_RINGTONE:
                    RingtonePreference ringtonePreference = new RingtonePreference(getActivity()) {
                        public int mRequestCode = 111;

                        @Override
                        protected void onClick() {
                            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                            onPrepareRingtonePickerIntent(intent);
                            startActivityForResult(intent, mRequestCode);
                        }

                        public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
                            if (requestCode == mRequestCode) {
                                if (data != null) {
                                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                                    if (callChangeListener(uri != null ? uri.toString() : "")) {
                                        onSaveRingtone(uri);
                                        if (uri != null) {
                                            c.text = uri.toString();
                                        }
                                        forms.controlUpdated(c);
                                    }
                                }
                                return true;
                            }
                            return false;
                        }
                    };
                    ringtonePreference.setKey(c.id);
                    ringtonePreference.setTitle(c.label);
                    ringtonePreference.setSummary(c.description);
                    ringtonePreference.setShowSilent(true);
                    ringtonePreference.setShowDefault(true);
                    ringtonePreference.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
                    ringtonePreference.setDefaultValue("content://settings/system/notification_sound");
                    ringtonePreference.setPersistent(true);
                    rootScreen.addPreference(ringtonePreference);
                    break;
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