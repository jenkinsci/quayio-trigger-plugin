package org.jenkinsci.plugins.quayio.notification;

import net.sf.json.JSONObject;

/**
 * Created by sirot on 17/01/2016.
 */
public class PushEventNotification {

    private final JSONObject payload;
    private final String repository;
    private final long received;

    public PushEventNotification(JSONObject payload) {
        this.payload = payload;
        this.repository = payload.getString("repository");
        this.received = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "PushEventNotification{" +
                "received=" + received +
                ", repository='" + repository + '\'' +
                '}';
    }

    public String getRepository() {
        return repository;
    }
}
