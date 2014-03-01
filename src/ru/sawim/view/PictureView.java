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
import android.widget.*;
import ru.sawim.R;
import ru.sawim.view.tasks.HtmlTask;

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
    private WebView webView;
    private ProgressBar progressBar;
    private HtmlTask htmlTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.picture_view, container, false);

        final RelativeLayout root = new RelativeLayout(getActivity());
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setContentView(root);
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

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

        link = HtmlTask.parseDb(link);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        htmlTask = new HtmlTask(getActivity(), link, webView, progressBar);
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

    @Override
    public void onResume() {
        super.onResume();
        if (htmlTask != null && htmlTask.isHide() && isVisible()) {
            dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        htmlTask = null;
    }
}