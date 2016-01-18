package org.jenkinsci.plugins.quayio.notification;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

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
        assertThat(trigger.getRepositories()).containsExactly("foo/bar", "qux/baz");
        project = (FreeStyleProject) j.configRoundtrip(project);
        trigger = QuayIoTrigger.getTrigger(project);
        assertThat(trigger).isNotNull();
        assertThat(trigger.getRepositories()).containsExactly("foo/bar", "qux/baz");
    }
}
