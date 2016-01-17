package org.jenkinsci.plugins.quayio.notification;

import hudson.Extension;
import hudson.model.*;
import hudson.model.Queue;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.interceptor.RespondSuccess;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by sirot on 17/01/2016.
 */
@Extension
public class QuayIoWebHook implements UnprotectedRootAction {

    public static final String PREFIX = "QUAY_IO_TRIGGER_";
    public static final String KEY_REPOSITORY = PREFIX + "REPOSITORY";

    /**
     * The namespace under Jenkins context path that this Action is bound to.
     */
    public static final String URL_NAME = "quayio-webhook";

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
        return URL_NAME;
    }

    @RequirePOST
    @RespondSuccess
    public void doNotify(StaplerRequest request, StaplerResponse response) throws IOException {
        PushEventNotification notification;
        String body = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
        try {
            JSONObject payload = JSONObject.fromObject(body);
            notification = new PushEventNotification(payload);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not parse the web hook payload!", e);
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
                        logger.log(Level.FINER, "job {0} doesn't have DockerHubTrigger set", p.getName());
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
        ParameterValue param = new StringParameterValue(KEY_REPOSITORY, notification.getRepository());
        //List<ParameterValue> parameters = getParameterValues(notification);
        List<ParameterValue> parameters = Arrays.asList(param);
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

        logger.info("Scheduled job " + job.asJob().getName() + " as Docker image " + notification.getRepository() + " has been pushed");
    }

//    private List<ParameterValue> getParameterValues(Job job, PushEventNotification notification) {
//        List<ParameterValue> parameters = new LinkedList<ParameterValue>();
//        if (job.isParameterized()) {
//            Collection<ParameterValue> defaults = getDefaultParametersValues(job);
//            for (ParameterValue value : defaults) {
//                if (!value.getName().equalsIgnoreCase(KEY_REPOSITORY) && !value.getName().equalsIgnoreCase(KEY_DOCKER_HUB_HOST)) {
//                    parameters.add(value);
//                }
//            }
//        }
//        parameters.add(new StringParameterValue(KEY_REPOSITORY, notification.getRepository()));
//        return parameters;
//    }

//    /**
//     * Direct copy from {@link ParameterizedJobMixIn#getDefaultParametersValues()} (version 1.580).
//     *
//     * @return the configured parameters with their default values.
//     */
//    private List<ParameterValue> getDefaultParametersValues(Job job) {
//        JobProperty paramDefProp = job.getProperty(ParametersDefinitionProperty.class);
//        ArrayList<ParameterValue> defValues = new ArrayList<ParameterValue>();
//
//        if (paramDefProp == null)
//            return defValues;
//
//        /* Scan for all parameter with an associated default values */
//        for (ParameterDefinition paramDefinition : ((ParametersDefinitionProperty)paramDefProp).getParameterDefinitions()) {
//            ParameterValue defaultValue = paramDefinition.getDefaultParameterValue();
//
//            if (defaultValue != null)
//                defValues.add(defaultValue);
//        }
//
//        return defValues;
//    }

    static class JobMixInWrapper<JobT extends Job<JobT, RunT> & ParameterizedJobMixIn.ParameterizedJob & Queue.Task, RunT extends Run<JobT, RunT> & Queue.Executable> extends ParameterizedJobMixIn<JobT, RunT> {

        private final JobT job;

        public JobMixInWrapper(JobT job) {
            this.job = job;
        }

        @Override
        protected JobT asJob() {
            return job;
        }
    }
}