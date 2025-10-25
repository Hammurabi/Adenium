package io.adenium.rpc;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.adenium.core.Context;
import org.json.JSONObject;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Messenger {
    private HttpExchange        exchange;
    private String              url;
    private String              query;
    private Map<String, String> regexMatches;

    public Messenger(HttpExchange exchange, String url, String query, String urlMatcher) {
        this.exchange   = exchange;
        this.url        = url;
        this.query      = query;

        regexMatches    = new HashMap<>();

        if (urlMatcher.contains(":")) {
            String surl[] = url.split("/");
            String regex[]= urlMatcher.split("/");

            for (int i = 0; i < regex.length; i ++) {
                if (regex[i].startsWith(":")) {
                    regexMatches.put(regex[i].substring(1), surl[i]);
                }
            }
        }
    }

    public static String requestURL(String url) {
        if (url.contains(":")) {
            return url.substring(0, url.indexOf(":"));
        }

        return url;
    }

    public void sendFile(String file) throws IOException {
        Pattern pattern = Pattern.compile("\\$\\{[A-z]+\\}");
        Matcher matcher = pattern.matcher(file);

        while (matcher.find()) {
            String query    = matcher.group(0);
            String result   = get(query.substring(2, query.length() - 1));
            if (result.isEmpty()) {
                throw new IOException();
            }

            file = matcher.replaceFirst(result);
            matcher = pattern.matcher(file);
        }

        if (file.endsWith("json")) {
            send("application/json", Context.getInstance().getResourceManager().get(file));
        } else if (file.endsWith("html")) {
            send("text/html", Context.getInstance().getResourceManager().get(file));
        } else if (file.endsWith("css")) {
            send("text/css", Context.getInstance().getResourceManager().get(file));
        } else if (file.endsWith("png")) {
            send("image/png", Context.getInstance().getResourceManager().get(file));
        } else if (file.endsWith("jpg") || file.endsWith("jpeg")) {
            send("image/jpeg", Context.getInstance().getResourceManager().get(file));
        } else if (file.endsWith("gif")) {
            send("image/gif", Context.getInstance().getResourceManager().get(file));
        } else if (file.endsWith("js")) {
            send("text/javascript", Context.getInstance().getResourceManager().get(file));
        }
    }

    public void send(String contentType, InputStream inputStream) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", contentType);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte buffer[] = new byte[4096];

        int read = 0;
        while ( (read = inputStream.read(buffer)) > 0 ) {
            outputStream.write(buffer, 0, read);
        }

        outputStream.close();
        send(contentType, outputStream.toByteArray());
    }

    public void send(String contentType, byte response[]) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.getResponseBody().flush();
        exchange.close();
    }

    public String getQuery() {
        return query;
    }

    public JSONObject getFormattedQuery() {
        return format(getQuery());
    }

    public static JSONObject format(String query) {
        String requests[] = query.split("&");
        JSONObject request = new JSONObject();
        for (int i = 0; i < requests.length; i ++) {
            String kv[] = requests[i].split("=");
            if (kv[1].toLowerCase().equals("true") || kv[1].toLowerCase().equals("false")) {
                request.put(kv[0], Boolean.parseBoolean(kv[1].toLowerCase()));
            } else if (kv[1].matches("\\d+")) {
                request.put(kv[0], Long.parseLong(kv[1]));
            } else {
                request.put(kv[0], kv[1]);
            }
        }

        return request;
    }

    public String getUrl() {
        return url;
    }

    public String get(String query) {
        if (regexMatches.containsKey(query)) {
            return regexMatches.get(query);
        }

        return "";
    }

    private static final String readUTF(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        StringBuilder builder = new StringBuilder();
        while (( line = reader.readLine() ) != null ) {
            builder.append(line).append("\n");
        }

        reader.close();
        return builder.toString();
    }

    public String getBodyUTF() throws IOException {
        return readUTF(exchange.getRequestBody());
    }

    public boolean isBinary() {
        return exchange.getRequestHeaders().get("Content-Type").get(0).equals("application/octet-stream");
    }

    public boolean isJson() {
        return exchange.getRequestHeaders().get("Content-Type").get(0).equals("application/json");
    }

    public byte[] getBodyBinary() throws IOException {
        byte buffer[] = new byte[1024];
        int read = 0;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        while ( (exchange.getRequestBody().read(buffer)) > 0 ) {
            outputStream.write(buffer, 0, read);
        }

        // this isn't necessary as of this moment.
        outputStream.close();
        return outputStream.toByteArray();
    }

    public String getContentType() {
        return exchange.getRequestHeaders().get("Content-Type").get(0);
    }
}
