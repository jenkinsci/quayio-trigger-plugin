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
import hudson.model.*;
import hudson.model.Queue;
import hudson.security.ACL;
import hudson.security.csrf.CrumbExclusion;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.interceptor.RespondSuccess;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by sirot on 17/01/2016.
 */
@Extension
public class QuayIoWebHook extends CrumbExclusion implements UnprotectedRootAction {

    /**
     * The namespace under Jenkins context path that this Action is bound to.
     */
    public static final String URL_PATH = "quayio-webhook";

    private static final int MIN_QUIET = 3;

    private static final Logger logger = Logger.getLogger(QuayIoWebHook.class.getName());

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Quay.io web hook";
    }

    @Override
    public String getUrlName() {
        return URL_PATH;
    }

    @RequirePOST
    @RespondSuccess
    public void doPush(StaplerRequest request, StaplerResponse response) throws IOException {
        PushEventNotification notification;
        String body = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
        try {
            JSONObject payload = JSONObject.fromObject(body);
            notification = new PushEventNotification(payload);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not parse the web hook payload!", e);
            logger.log(Level.FINER, "Payload content: " + body);
            return;
        }
        if (notification != null) {
            trigger(response, notification);
        }
    }

    private void trigger(StaplerResponse response, final PushEventNotification notification) throws IOException {
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return;
        }
        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            @Override
            public void run() {
                // search all jobs for DockerHubTrigger
                for (ParameterizedJobMixIn.ParameterizedJob p : jenkins.getAllItems(ParameterizedJobMixIn.ParameterizedJob.class)) {
                    QuayIoTrigger trigger = QuayIoTrigger.getTrigger(p);
                    if (trigger == null) {
                        logger.log(Level.FINER, "job {0} doesn't have QuayIoTrigger set", p.getName());
                        continue;
                    }
                    logger.log(Level.FINER, "Inspecting candidate job {0}", p.getName());
                    if (trigger.getRepositories().contains(notification.getRepository())) {
                        schedule(new JobMixInWrapper((Job)p), notification);
                    }
                }
            }
        });
    }

    private void schedule(final JobMixInWrapper<?, ?> job, final PushEventNotification notification) {
        if (!job.asJob().isBuildable()) {
            return;
        }
        //ParameterValue param = new StringParameterValue(KEY_REPOSITORY, notification.getRepository());
        List<ParameterValue> parameters = job.getParameterValues(notification);
        //List<ParameterValue> parameters = Arrays.asList(params);
        List<Action> queueActions = new LinkedList<Action>();

        queueActions.add(new ParametersAction(parameters));
        queueActions.add(new CauseAction(new PushEventNotificationCause(notification)));

        int quiet = Math.max(MIN_QUIET, job.asJob().getQuietPeriod());

        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            logger.log(Level.WARNING, "Tried to schedule a build while Jenkins was gone.");
            return;
        }
        job.scheduleBuild2(quiet, queueActions.toArray(new Action[2]));

        logger.info("Scheduled job " + job.asJob().getName() + " as Docker image " + notification.getRepository() + " has been pushed with tags " + StringUtils.join(notification.getTags(), ", "));
    }

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/" + URL_PATH)) {
            chain.doFilter(request, response);
            return true;
        }
        return false;
    }


    static class JobMixInWrapper<JobT extends Job<JobT, RunT> & ParameterizedJobMixIn.ParameterizedJob & Queue.Task, RunT extends Run<JobT, RunT> & Queue.Executable> extends ParameterizedJobMixIn<JobT, RunT> {

        public static final String PREFIX = "QUAY_IO_TRIGGER_";
        public static final String KEY_REPOSITORY = PREFIX + "REPOSITORY";
        public static final String KEY_TAG = PREFIX + "TAG";

        private final JobT job;

        public JobMixInWrapper(JobT job) {
            this.job = job;
        }

        @Override
        protected JobT asJob() {
            return job;
        }

        public List<ParameterValue> getParameterValues(PushEventNotification notification) {
            List<ParameterValue> parameters = new LinkedList<ParameterValue>();
            if (isParameterized()) {
                Collection<ParameterValue> defaults = getDefaultParametersValues();
                for (ParameterValue value : defaults) {
                    if (!value.getName().equalsIgnoreCase(KEY_REPOSITORY) && !value.getName().equalsIgnoreCase(KEY_TAG)) {
                        parameters.add(value);
                    }
                }
            }
            parameters.add(new StringParameterValue(KEY_REPOSITORY, notification.getRepository()));
            if (!notification.getTags().isEmpty()) {
                parameters.add(new StringParameterValue(KEY_TAG, notification.getTags().get(0)));
            }
            return parameters;
        }

        public List<ParameterValue> getDefaultParametersValues() {
            ParametersDefinitionProperty paramDefProp = asJob().getProperty(ParametersDefinitionProperty.class);
            ArrayList<ParameterValue> defValues = new ArrayList<ParameterValue>();

            if (paramDefProp == null)
                return defValues;

            /* Scan for all parameter with an associated default values */
            for (ParameterDefinition paramDefinition : ((ParametersDefinitionProperty)paramDefProp).getParameterDefinitions()) {
                ParameterValue defaultValue = paramDefinition.getDefaultParameterValue();

                if (defaultValue != null)
                    defValues.add(defaultValue);
            }

            return defValues;
        }

    }
}