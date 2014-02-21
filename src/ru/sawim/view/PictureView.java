package ru.sawim.view;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

    private static final String TAG = PictureView.class.getSimpleName();
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
        TextView textView = (TextView) v.findViewById(R.id.textView);
        textView.setText(link);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        webView = (WebView) v.findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setUseWideViewPort(true);
        webView.setInitialScale(1);
        settings.setLoadsImagesAutomatically(true);
        settings.setLightTouchEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        //settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        RetreiveHtmlTask htmlTask = new RetreiveHtmlTask();
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
        String html1 = "<html><head><meta charset=\"utf-8\"><style>.block{max-width:100%;}" +
                "body {margin: 0}</style></head><body><img class=\"block\" src=\"";
        String html2 = "\"></body></html>";
        webView.setWebViewClient(new WebViewClient());
        try {
            if (link.startsWith("http://pic4u.ru/") && htmlTask.get() != null) {
                webView.loadDataWithBaseURL(null, html1 + htmlTask.get() + html2, "text/html", "en_US", null);
            } else if (link.startsWith("http://pic4u.ru/")) {
                hide = true;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(link));
                startActivity(i);
            }
        } catch (Exception e) {
            //hide = true;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(link));
            startActivity(i);
        }
        return v;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }

    class RetreiveHtmlTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String result;
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    total.append(line);
                }
                result = total.toString();
                int start = result.indexOf("<div class='img-preview'>") + 36;
                if (start == -1) {
                    return null;
                }
                int end = result.indexOf("'><img src='", start);
                result = result.substring(start, end);
                result = url.getProtocol() + "://" + url.getHost() + result;
            } catch (Exception e) {
                return null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(ProgressBar.GONE);
        }
    }
}