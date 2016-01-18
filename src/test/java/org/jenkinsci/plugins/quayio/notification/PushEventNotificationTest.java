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

import com.google.common.io.Resources;
import net.sf.json.JSONObject;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jcsirot on 18/01/16.
 */
public class PushEventNotificationTest {

    @Test
    public void shouldHandlePushEventWithTagMap() throws Exception {
        // Given
        JSONObject payload = JSONObject.fromObject(Resources.toString(Resources.getResource("sample1.json"), Charset.forName("UTF-8")));
        // When
        PushEventNotification notification = new PushEventNotification(payload);
        // Then
        assertThat(notification.getRepository()).isEqualTo("mynamespace/repository");
        assertThat(notification.getHomepage()).isEqualTo("https://quay.io/repository/mynamespace/repository");
        assertThat(notification.getTags()).containsExactly("latest");
    }

    @Test
    public void shouldHandlePushEventWithTagArray() throws Exception {
        // Given
        JSONObject payload = JSONObject.fromObject(Resources.toString(Resources.getResource("sample2.json"), Charset.forName("UTF-8")));
        // When
        PushEventNotification notification = new PushEventNotification(payload);
        // Then
        assertThat(notification.getRepository()).isEqualTo("mynamespace/repository");
        assertThat(notification.getHomepage()).isEqualTo("https://quay.io/repository/mynamespace/repository");
        assertThat(notification.getTags()).containsExactly("latest", "v1.7");
    }
}
