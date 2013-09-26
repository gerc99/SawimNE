package ru.sawim.view;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import ru.sawim.R;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 26.09.13
 * Time: 19:03
 * To change this template use File | Settings | File Templates.
 */
public class PictureView extends DialogFragment {

    public static final String TAG = "PictureView";
    private String link;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setCanceledOnTouchOutside(true);
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.picture_view, container, false);
        TextView textView = (TextView) v.findViewById(R.id.textView);
        textView.setText(link);
        ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        ImageView imageView = (ImageView) v.findViewById(R.id.imageView);
        new DownloadImageTask(progressBar, imageView).execute(link);
        return v;
    }

    public void setLink(String link) {
        this.link = link;
    }

    class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ProgressBar progressBar;
        ImageView bmImage;

        public DownloadImageTask(ProgressBar progressBar, ImageView bmImage) {
            this.progressBar = progressBar;
            this.bmImage = bmImage;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            return Util.decodeAndResizeBitmap(urls[0]);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            bmImage.setVisibility(ImageView.GONE);
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            bmImage.setVisibility(ImageView.VISIBLE);
            progressBar.setVisibility(ProgressBar.GONE);
            bmImage.setImageBitmap(result);
        }
    }
}