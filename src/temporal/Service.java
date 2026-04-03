package temporal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class Service {
    public static final String BASE_URL = "https://temporal.dedomil.workers.dev/api";

    public interface Callback {
        void onSuccess(String response);

        void onError(String error);
    }

    public static void post(final String path, final String json, final String jwt, final Callback callback) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = BASE_URL + path;
                    HttpConnection c = (HttpConnection) Connector.open(url);
                    c.setRequestMethod(HttpConnection.POST);
                    c.setRequestProperty("Content-Type", "application/json");
                    c.setRequestProperty("User-Agent", "j2me/1.0");
                    c.setRequestProperty("Accept", "*/*");
                    if (jwt != null) {
                        c.setRequestProperty("authorization", jwt);
                    }
                    if (json != null) {
                        byte[] data = json.getBytes("UTF-8");
                        c.setRequestProperty("Content-Length", String.valueOf(data.length));
                        OutputStream os = c.openOutputStream();
                        os.write(data);
                        os.flush();
                        os.close();
                    }
                    processResponse(c, callback);
                } catch (Exception e) {
                    if (callback != null)
                        callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public static void get(final String path, final String jwt, final Callback callback) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = BASE_URL + path;
                    HttpConnection c = (HttpConnection) Connector.open(url);
                    c.setRequestMethod(HttpConnection.GET);
                    c.setRequestProperty("User-Agent", "j2me/1.0");
                    c.setRequestProperty("Accept", "*/*");
                    if (jwt != null) {
                        c.setRequestProperty("authorization", jwt);
                    }
                    processResponse(c, callback);
                } catch (Exception e) {
                    if (callback != null)
                        callback.onError(e.getMessage());
                }
            }
        }).start();
    }

    private static void processResponse(HttpConnection c, Callback callback) throws IOException {
        int code = c.getResponseCode();
        long len = c.getLength();
        InputStream is = c.openInputStream();
        String response = readFully(is);
        is.close();
        c.close();
        if (code >= 200 && code < 300) {
            if (callback != null)
                callback.onSuccess(response);
        } else {
            if (callback != null)
                callback.onError(response);
        }
    }

    public static String readFully(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        return new String(bos.toByteArray(), "UTF-8");
    }

    public static String buildJson(String[] keys, String[] values) {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        for (int i = 0; i < keys.length; i++) {
            sb.append("\"").append(keys[i]).append("\":\"").append(escape(values[i])).append("\"");
            if (i < keys.length - 1) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null)
            return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"')
                sb.append("\\\"");
            else if (c == '\\')
                sb.append("\\\\");
            else if (c == '\b')
                sb.append("\\b");
            else if (c == '\f')
                sb.append("\\f");
            else if (c == '\n')
                sb.append("\\n");
            else if (c == '\r')
                sb.append("\\r");
            else if (c == '\t')
                sb.append("\\t");
            else
                sb.append(c);
        }
        return sb.toString();
    }
}
