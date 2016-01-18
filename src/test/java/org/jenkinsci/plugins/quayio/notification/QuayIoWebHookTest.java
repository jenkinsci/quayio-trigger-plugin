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

import static org.assertj.core.api.Assertions.*;

import com.google.common.io.Resources;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockBuilder;
import org.jvnet.hudson.test.recipes.WithPlugin;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

public class QuayIoWebHookTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test(timeout = 60000)
    public void pushShouldTriggerBuild() throws Exception {
        // Given
        String json = Resources.toString(Resources.getResource("sample1.json"), Charset.forName("UTF-8"));
        FreeStyleProject project = j.createFreeStyleProject();
        final String repository = "mynamespace/repository";
        Set<String> set = new HashSet<String>();
        set.add(repository);
        project.addTrigger(new QuayIoTrigger(set));
        project.getBuildersList().add(new MockBuilder(Result.SUCCESS));

        // When
        Response r = RestAssured.given().contentType("application/json").body(json).post(j.getURL().toString() + "quayio-webhook/push");
        while(project.getBuilds().isEmpty()) {
            Thread.sleep(500);
        }

        // Then
        assertThat(r.getStatusCode()).isEqualTo(200);
        j.assertLogContains(repository, project.getLastBuild());
    }
}