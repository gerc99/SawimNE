package ru.sawim.view;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import ru.sawim.General;
import ru.sawim.R;
import sawim.cl.ContactList;
import sawim.comm.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 22.06.13
 * Time: 14:23
 * To change this template use File | Settings | File Templates.
 */
public class FileProgressView extends DialogFragment {

    private TextView captionText;
    private TextView descriptionText;
    private ProgressBar progressBar;
    private Button closeButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.file_progress_view, container, false);
        captionText = (TextView) v.findViewById(R.id.caption_text);
        descriptionText = (TextView) v.findViewById(R.id.description_text);
        progressBar = (ProgressBar) v.findViewById(R.id.myprogressbar);
        closeButton = (Button) v.findViewById(R.id.button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContactList.getInstance().removeTransfer(true);
            }
        });
        return v;
    }

    public void changeFileProgress(final int percent, final String caption, final String text) {
        final long time = General.getCurrentGmtTime();
        final String strTime = Util.getLocalDateString(time, true);
        Handler handler = new Handler(getActivity().getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                captionText.setText(caption + " " + strTime);
                descriptionText.setText(text);
                progressBar.setVisibility(ProgressBar.VISIBLE);
                Rect bounds = progressBar.getProgressDrawable().getBounds();
                progressBar.getProgressDrawable().setBounds(bounds);
                progressBar.setProgress(percent);
            }
        });

    }
}
