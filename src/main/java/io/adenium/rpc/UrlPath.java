package io.adenium.rpc;

import io.adenium.utils.VoidCallable;

public class UrlPath {
    private String                      path;
    private UrlPath[]                   paths;
    private VoidCallable<Messenger> onGET;

    public UrlPath(String path, VoidCallable<Messenger> onGET, UrlPath paths[]) {
        this.path   = path;
        this.paths  = paths;
        this.onGET  = onGET;
    }

    public void get(Messenger messenger) {
        onGET.call(messenger);
    }

    public String getPath() {
        return path;
    }

    public UrlPath[] getPaths() {
        return paths;
    }

    public VoidCallable<Messenger> getOnGET() {
        return onGET;
    }
}
