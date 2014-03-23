package protocol.vk.api;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import ru.sawim.modules.DebugLog;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 09.03.13 8:07
 *
 * @author vladimir
 */
public class WebClient {
    private HttpClient client = new DefaultHttpClient();
    private VkApp.VkDialogListener listener;

    public void oauth(String url, String email, String pass, VkApp.VkDialogListener _listener) {
        try {
            listener = _listener;
            FormParser fp = new FormParser();
            String page = request(url);
            int limit = 5;
            while (null != page) {
                HashMap<String, String> login = fp.process(page);
                login.put("email", email);
                login.put("pass", pass);
                page = request(login);
                limit--;
                if ((null != page) && (0 == limit)) {
                    DebugLog.println("vk page " + page);
                    listener.onError("");
                    break;
                }
            }
        } catch (IOException ignored) {
        }
    }

    private String request(String uri) {
        HttpGet request = new HttpGet(uri);
        try {
            HttpResponse response = client.execute(request);
            return EntityUtils.toString(response.getEntity());
        } catch (Exception ioex) {
            ru.sawim.modules.DebugLog.panic("uri" + uri, ioex);
            return null;
        }
    }

    private String request(HashMap<String, String> request) throws IOException {
        String url = request.get("@action");
        String method = request.get("@method").toUpperCase();
        request.remove("@action");
        request.remove("@method");
        if ("POST".equals(method)) {
            HttpPost post = new HttpPost(url);
            List<NameValuePair> nameValuePairs = new LinkedList<NameValuePair>();
            for (String key : request.keySet()) {
                nameValuePairs.add(new BasicNameValuePair(key, request.get(key)));
            }
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpContext context = new BasicHttpContext();
            HttpResponse response = client.execute(post, context);
            HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
            String resultUrl = currentReq.getURI().toString();
            if (-1 < resultUrl.indexOf("/blank.html")) {
                done(resultUrl);
                return null;
            }
            return EntityUtils.toString(response.getEntity());
        }
        throw new IOException("method do not supported");
    }

    private void done(String url) {
        if (url.contains("access_token")) {
            listener.onComplete(url);
        } else {
            listener.onError("");
        }
    }
}
