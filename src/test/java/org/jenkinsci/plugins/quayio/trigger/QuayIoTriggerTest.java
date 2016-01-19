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

import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jcsirot on 18/01/16.
 */
public class QuayIoTriggerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void shouldHandleConfiguration() throws Exception {
        Set<String> repositories = new HashSet<String>();
        repositories.add("foo/bar");
        repositories.add("qux/baz");
        FreeStyleProject project = j.createFreeStyleProject();
        project.addTrigger(new QuayIoTrigger(repositories));
        QuayIoTrigger trigger = QuayIoTrigger.getTrigger(project);
        assertThat(trigger).isNotNull();
        assertThat(trigger.getRepositories()).containsOnlyElementsOf(Arrays.asList("foo/bar", "qux/baz"));
        project = (FreeStyleProject) j.configRoundtrip(project);
        trigger = QuayIoTrigger.getTrigger(project);
        assertThat(trigger).isNotNull();
        assertThat(trigger.getRepositories()).containsOnlyElementsOf(Arrays.asList("foo/bar", "qux/baz"));
    }
}
