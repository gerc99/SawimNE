package ru.sawim.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimNotification;
import ru.sawim.activities.BaseActivity;
import ru.sawim.models.form.Forms;
import ru.sawim.widget.MySpinner;
import ru.sawim.widget.Util;

import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.06.13
 * Time: 21:30
 * To change this template use File | Settings | File Templates.
 */
public class FormView extends DialogFragment implements Forms.OnUpdateForm, DialogInterface.OnClickListener, DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

    public static final String TAG = FormView.class.getSimpleName();
    private TextView textView;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    private LinearLayout listLayout;
    private Button okButton;
    private int padding;
    private Forms forms;
    private static HashMap<String, Forms> formsMap = new HashMap<String, Forms>();

    public FormView init(Forms forms) {
        this.forms = forms;
        return this;
    }

    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        getLastForms().setUpdateFormListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getLastForms().setUpdateFormListener(null);
        formsMap.remove(formsMap.size() - 1);
        SawimNotification.clear(getLastForms().getCaption().hashCode());
    }

    private Forms getLastForms() {
        return forms;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        View dialogView = LayoutInflater.from(context).inflate(R.layout.form, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(getLastForms().getCaption());
        dialogBuilder.setView(dialogView);

        dialogBuilder.setPositiveButton(R.string.ok, this);
        dialogBuilder.setNegativeButton(R.string.cancel, this);

        textView = (TextView) dialogView.findViewById(R.id.textView);
        progressBar = (ProgressBar) dialogView.findViewById(R.id.progressBar);
        scrollView = (ScrollView) dialogView.findViewById(R.id.data_form_scroll);
        listLayout = (LinearLayout) dialogView.findViewById(R.id.data_form_linear);
        padding = Util.dipToPixels(getActivity(), 6);

        getActivity().supportInvalidateOptionsMenu();
        buildList();
        updateForm(false);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    public static void show(final BaseActivity activity, final Forms f) {
        formsMap.put(f.getCaption(), f);
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!activity.isFinishing()) {
                        new FormView().init(f).show(activity.getSupportFragmentManager().beginTransaction(), "form");
                    }
                }
            });
        }
    }

    public static void showCaptcha(final Forms f) {
        formsMap.put(f.getCaption(), f);
        SawimNotification.captcha(f.getCaption());
    }

    public static void showWindows(final BaseActivity activity, final String title) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!activity.isFinishing()) {
                    FormView formView = new FormView().init(formsMap.get(title));
                    formView.show(activity.getSupportFragmentManager().beginTransaction(), "form");
                }
            }
        });
    }

    @Override
    public void updateForm(final boolean isLoad) {
        getActivity().runOnUiThread(new Runnable() {
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
                if (getLastForms().getWarningString() != null) {
                    textView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    scrollView.setVisibility(View.GONE);
                    textView.setText(getLastForms().getWarningString());
                }
            }
        });
    }

    @Override
    public void back() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (getLastForms().getFormListener() != null) {
            boolean isOkButton = button == DialogInterface.BUTTON_POSITIVE;
            getLastForms().getFormListener()
                          .formAction((BaseActivity)getActivity(), getLastForms(), isOkButton);
        }
        Util.hideKeyboard(getActivity());
    }

    private void buildList() {
        Context context = getActivity();
        listLayout.removeAllViews();
        List<Forms.Control> controls = getLastForms().controls;
        int fontSize = SawimApplication.getFontSize();
        for (final Forms.Control c : controls) {
            switch (c.type) {
                case Forms.CONTROL_TEXT:
                    drawText(context, c, listLayout);
                    break;
                case Forms.CONTROL_INPUT:
                    EditText editText = new AppCompatEditText(context);
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
                            getLastForms().controlUpdated((BaseActivity)getActivity(), c);
                        }
                    });
                    listLayout.addView(editText);
                    break;
                case Forms.CONTROL_CHECKBOX:
                    CheckBox checkBox = new AppCompatCheckBox(context);
                    checkBox.setText(c.description);
                    checkBox.setChecked(c.selected);
                    checkBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            c.selected = !c.selected;
                            getLastForms().controlUpdated((BaseActivity)getActivity(), c);
                        }
                    });
                    listLayout.addView(checkBox);
                    break;
                case Forms.CONTROL_SELECT:
                    drawText(context, c, listLayout);
                    MySpinner spinner = new MySpinner(context);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                            android.R.layout.simple_spinner_item, c.items);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setPadding(0, padding, 0, padding);
                    spinner.setAdapter(adapter);
                    spinner.setPrompt(c.description);
                    spinner.setSelection(c.current);
                    spinner.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                            c.current = position;
                            getLastForms().controlUpdated((BaseActivity)getActivity(), c);
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
                            getLastForms().controlUpdated((BaseActivity)getActivity(), c);
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
                            getLastForms().controlUpdated((BaseActivity)getActivity(), c);
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
                    imageView.setImageDrawable(c.image);
                    drawText(context, c, listLayout);
                    listLayout.addView(imageView);
                    break;
                case Forms.CONTROL_LINK:
                    drawText(context, c, listLayout);
                    break;
                case Forms.CONTROL_BUTTON:
                    Button button = new AppCompatButton(context);
                    button.setText(getText(c));
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Util.hideKeyboard(getActivity());
                            getLastForms().controlUpdated((BaseActivity)getActivity(), c);
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

}