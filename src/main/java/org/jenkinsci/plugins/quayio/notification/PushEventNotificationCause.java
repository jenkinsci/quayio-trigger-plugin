package org.jenkinsci.plugins.quayio.notification;

import hudson.model.Cause;
import net.sf.json.JSONObject;

/**
 * Created by sirot on 17/01/2016.
 */
public class PushEventNotificationCause extends Cause {

    private final PushEventNotification notification;

    public PushEventNotificationCause(PushEventNotification notification) {
        this.notification = notification;
    }

    public PushEventNotification getNotification() {
        return notification;
    }

    @Override
    public String getShortDescription() {
        return String.format("Triggered by push of %s to Quay.io", notification.getRepository());
    }
}
