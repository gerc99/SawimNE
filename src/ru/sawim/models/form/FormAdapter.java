package ru.sawim.models.form;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.sawim.General;
import ru.sawim.R;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.06.13
 * Time: 20:27
 * To change this template use File | Settings | File Templates.
 */
public class FormAdapter extends BaseAdapter {

    private List<Forms.Control> controls;
    private final Context context;

    public FormAdapter(Context context, List<Forms.Control> items) {
        this.context = context;
        this.controls = items;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return controls.size();
    }

    @Override
    public Forms.Control getItem(int i) {
        return controls.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        final Forms.Control c = controls.get(position);
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inf = LayoutInflater.from(context);
            convertView = inf.inflate(R.layout.form_item, null);
            holder = new ViewHolder();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        LinearLayout ll = (LinearLayout) convertView;
        holder.descView = (TextView) ll.findViewById(R.id.descView);
        holder.labelView = (TextView) ll.findViewById(R.id.labelView);
        holder.textView = (TextView) ll.findViewById(R.id.textView);
        holder.imageView = (ImageView) ll.findViewById(R.id.imageView);
        holder.checkBox = (CheckBox) ll.findViewById(R.id.checkBox);
        holder.spinner = (Spinner) ll.findViewById(R.id.spinner);
        holder.seekBar = (SeekBar) ll.findViewById(R.id.seekBar);
        holder.editText = (EditText) ll.findViewById(R.id.editTextBox);
        holder.descView.setVisibility(TextView.GONE);
        holder.labelView.setVisibility(TextView.GONE);
        holder.textView.setVisibility(TextView.GONE);
        holder.imageView.setVisibility(ImageView.GONE);
        holder.checkBox.setVisibility(CheckBox.GONE);
        holder.spinner.setVisibility(Spinner.GONE);
        holder.seekBar.setVisibility(SeekBar.GONE);
        holder.editText.setVisibility(EditText.GONE);
        if (Forms.CONTROL_TEXT == c.type) {
            drawText(c, holder);
        } else if (Forms.CONTROL_INPUT == c.type) {
            drawText(c, holder);
            holder.editText.setVisibility(EditText.VISIBLE);
            holder.editText.setText(c.text);
            holder.editText.addTextChangedListener(new TextWatcher() {

                public void afterTextChanged(Editable s) {
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    c.text = s.toString();
                    Forms.getInstance().controlUpdated(c);
                }
            });
        } else if (Forms.CONTROL_CHECKBOX == c.type) {
            holder.checkBox.setVisibility(CheckBox.VISIBLE);
            holder.checkBox.setText(c.description);
            holder.checkBox.setChecked(c.selected);
            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    c.selected = !c.selected;
                    Forms.getInstance().controlUpdated(c);
                }
            });
        } else if (Forms.CONTROL_SELECT == c.type) {
            drawText(c, holder);
            holder.spinner.setVisibility(Spinner.VISIBLE);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, c.items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinner.setAdapter(adapter);
            holder.spinner.setPrompt(c.description);
            holder.spinner.setSelection(c.current);
            holder.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    c.current = position;
                    Forms.getInstance().controlUpdated(c);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        } else if (Forms.CONTROL_GAUGE == c.type) {
            drawText(c, holder);
            holder.seekBar.setVisibility(SeekBar.VISIBLE);
            holder.seekBar.setProgress(c.level);
            holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        } else if (Forms.CONTROL_IMAGE == c.type) {
            drawText(c, holder);
            holder.imageView.setVisibility(ImageView.VISIBLE);
            holder.imageView.setImageBitmap(c.image);
        } else if (Forms.CONTROL_LINK == c.type) {
            drawText(c, holder);
        }
        return ll;
    }

    private void drawText(Forms.Control c, ViewHolder holder) {
        if (c.label != null) {
            holder.labelView.setVisibility(TextView.VISIBLE);
            holder.labelView.setText(c.label);
        }
        if (c.description != null) {
            holder.descView.setVisibility(TextView.VISIBLE);
            holder.descView.setText(c.description);
        }
    }

    private static class ViewHolder {
        ImageView imageView;
        TextView textView;
        TextView descView;
        TextView labelView;
        CheckBox checkBox;
        Spinner spinner;
        SeekBar seekBar;
        EditText editText;
    }
}
