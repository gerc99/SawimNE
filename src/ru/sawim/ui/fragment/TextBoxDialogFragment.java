package ru.sawim.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import ru.sawim.R;
import ru.sawim.comm.JLocale;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 10.06.13
 * Time: 18:25
 * To change this template use File | Settings | File Templates.
 */
public class TextBoxDialogFragment extends DialogFragment {
    private EditText editText;
    private String caption = null;
    private String text;
    private TextBoxListener textBoxListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        View dialogView = View.inflate(context, R.layout.text_box_view, null);
        editText = (EditText) dialogView.findViewById(R.id.editText);
        editText.setText(text);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        if (caption == null) {
            super.onCreateDialog(savedInstanceState).requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else {
            dialogBuilder.setTitle(caption);
        }
        dialogBuilder.setView(dialogView);
        dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                textBoxListener.textboxAction(TextBoxDialogFragment.this, true);
            }
        });
        dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                textBoxListener.textboxAction(TextBoxDialogFragment.this, false);
            }
        });

        AlertDialog dialog = dialogBuilder.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setCaption(int resId) {
        this.caption = JLocale.getString(resId);
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

    public interface TextBoxListener {
        void textboxAction(TextBoxDialogFragment box, boolean ok);
    }
}
