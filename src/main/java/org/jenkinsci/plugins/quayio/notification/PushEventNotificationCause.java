/*
 *     Copyright 2016 Jean-Christophe Sirot <sirot@chelonix.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.quayio.notification;

import hudson.Extension;
import hudson.model.Cause;
import org.jenkinsci.plugins.buildtriggerbadge.provider.BuildTriggerBadgeProvider;

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
        return String.format("Triggered by push of <a href=\"%s\">%s</a> to Quay.io",
                notification.getHomepage(), notification.getRepository());
    }

    @Extension
    public static class QuayIoTriggerBadgeProvider extends BuildTriggerBadgeProvider {
        @Override
        public String provideIcon(Cause cause) {
            if (cause instanceof PushEventNotificationCause) {
                return "/plugin/quayio-notification/images/quay.png";
            }
            return null;
        }
    }
}
