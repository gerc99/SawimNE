package ru.sawim.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.models.form.Forms;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.06.13
 * Time: 21:30
 * To change this template use File | Settings | File Templates.
 */
public class FormView extends Fragment implements Forms.OnUpdateForm, View.OnClickListener {

    LinearLayout listLayout;
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
        okButton.setOnClickListener(this);
        buildList(listLayout);
        cancelButton = (Button) getActivity().findViewById(R.id.data_form_cancel);
        cancelButton.setOnClickListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.form, container, false);
        listLayout = (LinearLayout) v.findViewById(R.id.data_form_linear);
        return v;
    }

    private void buildList(LinearLayout convertView) {
        convertView.removeAllViews();
        List<Forms.Control> controls = Forms.getInstance().controls;
        for (int position = 0; position < controls.size(); ++position) {
            final Forms.Control c = controls.get(position);
            ImageView imageView = new ImageView(getActivity());
            TextView textView = new TextView(getActivity());
            TextView descView = new TextView(getActivity());
            TextView labelView = new TextView(getActivity());
            CheckBox checkBox = new CheckBox(getActivity());
            Spinner spinner = new Spinner(getActivity());
            SeekBar seekBar = new SeekBar(getActivity());
            EditText editText = new EditText(getActivity());

            descView.setVisibility(TextView.GONE);
            labelView.setVisibility(TextView.GONE);
            textView.setVisibility(TextView.GONE);
            imageView.setVisibility(ImageView.GONE);
            checkBox.setVisibility(CheckBox.GONE);
            spinner.setVisibility(Spinner.GONE);
            seekBar.setVisibility(SeekBar.GONE);
            editText.setVisibility(EditText.GONE);
            if (Forms.CONTROL_TEXT == c.type) {
                drawText(c, labelView, descView, convertView);
            } else if (Forms.CONTROL_INPUT == c.type) {
                drawText(c, labelView, descView, convertView);
                editText.setVisibility(EditText.VISIBLE);
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
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, c.items);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                spinner.setPrompt(c.description);
                spinner.setSelection(c.current);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                convertView.addView(seekBar);
            } else if (Forms.CONTROL_IMAGE == c.type) {
                drawText(c, labelView, descView, convertView);
                imageView.setVisibility(ImageView.VISIBLE);
                imageView.setImageBitmap(c.image);
                convertView.addView(imageView);
            } else if (Forms.CONTROL_LINK == c.type) {
                drawText(c, labelView, descView, convertView);
            }
        }
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

    @Override
    public void updateForm() {
        SawimApplication.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buildList(listLayout);
            }
        });
    }

    @Override
    public void back() {
        getActivity().finish();
    }

    @Override
    public void onClick(View view) {
        if (view.equals(cancelButton)) {
            Forms.getInstance().getFormListener().formAction(Forms.getInstance(), false);
        } else if (view.equals(okButton)) {
            new Thread() {
                public void run() {
                    SawimApplication.getInstance().runOnUiThread(new Runnable() {
                        public void run() {
                            Forms.getInstance().getFormListener().formAction(Forms.getInstance(), true);
                        }
                    });
                }
            }.start();
        }
    }

    public boolean onBackPressed() {
        if (Forms.getInstance().getBackPressedListener() == null) return true;
        return Forms.getInstance().getBackPressedListener().back();
    }
}
