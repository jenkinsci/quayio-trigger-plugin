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
package org.jenkinsci.plugins.quayio.trigger;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by sirot on 17/01/2016.
 */
public class PushEventNotification {

    private final JSONObject payload;
    private final String repository;
    private final List<String> tags = new ArrayList<String>();
    private final String homepage;
    private final long received;

    public PushEventNotification(JSONObject payload) {
        this.payload = payload;
        this.homepage = payload.getString("homepage");
        this.repository = payload.getString("repository");
        Iterator<String> it;
        try {
            it = payload.getJSONObject("updated_tags").keys();
        } catch (JSONException je) {
            /* updated_tags is not an objet, it may be an array */
            it = payload.getJSONArray("updated_tags").iterator();
        }
        while (it.hasNext()) {
            String key = it.next();
            this.tags.add(key);
        }
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

    public List<String> getTags() {
        return tags;
    }

    public String getHomepage() {
        return homepage;
    }
}
