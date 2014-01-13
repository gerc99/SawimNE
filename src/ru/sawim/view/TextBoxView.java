package ru.sawim.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import ru.sawim.R;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 10.06.13
 * Time: 18:25
 * To change this template use File | Settings | File Templates.
 */
public class TextBoxView extends DialogFragment {
    private EditText editText;
    private String caption = null;
    private String text;
    private TextBoxListener textBoxListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        View dialogView = LayoutInflater.from(context).inflate(R.layout.text_box_view, null);
        editText = (EditText) dialogView.findViewById(R.id.editText);
        editText.setText(text);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(R.string.ms_status_menu);
        dialogBuilder.setTitle(caption);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setInverseBackgroundForced(Util.isNeedToInverseDialogBackground());
        dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                textBoxListener.textboxAction(TextBoxView.this, true);
            }
        });
        dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                textBoxListener.textboxAction(TextBoxView.this, false);
            }
        });
        return dialogBuilder.create();
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setString(String string) {
        text = string == null ? "" : string;
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

    public static interface TextBoxListener {
        void textboxAction(TextBoxView box, boolean ok);
    }
}