package protocol.vk.api;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class VkDialog extends Dialog {
    public static final float[] DIMENSIONS_LANDSCAPE = {20, 60};
    public static final float[] DIMENSIONS_PORTRAIT = {40, 60};
    static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT);

    private String mUrl;
    private VkApp.VkDialogListener mListener;
    private ProgressDialog mSpinner;
    private WebView mWebView;
    private LinearLayout mContent;

    public VkDialog(Context context, String url, VkApp.VkDialogListener listener) {
        super(context);
        mUrl = url;
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner = new ProgressDialog(getContext());

        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("Loading...");

        mContent = new LinearLayout(getContext());

        mContent.setOrientation(LinearLayout.VERTICAL);

        setUpWebView();

        Display display = getWindow().getWindowManager().getDefaultDisplay();
        final float scale = getContext().getResources().getDisplayMetrics().density;
        float[] dimensions = (display.getWidth() < display.getHeight()) ? DIMENSIONS_PORTRAIT : DIMENSIONS_LANDSCAPE;

        addContentView(mContent, new FrameLayout.LayoutParams(
                display.getWidth() - (int) (dimensions[0] * scale + 0.5f),
                display.getHeight() - (int) (dimensions[1] * scale + 0.5f)));
    }

    private void setUpWebView() {
        mWebView = new WebView(getContext());

        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new VkWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(mUrl);
        mWebView.setLayoutParams(FILL);

        mContent.addView(mWebView);
    }

    private class VkWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //Log.d(Constants.DEBUG_TAG, "Redirecting URL " + url);
            ru.sawim.modules.DebugLog.println("redirect to " + url);

            if (url.startsWith(VkApp.CALLBACK_URL) & (!url.contains("error"))) {
                //Log.d(Constants.DEBUG_TAG,"url contains callback url");

                mListener.onComplete(url);
                VkDialog.this.dismiss();

                return true;
            } else if (url.contains("error")) {
                VkDialog.this.dismiss();
                return false;
            } else {
                //Log.d(Constants.DEBUG_TAG,"url not contains callback url");
                view.loadUrl(url);
                return true;
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            //Log.d(Constants.DEBUG_TAG, "Page error: " + description);
            super.onReceivedError(view, errorCode, description, failingUrl);

            mListener.onError(description);

            VkDialog.this.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            //Log.d(Constants.DEBUG_TAG, "Loading URL: " + url);
            super.onPageStarted(view, url, favicon);

            if (url.contains("error")) {
                VkDialog.this.dismiss();
                return;
            } else if (url.contains("access_token")) {
                VkDialog.this.dismiss();
                mListener.onComplete(url);
                return;
            }
            mSpinner.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mSpinner.dismiss();
        }
    }
}