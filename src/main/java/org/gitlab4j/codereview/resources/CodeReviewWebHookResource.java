
package org.gitlab4j.codereview.resources;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.webhook.Event;
import org.gitlab4j.api.webhook.IssueEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.WebHookManager;

/**
 * CodeReviewWebHookResource
 * 
 * This class provides endpoint for GitLab WebHook callouts.
 * 
 * @author greg@messners.com
 */
@Path("/webhook")
public class CodeReviewWebHookResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response processEvent(Event event) {

        try {
            getWebHookManager().handleEvent(event);
            return Response.ok().entity("Processed " + event.getObjectKind() + " event").type(MediaType.TEXT_PLAIN).build();
        } catch (GitLabApiException gae) {
            String errorMessage = gae.getMessage();
            return (Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage).type(MediaType.TEXT_PLAIN).build());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/issue")
    public Response issueEventHandler(IssueEvent event) {
        return (processEvent(event));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/merge-request")
    public Response mergeRequestEventHandler(MergeRequestEvent event) {
        return (processEvent(event));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/push")
    public Response pushEventHandler(PushEvent event) {
        return (processEvent(event));
    }

    @Context
    ServletContext servletContext;

    private WebHookManager getWebHookManager() {
        return ((WebHookManager) servletContext.getAttribute(WebHookManager.class.getSimpleName()));
    }
}
