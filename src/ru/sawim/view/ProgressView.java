package ru.sawim.view;

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
import ru.sawim.R;
import ru.sawim.activities.BaseActivity;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 22.06.13
 * Time: 14:23
 * To change this template use File | Settings | File Templates.
 */
public class ProgressView extends DialogFragment {

    private TextView captionText;
    private TextView descriptionText;
    private ProgressBar progressBar;
    private Button closeButton;
    OnProgressListener listener;

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
                listener.onClose();
            }
        });
        return v;
    }

    public void showProgress(OnProgressListener listener) {
        this.listener = listener;
        show(BaseActivity.getCurrentActivity().getSupportFragmentManager().beginTransaction(), "file_progress");
    }

    public void changeFileProgress(final int percent, final String caption, final String text) {
        if (BaseActivity.getCurrentActivity() == null || !isAdded()) return;
        Handler handler = new Handler(BaseActivity.getCurrentActivity().getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                captionText.setText(caption);
                descriptionText.setText(text);
                progressBar.setVisibility(ProgressBar.VISIBLE);
                progressBar.getProgressDrawable().setBounds(progressBar.getProgressDrawable().getBounds());
                progressBar.setProgress(percent);
            }
        });
    }

    public interface OnProgressListener {
        public void onClose();
    }
}
