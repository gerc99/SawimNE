package ru.sawim.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import sawim.forms.ManageContactListForm;
import sawim.modules.Emotions;
import sawim.ui.TextBoxListener;
import ru.sawim.R;
import ru.sawim.models.SmilesAdapter;
import ru.sawim.models.form.Forms;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 10.06.13
 * Time: 18:25
 * To change this template use File | Settings | File Templates.
 */
public class TextBoxView extends DialogFragment implements View.OnClickListener {

    private EditText editText;
    private Button okButton;
    private Button cancelButton;
    private String caption = null;
    private String text;
    private TextBoxListener textBoxListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (caption == null) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else {
            getDialog().setTitle(caption);
        }
        View v = inflater.inflate(R.layout.text_box_view, container, false);
        editText = (EditText) v.findViewById(R.id.editText);
        editText.setText(text);
        okButton = (Button) v.findViewById(R.id.buttonOk);
        okButton.setOnClickListener(this);
        cancelButton = (Button) v.findViewById(R.id.buttonCancel);
        cancelButton.setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View view) {
        if (view.equals(cancelButton)) {
            textBoxListener.textboxAction(this, false);
            dismiss();
        } else if (view.equals(okButton)) {
            textBoxListener.textboxAction(this, true);
            dismiss();
        }
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setString(String string) {
        if (string == null) {
            text = "";
        } else {
            text = string;
        }
    }

    public void setTextBoxListener(TextBoxListener textBoxListener) {
        this.textBoxListener = textBoxListener;
    }

    public String getString() {
        return editText.getText().toString();
    }

    public void back() {
        dismiss();
    }
}
