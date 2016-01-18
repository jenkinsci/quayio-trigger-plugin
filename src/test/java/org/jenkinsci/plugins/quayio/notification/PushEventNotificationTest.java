package org.jenkinsci.plugins.quayio.notification;

import com.google.common.io.Resources;
import net.sf.json.JSON;
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
