package ru.sawim.view;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import ru.sawim.SawimApplication;
import ru.sawim.R;
import ru.sawim.Scheme;
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

    public static final String TAG = "FormView";
    private LinearLayout listLayout;
    private Button okButton;
    private Button cancelButton;

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
        LinearLayout rootLayout = (LinearLayout) v.findViewById(R.id.data_form);
        listLayout = (LinearLayout) v.findViewById(R.id.data_form_linear);
        if (!Scheme.isSystemBackground())
            rootLayout.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        return v;
    }

    public static void show() {
        SawimApplication.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SawimActivity.resetBar();
                if (SawimApplication.getCurrentActivity().getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    SawimApplication.getCurrentActivity().setContentView(R.layout.intercalation_layout);
                FormView newFragment = new FormView();
                FragmentTransaction transaction = SawimApplication.getCurrentActivity().getSupportFragmentManager().beginTransaction();
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
                if (SawimApplication.getCurrentActivity().getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    ((SawimActivity) SawimApplication.getCurrentActivity()).recreateActivity();
                else
                    getFragmentManager().popBackStack();
                hideKeyboard();
                SawimApplication.getCurrentActivity().supportInvalidateOptionsMenu();
            }
        });
    }

    private void hideKeyboard() {
        if (SawimApplication.getCurrentActivity().getCurrentFocus() != null)
            ((InputMethodManager) SawimApplication.getCurrentActivity().getSystemService("input_method")).hideSoftInputFromWindow(SawimApplication.getCurrentActivity().getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    public void onClick(View view) {
        if (Forms.getInstance().getFormListener() != null)
            Forms.getInstance().getFormListener().formAction(Forms.getInstance(), view.equals(okButton));
        hideKeyboard();
    }

    public boolean hasBack() {
        hideKeyboard();
        if (Forms.getInstance().getBackPressedListener() == null) return true;
        return Forms.getInstance().getBackPressedListener().back();
    }

    private void buildList() {
        listLayout.removeAllViews();
        List<Forms.Control> controls = Forms.getInstance().controls;
        int padding = Util.dipToPixels(getActivity(), 15);
        for (int position = 0; position < controls.size(); ++position) {
            final Forms.Control c = controls.get(position);
            ViewHolder holder = new ViewHolder();
            holder.imageView = new ImageView(getActivity());
            holder.descView = new TextView(getActivity());
            holder.labelView = new TextView(getActivity());
            holder.checkBox = new CheckBox(getActivity());
            holder.spinner = new MySpinner(getActivity());
            holder.seekBar = new SeekBar(getActivity());
            holder.editText = new EditText(getActivity());

            final ImageView imageView = holder.imageView;
            final TextView descView = holder.descView;
            final TextView labelView = holder.labelView;
            final CheckBox checkBox = holder.checkBox;
            final MySpinner spinner = holder.spinner;
            final SeekBar seekBar = holder.seekBar;
            final EditText editText = holder.editText;

            descView.setVisibility(TextView.GONE);
            labelView.setVisibility(TextView.GONE);
            imageView.setVisibility(ImageView.GONE);
            checkBox.setVisibility(CheckBox.GONE);
            spinner.setVisibility(Spinner.GONE);
            seekBar.setVisibility(SeekBar.GONE);
            editText.setVisibility(EditText.GONE);
            setAllTextSize(descView, labelView, checkBox, editText, SawimApplication.getFontSize());

            imageView.setPadding(0, padding, 0, padding);
            labelView.setPadding(0, padding, 0, padding);
            spinner.setPadding(0, padding, 0, padding);
            seekBar.setPadding(0, padding, 0, padding);

            if (Forms.CONTROL_TEXT == c.type) {
                drawText(c, labelView, descView, listLayout);
            } else if (Forms.CONTROL_INPUT == c.type) {
                drawText(c, labelView, descView, listLayout);
                editText.setVisibility(EditText.VISIBLE);
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
            } else if (Forms.CONTROL_CHECKBOX == c.type) {
                checkBox.setVisibility(CheckBox.VISIBLE);
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
            } else if (Forms.CONTROL_SELECT == c.type) {
                drawText(c, labelView, descView, listLayout);
                spinner.setVisibility(Spinner.VISIBLE);
                MySpinnerAdapter adapter = new MySpinnerAdapter(getActivity(), c.items);
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
            } else if (Forms.CONTROL_GAUGE == c.type) {
                drawText(c, labelView, descView, listLayout);
                seekBar.setVisibility(SeekBar.VISIBLE);
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
            } else if (Forms.CONTROL_GAUGE_FONT == c.type) {
                descView.setVisibility(TextView.VISIBLE);
                descView.setText(c.description + "(" + c.level + ")");
                seekBar.setVisibility(SeekBar.VISIBLE);
                seekBar.setMax(60);
                seekBar.setProgress(c.level);
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                listLayout.addView(seekBar);
            } else if (Forms.CONTROL_IMAGE == c.type) {
                drawText(c, labelView, descView, listLayout);
                imageView.setVisibility(ImageView.VISIBLE);
                imageView.setImageDrawable(c.image);
                listLayout.addView(imageView);
            } else if (Forms.CONTROL_LINK == c.type) {
                drawText(c, labelView, descView, listLayout);
            }
        }
    }

    private void setAllTextSize(TextView descView, TextView labelView, CheckBox checkBox, EditText editText, int size) {
        descView.setTextSize(size - 1);
        labelView.setTextSize(size - 1);
        checkBox.setTextSize(size);
        editText.setTextSize(size);
    }

    private void drawText(Forms.Control c, TextView labelView, TextView descView, LinearLayout convertView) {
        if (c.label != null) {
            labelView.setVisibility(TextView.VISIBLE);
            labelView.setText(c.label);
            convertView.addView(labelView);
        }
        if (c.description != null) {
            descView.setVisibility(TextView.VISIBLE);
            descView.setText(c.description);
            convertView.addView(descView);
        }
    }

    static class ViewHolder {
        ImageView imageView;
        TextView descView;
        TextView labelView;
        CheckBox checkBox;
        MySpinner spinner;
        SeekBar seekBar;
        EditText editText;
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
            if (Scheme.isBlack() && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                dropDownViewHolder.label.setTextColor(0xff000000);
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