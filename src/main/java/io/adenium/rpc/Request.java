package io.adenium.rpc;

import com.sun.net.httpserver.HttpExchange;
import io.adenium.utils.VoidCallableThrowsT;

import java.io.IOException;

public class Request {
    private String  patternMatcher;
    private String  patternURL;
    private boolean mustMatch;
    private VoidCallableThrowsT<Messenger, IOException> function;

    public Request(String pattern, boolean mustMatch, VoidCallableThrowsT<Messenger, IOException> function) {
        this.patternMatcher = pattern;
        this.patternURL = Messenger.requestURL(pattern);
        this.mustMatch = mustMatch;
        this.function = function;
    }

    public boolean submit(String url) {
        boolean isMatch = false;

        if (mustMatch) {
            isMatch = patternURL.equals(url);
        } else {
            isMatch = url.startsWith(patternURL);
        }

        return isMatch;
    }

    public void call(HttpExchange exchange, String url, String query) throws IOException {
        function.call(new Messenger(exchange, url, query, patternMatcher));
    }
}
