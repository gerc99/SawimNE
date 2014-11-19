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
import android.widget.RelativeLayout;
import android.widget.TextView;
import ru.sawim.R;
import ru.sawim.view.tasks.HtmlTask;
import ru.sawim.widget.Util;

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
    private HtmlTask htmlTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.picture_view, container, false);

        final RelativeLayout root = new RelativeLayout(getActivity());
        root.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setContentView(root);
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        TextView textView = (TextView) v.findViewById(R.id.textView);
        textView.setText(link);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
        final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
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

        link = HtmlTask.parseDropBox(link);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        htmlTask = new HtmlTask(getActivity(), link, webView, progressBar) {
            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                int newProgress = values[0];
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    webView.setVisibility(ImageView.VISIBLE);
                    progressBar.setVisibility(ProgressBar.GONE);
                } else {
                    webView.setVisibility(ImageView.GONE);
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }
            }
        };
        htmlTask.execute(link);
        webView.setWebChromeClient(new WebChromeClient() );

        v.setBackgroundColor(Util.getSystemBackground(getActivity().getApplicationContext()));
        root.setBackgroundColor(Util.getSystemBackground(getActivity().getApplicationContext()));
        webView.setBackgroundColor(Util.getSystemBackground(getActivity().getApplicationContext()));
        return v;
    }

    public void setLink(String link) {
        this.link = link;
    }

    private void close() {
        if (htmlTask != null && htmlTask.isHide() && isVisible()) {
            dismiss();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        close();
    }

    @Override
    public void onDestroy() {
        if (htmlTask != null) {
            htmlTask.cancel(false);
            htmlTask = null;
        }
        super.onDestroy();
        if (webView != null) {
            webView.destroyDrawingCache();
            webView.stopLoading();
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearFormData();
            webView.destroy();
        }
    }
}