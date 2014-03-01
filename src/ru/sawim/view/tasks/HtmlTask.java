package ru.sawim.view.tasks;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.webkit.WebView;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by admin on 01.03.14.
 */
public class HtmlTask extends AsyncTask<String, Void, String[]> {

    public static final String TAG = HtmlTask.class.getSimpleName();

    public static final String PIK4U = "http://pic4u.ru/";

    private FragmentActivity activity;
    private boolean hide = false;
    private WebView webView;
    private String link;
    private ProgressBar progressBar;

    public HtmlTask(FragmentActivity activity, String link, WebView webView, ProgressBar progressBar) {
        this.activity = activity;
        this.link = link;
        this.webView = webView;
        this.progressBar = progressBar;
    }

    @Override
    protected String[] doInBackground(String... urls) {
        String[] result = {null, null};
        try {
            URL url = new URL(urls[0]);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setInstanceFollowRedirects(false);
            String newUrl = urlConnection.getHeaderField("Location");
            if (newUrl != null) {
                newUrl = parseDb(newUrl);
                link = newUrl;
                urlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
            }

            result[1] = urlConnection.getContentType();
            if (isImage(result[1])) {
                return result;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                total.append(line);
            }
            result[0] = total.toString();
            if (link.startsWith(PIK4U)) {
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

        String html1 = "<html><head><meta charset=\"utf-8\"><style>.block{width:100%;}" +
                "body {margin: 0}</style></head><body><img class=\"block\" src=\"";
        String html2 = "\"></body></html>";
        try {
            if (link.startsWith(PIK4U) && s[0] != null) {
                webView.loadDataWithBaseURL(null, html1 + s[0] + html2, "text/html", "en_US", null);
            } else if (link.startsWith(PIK4U) && !isImage(s[1]) || !isImage(s[1])) {
                hide = true;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(link));
                activity.startActivity(i);
            } else {
                webView.loadDataWithBaseURL(null, html1 + link + html2, "text/html", "en_US", null);
            }
        } catch (Exception e) {
            hide = true;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(link));
            activity.startActivity(i);
        }
    }

    public static String parseDb(String link) {
        if (-1 != link.indexOf("https://www.dropbox.com")) {
            link = link.replace("https://www.dropbox", "http://dl.dropboxusercontent");
        }
        return link;
    }

    private boolean isImage(String link) {
        return link.startsWith("image/");
    }

    public boolean isHide() {
        return hide;
    }
}
