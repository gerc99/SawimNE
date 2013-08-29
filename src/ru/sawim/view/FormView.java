package ru.sawim.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
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
        okButton = (Button) getActivity().findViewById(R.id.data_form_ok);
        okButton.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_BACKGROUND), PorterDuff.Mode.MULTIPLY);
        okButton.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        okButton.setOnClickListener(this);
        buildList(listLayout);
        cancelButton = (Button) getActivity().findViewById(R.id.data_form_cancel);
        cancelButton.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_BACKGROUND), PorterDuff.Mode.MULTIPLY);
        cancelButton.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        cancelButton.setOnClickListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.form, container, false);
        LinearLayout rootLayout = (LinearLayout) v.findViewById(R.id.data_form);
        rootLayout.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
        listLayout = (LinearLayout) v.findViewById(R.id.data_form_linear);
        return v;
    }

    public static void show(FragmentActivity a) {
        if (a.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            a.setContentView(R.layout.intercalation_layout);
        FormView newFragment = new FormView();
        FragmentTransaction transaction = a.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment, FormView.TAG);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    public static void show() {
        show(SawimActivity.getInstance());
    }

    @Override
    public void updateForm() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buildList(listLayout);
            }
        });
    }

    @Override
    public void back() {
        if (SawimActivity.getInstance().getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            SawimActivity.getInstance().recreateActivity();
        else
            getFragmentManager().popBackStack();
    }

    @Override
    public void onClick(View view) {
        if (Forms.getInstance().getFormListener() != null)
            Forms.getInstance().getFormListener().formAction(Forms.getInstance(), view.equals(okButton));
        Forms.getInstance().clearListeners();
    }

    public boolean hasBack() {
        if (Forms.getInstance().getBackPressedListener() == null) return true;
        return Forms.getInstance().getBackPressedListener().back();
    }

    private void buildList(final LinearLayout convertView) {
        convertView.removeAllViews();
        List<Forms.Control> controls = Forms.getInstance().controls;
        for (int position = 0; position < controls.size(); ++position) {
            final Forms.Control c = controls.get(position);
            ViewHolder holder = new ViewHolder();
            holder.imageView = new ImageView(getActivity());
            holder.textView = new TextView(getActivity());
            holder.descView = new TextView(getActivity());
            holder.labelView = new TextView(getActivity());
            holder.checkBox = new CheckBox(getActivity());
            holder.spinner = new MySpinner(getActivity());
            holder.seekBar = new SeekBar(getActivity());
            holder.editText = new EditText(getActivity());

            final ImageView imageView = holder.imageView;
            final TextView textView = holder.textView;
            final TextView descView = holder.descView;
            final TextView labelView = holder.labelView;
            final CheckBox checkBox = holder.checkBox;
            final MySpinner spinner = holder.spinner;
            final SeekBar seekBar = holder.seekBar;
            final EditText editText = holder.editText;

            descView.setVisibility(TextView.GONE);
            labelView.setVisibility(TextView.GONE);
            textView.setVisibility(TextView.GONE);
            imageView.setVisibility(ImageView.GONE);
            checkBox.setVisibility(CheckBox.GONE);
            spinner.setVisibility(Spinner.GONE);
            seekBar.setVisibility(SeekBar.GONE);
            editText.setVisibility(EditText.GONE);
            setAllTextSize(descView, labelView, textView, checkBox, editText, General.getFontSize());
            descView.setTextColor(Scheme.getInversColor(Scheme.THEME_TEXT));
            labelView.setTextColor(Scheme.getInversColor(Scheme.THEME_TEXT));
            textView.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
            checkBox.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
            spinner.getBackground().setColorFilter(Scheme.getColor(Scheme.THEME_BACKGROUND), PorterDuff.Mode.MULTIPLY);
            seekBar.setBackgroundColor(Scheme.getColor(Scheme.THEME_BACKGROUND));
            editText.setTextColor(Scheme.getInversColor(Scheme.THEME_TEXT));
            if (Forms.CONTROL_TEXT == c.type) {
                drawText(c, labelView, descView, convertView);
            } else if (Forms.CONTROL_INPUT == c.type) {
                drawText(c, labelView, descView, convertView);
                editText.setVisibility(EditText.VISIBLE);
                editText.setText(c.text);
                editText.addTextChangedListener(new TextWatcher() {

                    public void afterTextChanged(Editable s) { }

                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        c.text = s.toString();
                        Forms.getInstance().controlUpdated(c);
                    }
                });
                convertView.addView(editText);
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
                convertView.addView(checkBox);
            } else if (Forms.CONTROL_SELECT == c.type) {
                drawText(c, labelView, descView, convertView);
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
                convertView.addView(spinner);
            } else if (Forms.CONTROL_GAUGE == c.type) {
                drawText(c, labelView, descView, convertView);
                seekBar.setVisibility(SeekBar.VISIBLE);
                seekBar.setProgress(c.level);
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        c.level = i;
                        Forms.getInstance().controlUpdated(c);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) { }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) { }
                });
                convertView.addView(seekBar);
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
                    public void onStartTrackingTouch(SeekBar seekBar) { }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) { }
                });
                convertView.addView(descView);
                convertView.addView(seekBar);
            } else if (Forms.CONTROL_IMAGE == c.type) {
                drawText(c, labelView, descView, convertView);
                imageView.setVisibility(ImageView.VISIBLE);
                imageView.setImageDrawable(c.image);
                convertView.addView(imageView);
            } else if (Forms.CONTROL_LINK == c.type) {
                drawText(c, labelView, descView, convertView);
            }
        }
    }

    private void setAllTextSize(TextView descView, TextView labelView, TextView textView, CheckBox checkBox, EditText editText, int size) {
        descView.setTextSize(size - 1);
        labelView.setTextSize(size - 1);
        textView.setTextSize(size);
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
        TextView textView;
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
            headerViewHolder.header.setTextSize(General.getFontSize());
            headerViewHolder.header.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
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
            v.setBackgroundColor(Scheme.getInversColor(Scheme.THEME_BACKGROUND));

            dropDownViewHolder.label.setTextSize(General.getFontSize());
            dropDownViewHolder.label.setTextColor(Scheme.getInversColor(Scheme.THEME_TEXT));
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