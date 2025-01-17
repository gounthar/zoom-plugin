package io.jenkins.plugins.zoom.workflow;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.zoom.MessageBuilder;
import io.jenkins.plugins.zoom.ZoomNotifyClient;
import jenkins.model.Jenkins;
import lombok.extern.slf4j.Slf4j;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.util.Set;

@Slf4j
public class ZoomSendStep extends Step {

    private String webhookUrl;
    private Secret authToken;
    private boolean jenkinsProxyUsed;
    private String message;

    @DataBoundConstructor
    public ZoomSendStep() {
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new ZoomSendStepExecution(this, stepContext);
    }


    private static class ZoomSendStepExecution extends SynchronousNonBlockingStepExecution {

        private static final long serialVersionUID = 1L;
        private transient final ZoomSendStep step;

        protected ZoomSendStepExecution(ZoomSendStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        //do the work of the step
        @Override
        protected Object run() throws Exception {
            Run run = getContext().get(Run.class);
            log.info("Call sendMessage: {}", run.getFullDisplayName());
            TaskListener listener = getContext().get(TaskListener.class);
            MessageBuilder messageBuilder = new MessageBuilder(null, run, listener);
            String msg = messageBuilder.buildPipeMsg(this.step.getMessage());
            ZoomNotifyClient.notify(this.step.getWebhookUrl(), this.step.getAuthToken(), this.step.isJenkinsProxyUsed(), msg);
            return null;
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "zoomSend";
        }

        @Override
        public String getDisplayName() {
            return "zoomSend";
        }

        @POST
        public FormValidation doTestConnection(@QueryParameter("webhookUrl") final String webhookUrl,
                                               @QueryParameter("authToken") final String authToken,
                                               @QueryParameter("jenkinsProxyUsed") final boolean jenkinsProxyUsed){
            Jenkins.get().checkPermission(Permission.CONFIGURE);
            if(ZoomNotifyClient.notify(webhookUrl, authToken, jenkinsProxyUsed, null)){
                return FormValidation.ok("Connection is ok");
            }
            return FormValidation.error("Connect failed");
        }
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    @DataBoundSetter
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Secret getAuthToken() {
        return authToken;
    }

    @DataBoundSetter
    public void setAuthToken(String authToken) {
        this.authToken = Secret.fromString(authToken);
    }

    public boolean isJenkinsProxyUsed() {
        return jenkinsProxyUsed;
    }

    @DataBoundSetter
    public void setJenkinsProxyUsed(boolean jenkinsProxyUsed) {
        this.jenkinsProxyUsed = jenkinsProxyUsed;
    }

    public String getMessage() {
        return message;
    }

    @DataBoundSetter
    public void setMessage(String message) {
        this.message = message;
    }
}
