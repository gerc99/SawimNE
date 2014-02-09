package ru.sawim.view;

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
    private WebView webView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = inflater.inflate(R.layout.picture_view, container, false);
        TextView textView = (TextView) v.findViewById(R.id.textView);
        textView.setText(link);
        final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.progressBar);

        webView = (WebView) v.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        //webView.getSettings().setSupportZoom(true);
        //webView.getSettings().setBuiltInZoomControls(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
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
        webView.loadUrl(link);
        webView.setWebViewClient(new WebViewClient());
        return v;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webView != null) webView.destroy();
    }
}