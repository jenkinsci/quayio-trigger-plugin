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
