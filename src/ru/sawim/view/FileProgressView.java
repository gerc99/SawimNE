package ru.sawim.view;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import sawim.comm.Util;
import sawim.roster.RosterHelper;

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
                RosterHelper.getInstance().removeTransfer(true);
                FileProgressView.this.dismiss();
            }
        });
        return v;
    }

    public void showProgress() {
        FragmentTransaction transaction = SawimApplication.getCurrentActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(this, "file_progress");
        transaction.commit();
    }

    public void changeFileProgress(final int percent, final String caption, final String text) {
        if (SawimApplication.getCurrentActivity() == null) return;
        if (percent == 100) {
            RosterHelper.getInstance().removeTransfer(true);
            dismiss();
        }
        final String strTime = Util.getLocalDateString(SawimApplication.getCurrentGmtTime(), true);
        Handler handler = new Handler(SawimApplication.getCurrentActivity().getMainLooper());
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
