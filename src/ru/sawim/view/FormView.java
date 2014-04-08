package ru.sawim.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import protocol.ContactMenu;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.chat.Chat;
import ru.sawim.models.form.Forms;
import ru.sawim.util.JLocale;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.MySpinner;
import ru.sawim.widget.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.06.13
 * Time: 21:30
 * To change this template use File | Settings | File Templates.
 */
public class FormView extends DialogFragment implements Forms.OnUpdateForm, View.OnClickListener, DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

    public static final String TAG = FormView.class.getSimpleName();
    private TextView textView;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    private LinearLayout listLayout;
    private Button okButton;
    private Button cancelButton;
    private int padding;
    private static ArrayList<FormView> fragmentsStack = new ArrayList<FormView>();
    private static ArrayList<Forms> forms = new ArrayList<Forms>();

    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        getLastForms().setUpdateFormListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getLastForms().setUpdateFormListener(null);
        forms.remove(forms.size() - 1);
    }

    private static Forms getLastForms() {
         return forms.get(forms.size() - 1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(getLastForms().getCaption());
        getDialog().setCanceledOnTouchOutside(false);
        View v = inflater.inflate(R.layout.form, container, false);
        textView = (TextView) v.findViewById(R.id.textView);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        scrollView = (ScrollView) v.findViewById(R.id.data_form_scroll);
        listLayout = (LinearLayout) v.findViewById(R.id.data_form_linear);
        padding = Util.dipToPixels(getActivity(), 6);

        okButton = (Button) v.findViewById(R.id.data_form_ok);
        okButton.setOnClickListener(this);
        buildList();
        cancelButton = (Button) v.findViewById(R.id.data_form_cancel);
        cancelButton.setOnClickListener(this);
        getActivity().supportInvalidateOptionsMenu();
        updateForm(false);
        return v;
    }

    public static void show(final Forms forms) {
        BaseActivity.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final FormView formView = new FormView();
                formView.forms.add(forms);
                if (BaseActivity.getCurrentActivity().isFinishing()) {
                    fragmentsStack.add(formView);
                } else {
                    show(formView);
                }
            }
        });
    }

    public static void showLastWindow() {
        int fragmentsStackCount = fragmentsStack.size();
        if (fragmentsStackCount > 0) {
            show(fragmentsStack.get(fragmentsStackCount - 1));
            fragmentsStack.remove(fragmentsStackCount - 1);
        }
    }

    private static void show(FormView formView) {
        formView.show(BaseActivity.getCurrentActivity().getSupportFragmentManager().beginTransaction(), "form");
    }

    @Override
    public void updateForm(final boolean isLoad) {
        BaseActivity.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isLoad && getLastForms().getWaitingString() != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    scrollView.setVisibility(View.GONE);
                    textView.setVisibility(View.VISIBLE);
                    textView.setText(getLastForms().getWaitingString());
                }
                if (isLoad) {
                    textView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                    buildList();
                }
                if (getLastForms().getErrorString() != null) {
                    textView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    scrollView.setVisibility(View.GONE);
                    textView.setText(getLastForms().getErrorString());
                }
            }
        });
    }

    @Override
    public void back() {
        BaseActivity.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        });
    }

    private void hideKeyboard() {
        if (BaseActivity.getCurrentActivity().getCurrentFocus() != null)
            ((InputMethodManager) BaseActivity.getCurrentActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(BaseActivity.getCurrentActivity().getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    public void onClick(View view) {
        if (getLastForms().getFormListener() != null)
            getLastForms().getFormListener()
                    .formAction(getLastForms(), view.equals(okButton));
        hideKeyboard();
        dismiss();
    }

    public boolean hasBack() {
        hideKeyboard();
        return getLastForms().getBackPressedListener() == null || getLastForms().getBackPressedListener().back();
    }

    private void buildList() {
        Context context = BaseActivity.getCurrentActivity();
        listLayout.removeAllViews();
        List<Forms.Control> controls = getLastForms().controls;
        int fontSize = SawimApplication.getFontSize();
        for (int position = 0; position < controls.size(); ++position) {
            final Forms.Control c = controls.get(position);
            switch (c.type) {
                case Forms.CONTROL_TEXT:
                    drawText(context, c, listLayout);
                    break;
                case Forms.CONTROL_INPUT:
                    EditText editText = new EditText(context);
                    drawText(context, c, listLayout);
                    editText.setHint(R.string.enter_the);
                    editText.setText(c.text);
                    editText.addTextChangedListener(new TextWatcher() {

                        public void afterTextChanged(Editable s) {
                        }

                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            c.text = s.toString();
                            getLastForms().controlUpdated(c);
                        }
                    });
                    listLayout.addView(editText);
                    break;
                case Forms.CONTROL_CHECKBOX:
                    CheckBox checkBox = new CheckBox(context);
                    checkBox.setText(c.description);
                    checkBox.setChecked(c.selected);
                    checkBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            c.selected = !c.selected;
                            getLastForms().controlUpdated(c);
                        }
                    });
                    listLayout.addView(checkBox);
                    break;
                case Forms.CONTROL_SELECT:
                    drawText(context, c, listLayout);
                    MySpinner spinner = new MySpinner(context);
                    MySpinnerAdapter adapter = new MySpinnerAdapter(context, c.items);
                    spinner.setPadding(0, padding, 0, padding);
                    spinner.setAdapter(adapter);
                    spinner.setPrompt(c.description);
                    spinner.setSelection(c.current);
                    spinner.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                            c.current = position;
                            getLastForms().controlUpdated(c);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                        }
                    });
                    listLayout.addView(spinner);
                    break;
                case Forms.CONTROL_GAUGE:
                    drawText(context, c, listLayout);
                    SeekBar seekBar = new SeekBar(context);
                    seekBar.setPadding(0, padding, 0, padding);
                    seekBar.setProgress(c.level);
                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                            c.level = i;
                            getLastForms().controlUpdated(c);
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
                    SeekBar seekBarFont = new SeekBar(context);
                    final TextView descView = new TextView(context);
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
                            getLastForms().controlUpdated(c);
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
                    final ImageView imageView = new ImageView(context);
                    imageView.setPadding(0, padding, 0, padding);
                    imageView.setAdjustViewBounds(true);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageDrawable(c.image);
                    drawText(context, c, listLayout);
                    listLayout.addView(imageView);
                    break;
                case Forms.CONTROL_LINK:
                    drawText(context, c, listLayout);
                    break;
                case Forms.CONTROL_BUTTON:
                    Button button = new Button(context);
                    button.setText(getText(c));
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideKeyboard();
                            getLastForms().controlUpdated(c);
                        }
                    });
                    listLayout.addView(button);
                    break;
            }
        }
    }

    private void drawText(Context context, Forms.Control c, LinearLayout convertView) {
        final TextView descView = new TextView(context);
        final TextView labelView = new TextView(context);
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