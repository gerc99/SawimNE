package ru.sawim.view;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.form.Forms;
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
public class FormView extends SawimFragment implements Forms.OnUpdateForm, View.OnClickListener {

    public static final String TAG = FormView.class.getSimpleName();
    private TextView textView;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    private LinearLayout listLayout;
    private Button okButton;
    private Button cancelButton;
    private int padding;

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
        okButton = (Button) getActivity().findViewById(R.id.data_form_ok);
        okButton.setOnClickListener(this);
        buildList();
        cancelButton = (Button) getActivity().findViewById(R.id.data_form_cancel);
        cancelButton.setOnClickListener(this);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.form, container, false);
        textView = (TextView) v.findViewById(R.id.textView);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        scrollView = (ScrollView) v.findViewById(R.id.data_form_scroll);
        listLayout = (LinearLayout) v.findViewById(R.id.data_form_linear);
        padding = Util.dipToPixels(getActivity(), 6);
        if (!Scheme.isSystemBackground())
            v.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));

        updateForm(false);
        return v;
    }

    public static void show() {
        BaseActivity.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FormView formView = new FormView();
                if (BaseActivity.getCurrentActivity().isFinishing()) {
                    SawimApplication.fragmentsStack.add(formView);
                } else {
                    show(formView);
                }
            }
        });
    }

    public static void showLastWindow() {
        int fragmentsStackCount = SawimApplication.fragmentsStack.size();
        if (fragmentsStackCount > 0) {
            show(SawimApplication.fragmentsStack.get(fragmentsStackCount - 1));
            SawimApplication.fragmentsStack.remove(fragmentsStackCount - 1);
        }
    }

    private static void show(Fragment fragment) {
        if (SawimApplication.isManyPane())
            BaseActivity.getCurrentActivity().setContentView(R.layout.intercalation_layout);
        FragmentTransaction transaction = BaseActivity.getCurrentActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, FormView.TAG);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void onResume() {
        super.onResume();
        BaseActivity.getCurrentActivity().resetBar(Forms.getInstance().getCaption());
        BaseActivity.getCurrentActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void updateForm(final boolean isLoad) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isLoad && Forms.getInstance().getWaitingString() != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    scrollView.setVisibility(View.GONE);
                    textView.setVisibility(View.VISIBLE);
                    textView.setText(Forms.getInstance().getWaitingString());
                }
                if (isLoad) {
                    textView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                    buildList();
                }
                if (Forms.getInstance().getErrorString() != null) {
                    textView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    scrollView.setVisibility(View.GONE);
                    textView.setText(Forms.getInstance().getErrorString());
                }
            }
        });
    }

    @Override
    public void back() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (SawimApplication.isManyPane())
                    ((SawimActivity) BaseActivity.getCurrentActivity()).recreateActivity();
                else if (!BaseActivity.getCurrentActivity().isFinishing())
                    BaseActivity.getCurrentActivity().getSupportFragmentManager().popBackStack();
                hideKeyboard();
                BaseActivity.getCurrentActivity().supportInvalidateOptionsMenu();
            }
        });
    }

    private void hideKeyboard() {
        if (BaseActivity.getCurrentActivity().getCurrentFocus() != null)
            ((InputMethodManager) BaseActivity.getCurrentActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(BaseActivity.getCurrentActivity().getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    public void onClick(View view) {
        if (Forms.getInstance().getFormListener() != null)
            Forms.getInstance().getFormListener().formAction(Forms.getInstance(), view.equals(okButton));
        hideKeyboard();
    }

    public boolean hasBack() {
        hideKeyboard();
        return Forms.getInstance().getBackPressedListener() == null || Forms.getInstance().getBackPressedListener().back();
    }

    private void buildList() {
        listLayout.removeAllViews();
        List<Forms.Control> controls = Forms.getInstance().controls;
        int fontSize = SawimApplication.getFontSize();
        for (int position = 0; position < controls.size(); ++position) {
            final Forms.Control c = controls.get(position);
            switch (c.type) {
                case Forms.CONTROL_TEXT:
                    drawText(c, listLayout);
                    break;
                case Forms.CONTROL_INPUT:
                    EditText editText = new EditText(getActivity());
                    drawText(c, listLayout);
                    editText.setHint(R.string.enter_the);
                    editText.setText(c.text);
                    editText.addTextChangedListener(new TextWatcher() {

                        public void afterTextChanged(Editable s) {
                        }

                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            c.text = s.toString();
                            Forms.getInstance().controlUpdated(c);
                        }
                    });
                    listLayout.addView(editText);
                    break;
                case Forms.CONTROL_CHECKBOX:
                    CheckBox checkBox = new CheckBox(getActivity());
                    checkBox.setText(c.description);
                    checkBox.setChecked(c.selected);
                    checkBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            c.selected = !c.selected;
                            Forms.getInstance().controlUpdated(c);
                        }
                    });
                    listLayout.addView(checkBox);
                    break;
                case Forms.CONTROL_SELECT:
                    drawText(c, listLayout);
                    MySpinner spinner = new MySpinner(getActivity());
                    MySpinnerAdapter adapter = new MySpinnerAdapter(getActivity(), c.items);
                    spinner.setPadding(0, padding, 0, padding);
                    spinner.setAdapter(adapter);
                    spinner.setPrompt(c.description);
                    spinner.setSelection(c.current);
                    spinner.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                            c.current = position;
                            Forms.getInstance().controlUpdated(c);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                        }
                    });
                    listLayout.addView(spinner);
                    break;
                case Forms.CONTROL_GAUGE:
                    drawText(c, listLayout);
                    SeekBar seekBar = new SeekBar(getActivity());
                    seekBar.setPadding(0, padding, 0, padding);
                    seekBar.setProgress(c.level);
                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                            c.level = i;
                            Forms.getInstance().controlUpdated(c);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
                    listLayout.addView(seekBar);
                    break;
                case Forms.CONTROL_GAUGE_FONT:
                    SeekBar seekBarFont = new SeekBar(getActivity());
                    final TextView descView = new TextView(getActivity());
                    descView.setTextSize(fontSize);
                    descView.setText(c.description + "(" + c.level + ")");
                    seekBarFont.setMax(60);
                    seekBarFont.setProgress(c.level);
                    seekBarFont.setPadding(0, padding, 0, padding);
                    seekBarFont.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                            c.level = i;
                            descView.setTextSize(c.level);
                            descView.setText(c.description + "(" + c.level + ")");
                            Forms.getInstance().controlUpdated(c);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
                    listLayout.addView(descView);
                    listLayout.addView(seekBarFont);
                    break;
                case Forms.CONTROL_IMAGE:
                    final ImageView imageView = new ImageView(getActivity());
                    imageView.setPadding(0, padding, 0, padding);
                    imageView.setAdjustViewBounds(true);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageDrawable(c.image);
                    drawText(c, listLayout);
                    listLayout.addView(imageView);
                    break;
                case Forms.CONTROL_LINK:
                    drawText(c, listLayout);
                    break;
                case Forms.CONTROL_BUTTON:
                    Button button = new Button(getActivity());
                    button.setText(getText(c));
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideKeyboard();
                            Forms.getInstance().controlUpdated(c);
                        }
                    });
                    listLayout.addView(button);
                    break;
            }
        }
    }

    private void drawText(Forms.Control c, LinearLayout convertView) {
        final TextView descView = new TextView(getActivity());
        final TextView labelView = new TextView(getActivity());
        if (c.label != null) {
            labelView.setPadding(0, padding, 0, padding);
            labelView.setText(c.label);
            convertView.addView(labelView);
        }
        if (c.description != null) {
            descView.setPadding(0, padding, 0, padding);
            descView.setText(c.description);
            convertView.addView(descView);
        }
    }

    private String getText(Forms.Control c) {
        String s = "";
        if (c.label != null) s = c.label;
        if (c.label != null && c.description != null) s += "\n";
        if (c.description != null) s += c.description;
        return s;
    }

    static class MySpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

        private String[] items;
        Context context;
        LayoutInflater layoutInflater;

        public MySpinnerAdapter(Context context, String[] items) {
            this.context = context;
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public String getItem(int i) {
            return items[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            View v = convertView;
            HeaderViewHolder headerViewHolder;
            String string = getItem(position);
            if (v == null) {
                v = layoutInflater.inflate(R.layout.spinner_item, null);
                headerViewHolder = new HeaderViewHolder();
                headerViewHolder.header = (TextView) v.findViewById(R.id.header);
                v.setTag(headerViewHolder);
            } else {
                headerViewHolder = (HeaderViewHolder) v.getTag();
            }
            if (string == null) return v;
            if (Scheme.isBlack() && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                headerViewHolder.header.setTextColor(0xff000000);
            headerViewHolder.header.setTextSize(SawimApplication.getFontSize());
            headerViewHolder.header.setText(string);
            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            DropDownViewHolder dropDownViewHolder;
            String string = getItem(position);
            if (v == null) {
                v = layoutInflater.inflate(R.layout.spinner_dropdown_item, null);
                dropDownViewHolder = new DropDownViewHolder();
                dropDownViewHolder.label = (TextView) v.findViewById(R.id.label);
                v.setTag(dropDownViewHolder);
            } else {
                dropDownViewHolder = (DropDownViewHolder) v.getTag();
            }
            if (string == null) return v;
            if (Util.isNeedToFixSpinnerAdapter()) {
                dropDownViewHolder.label.setTextColor(0xff000000);
            }
            dropDownViewHolder.label.setTextSize(SawimApplication.getFontSize());
            dropDownViewHolder.label.setText(string);
            return v;
        }

        static class HeaderViewHolder {
            TextView header;
        }

        static class DropDownViewHolder {
            TextView label;
        }
    }
}