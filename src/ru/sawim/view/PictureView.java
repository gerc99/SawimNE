package ru.sawim.view;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.*;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import ru.sawim.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 26.09.13
 * Time: 19:03
 * To change this template use File | Settings | File Templates.
 */
public class PictureView extends DialogFragment {

    public static final String TAG = PictureView.class.getSimpleName();
    private String link;
    private boolean hide = false;
    private WebView webView;
    private ProgressBar progressBar;

    @Override
    public void onResume() {
        super.onResume();
        if (hide && isVisible()) {
            dismiss();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.picture_view, container, false);

        Display display = getDialog().getWindow().getWindowManager().getDefaultDisplay();
        final float scale = getResources().getDisplayMetrics().density;
        float[] DIMENSIONS_LANDSCAPE = {20, 60};
        float[] DIMENSIONS_PORTRAIT = {40, 60};
        float[] dimensions = (display.getWidth() < display.getHeight()) ? DIMENSIONS_PORTRAIT : DIMENSIONS_LANDSCAPE;
        v.setLayoutParams(new FrameLayout.LayoutParams(
                display.getWidth() - (int) (dimensions[0] * scale + 0.5f),
                display.getHeight() - (int) (dimensions[1] * scale + 0.5f)));

        TextView textView = (TextView) v.findViewById(R.id.textView);
        textView.setText(link);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        webView = (WebView) v.findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setUseWideViewPort(true);
        webView.setInitialScale(1);
        settings.setLoadsImagesAutomatically(true);
        settings.setLightTouchEnabled(true);
        //settings.setJavaScriptCanOpenWindowsAutomatically(true);
        //settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        if (-1 != link.indexOf("https://www.dropbox.com")) {
            link = link.replace("https://www.dropbox", "http://dl.dropboxusercontent");
        }
        progressBar.setVisibility(ProgressBar.VISIBLE);
        HtmlTask htmlTask = new HtmlTask();
        htmlTask.execute(link);
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    view.setVisibility(ImageView.VISIBLE);
                    progressBar.setVisibility(ProgressBar.GONE);
                } else {
                    view.setVisibility(ImageView.GONE);
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }
            }
        });
        return v;
    }

    public void setLink(String link) {
        this.link = link;
    }

    class HtmlTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... urls) {
            String[] result = {null, null};
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                result[1] = urlConnection.getContentType();
                if (result[1].startsWith("image/")) {
                    return result;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    total.append(line);
                }
                result[0] = total.toString();
                if (link.startsWith("http://pic4u.ru/")) {
                    int start = result[0].indexOf("<div class='img-preview'>") + 36;
                    if (start == -1) {
                        result[0] = null;
                        return result;
                    }
                    int end = result[0].indexOf("'><img src='", start);
                    result[0] = result[0].substring(start, end);
                }
                result[0] = url.getProtocol() + "://" + url.getHost() + result[0];
            } catch (Exception e) {
                e.printStackTrace();
                result[0] = null;
                return result;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String[] s) {
            progressBar.setVisibility(ProgressBar.GONE);

            String html1 = "<html><head><meta charset=\"utf-8\"><style>.block{max-width:100%;}" +
                    "body {margin: 0}</style></head><body><img class=\"block\" src=\"";
            String html2 = "\"></body></html>";
            try {
                if (link.startsWith("http://pic4u.ru/") && s[0] != null) {
                    webView.loadDataWithBaseURL(null, html1 + s[0] + html2, "text/html", "en_US", null);
                } else if (link.startsWith("http://pic4u.ru/") && !s[1].startsWith("image/") || !s[1].startsWith("image/")) {
                    hide = true;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(link));
                    startActivity(i);
                } else {
                    webView.loadDataWithBaseURL(null, html1 + link + html2, "text/html", "en_US", null);
                }
            } catch (Exception e) {
                hide = true;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(link));
                startActivity(i);
            }
        }
    }
}