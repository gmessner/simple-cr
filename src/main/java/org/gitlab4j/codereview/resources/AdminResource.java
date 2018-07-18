
package org.gitlab4j.codereview.resources;

import java.net.URI;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectHook;
import org.gitlab4j.codereview.CodeReviewConfiguration;
import org.gitlab4j.codereview.beans.AppResponse;
import org.gitlab4j.codereview.dao.ProjectConfig;
import org.gitlab4j.codereview.dao.ProjectConfig.MailToType;
import org.gitlab4j.codereview.dao.ProjectConfigDAO;
import org.gitlab4j.codereview.server.EmbeddedServer;
import org.gitlab4j.codereview.utils.StringUtils;
import org.jdbi.v3.core.Jdbi;

/**
 * AdminResource
 * 
 * This class provides an endpoint for GitLab-CR admin functionality, providing for the management of
 * the gitlab repository project being monitored.
 */
@Path("/admin")
public class AdminResource {

    private static Logger logger = LogManager.getLogger();

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest request;
    @Context
    UriInfo uriInfo;

    @GET
    @Path("/{groupName}/{projectName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("groupName") String groupName, @PathParam("projectName") String projectName) {

        checkAuthentication();
        logger.info("List code review setup for project, group=" + groupName + ", project=" + projectName);

        GitLabApi gitlabApi = (GitLabApi) servletContext.getAttribute(EmbeddedServer.GITLAB_API);

        // Get the specified project
        Project project;
        try {
            project = getProject(gitlabApi, groupName, projectName);
        } catch (ResponseException re) {
            logger.error("Problem getting project info, error=" + re.getMessage());
            return (AppResponse.getMessageResponse(false, "Could not load project info from GitLab server"));
        }

        // Load the Simple-CR project config
        int projectId = project.getId();
        ProjectConfigDAO dao = getProjectConfigDAO();
        ProjectConfig projectConfig = dao.find(projectId);
        if (projectConfig == null) {
            return (AppResponse.getMessageResponse(true, "Project not configured in Simple-CR."));
        }
        
        return (AppResponse.getDataResponse(true, projectConfig));
    }

    @POST
    @Path("/{groupName}/{projectName}")
    public Response add(@PathParam("groupName") String groupName, @PathParam("projectName") String projectName,
            @DefaultValue("true") @FormParam("enabled") boolean enabled, @FormParam("branch_regex") String branchRegex,
            @DefaultValue("project") @FormParam("mail_to") String mailTo, @FormParam("additional_mail_to") String additionalMailTo,
            @FormParam("exclude_mail_to") String excludeMailTo,
            @DefaultValue("false") @FormParam("include_default_mail_to") boolean includeDefaultMailTo) {

        checkAuthentication();
        logger.info("Add code review setup for project, group=" + groupName + ", project=" + projectName);

        GitLabApi gitlabApi = (GitLabApi) servletContext.getAttribute(EmbeddedServer.GITLAB_API);
        CodeReviewConfiguration config = (CodeReviewConfiguration) servletContext.getAttribute(EmbeddedServer.CONFIG);

        // Get the specified project
        Project project = getProject(gitlabApi, groupName, projectName);

        // See if we already have this project in the system
        int projectId = project.getId();
        ProjectConfigDAO dao = getProjectConfigDAO();
        ProjectConfig projectConfig = dao.find(projectId);
        if (projectConfig != null) {
            logger.info("This project is already in the system, use PUT to make modifications" + ", group=" + groupName + ", project=" + projectName);
            String message = "This project is already in the system, use PUT to make modifications.";
            throw (new ResponseException(Response.Status.CONFLICT, message));
        }

        MailToType mailToType = MailToType.findByString(mailTo);
        if (mailToType == null) {
            throw (new ResponseException(Status.BAD_REQUEST, "Invalid mail_to[" + mailTo + "]"));
        }

        // Build the Url to the simple-cr webhook
        String webhookUrl = StringUtils.buildUrlString(config.getSimpleCrUrl(), config.getPath(), "webhook");

        // Add the webhook to the project at the GitLab server
        ProjectHook projectHook;
        try {
            projectHook = gitlabApi.getProjectApi().addHook(projectId, webhookUrl, true, false, true);
        } catch (GitLabApiException gle) {
            throw (new ResponseException(gle));
        }

        try {

            projectConfig = new ProjectConfig();
            projectConfig.setProjectId(projectId);
            projectConfig.setHookId(projectHook.getId());
            projectConfig.setEnabled(enabled);
            projectConfig.setBranchRegex(branchRegex);
            projectConfig.setMailToType(mailToType);
            projectConfig.setAdditionalMailTo(additionalMailTo);
            projectConfig.setExcludeMailTo(excludeMailTo);
            projectConfig.setIncludeDefaultMailTo(includeDefaultMailTo);
            int numInserted = dao.insert(projectConfig);
            if (numInserted != 1) {
                throw (new ResponseException(Status.INTERNAL_SERVER_ERROR, "Problem creating project configuration."));
            }

        } catch (Exception e) {
            throw (new ResponseException(Status.INTERNAL_SERVER_ERROR, e.getMessage()));
        }

        URI createdUri = uriInfo.getRequestUri();
        logger.info("Created project config for " + groupName + "/" + projectName + ", location=" + createdUri.toString());
        return (Response.created(createdUri).build());
    }

    @PUT
    @Path("/{groupName}/{projectName}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response update(@PathParam("groupName") String groupName, @PathParam("projectName") String projectName, @FormParam("enabled") Boolean enabled,
            @FormParam("branch_regex") String branchRegex, @FormParam("mail_to") String mailTo, @FormParam("additional_mail_to") String additionalMailTo,
            @FormParam("exclude_mail_to") String excludeMailTo, @FormParam("include_default_mail_to") Boolean includeDefaultMailTo) {

        checkAuthentication();
        logger.info("Update code review setup for project, group=" + groupName + ", project=" + projectName);

        // Get the specified project
        GitLabApi gitlabApi = (GitLabApi) servletContext.getAttribute(EmbeddedServer.GITLAB_API);
        Project project = getProject(gitlabApi, groupName, projectName);

        // Make sure we have this project in the system
        int projectId = project.getId();
        ProjectConfigDAO dao = getProjectConfigDAO();
        ProjectConfig projectConfig = dao.find(projectId);

        if (projectConfig == null) {
            logger.info("This project was not in the system" + ", group=" + groupName + ", project=" + projectName);
            String message = "The specified project was not found in simple-cr the system.";
            throw (new ResponseException(Response.Status.NOT_FOUND, message));
        }

        MailToType mailToType = null;
        if (mailTo != null && mailTo.trim().length() > 0) {
            mailToType = MailToType.findByString(mailTo);
            if (mailToType == null) {
                throw (new ResponseException(Status.BAD_REQUEST, "Invalid mail_to[" + mailTo + "]"));
            }

            projectConfig.setMailToType(mailToType);
        }

        if (enabled != null) {
            projectConfig.setEnabled(enabled);
        }

        if (branchRegex != null) {

            if (branchRegex.trim().isEmpty()) {
                branchRegex = null;
            }

            projectConfig.setBranchRegex(branchRegex);
        }

        if (additionalMailTo != null) {

            if (additionalMailTo.trim().isEmpty()) {
                additionalMailTo = null;
            }

            projectConfig.setAdditionalMailTo(additionalMailTo);
        }

        if (excludeMailTo != null) {

            if (excludeMailTo.trim().isEmpty()) {
                excludeMailTo = null;
            }

            projectConfig.setExcludeMailTo(excludeMailTo);
        }

        if (includeDefaultMailTo != null) {
            projectConfig.setIncludeDefaultMailTo(includeDefaultMailTo);
        }

        int numUpdated = dao.update(projectConfig);
        if (numUpdated != 1) {
            throw (new ResponseException(Status.INTERNAL_SERVER_ERROR, "Problem updating project configuration."));
        }

        String message = "Updated project config for " + groupName + "/" + projectName;
        logger.info(message);
        return (Response.ok().entity(message).type(MediaType.TEXT_PLAIN).build());
    }

    @DELETE
    @Path("/{groupName}/{projectName}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response delete(@PathParam("groupName") String groupName, @PathParam("projectName") String projectName) {

        checkAuthentication();
        logger.info("Delete code review setup for project, group=" + groupName + ", project=" + projectName);

        // Get the specified project
        GitLabApi gitlabApi = (GitLabApi) servletContext.getAttribute(EmbeddedServer.GITLAB_API);
        Project project = getProject(gitlabApi, groupName, projectName);

        // Make sure we have this project in the system
        int projectId = project.getId();
        ProjectConfigDAO dao = getProjectConfigDAO();
        ProjectConfig projectConfig = dao.find(projectId);
        if (projectConfig == null) {
            logger.info("This project was not in the system" + ", group=" + groupName + ", project=" + projectName);
            String message = "The specified project was not found in the simple-cr system.";
            throw (new ResponseException(Response.Status.NOT_FOUND, message));
        }

        // Delete the hook from the GitLab server
        try {
            gitlabApi.getProjectApi().deleteHook(projectId, projectConfig.getHookId());
        } catch (GitLabApiException gle) {
            throw (new ResponseException(gle));
        }

        // We got here then delete the record
        int numDeleted = dao.delete(projectId);
        if (numDeleted != 1) {
            throw (new ResponseException(Status.INTERNAL_SERVER_ERROR, "Problem deleting project configuration."));
        }

        String message = "Deleted project config for " + groupName + "/" + projectName;
        logger.info(message);
        return (Response.ok().entity(message).type(MediaType.TEXT_PLAIN).build());
    }

    private Project getProject(GitLabApi gitlabApi, String groupName, String projectName) {

        try {
            return (gitlabApi.getProjectApi().getProject(groupName, projectName));
        } catch (GitLabApiException gle) {
            logger.error("Problem getting project info, httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            throw (new ResponseException(gle));
        }
    }

    private ProjectConfigDAO getProjectConfigDAO() {
        Jdbi jdbi = (Jdbi) servletContext.getAttribute(EmbeddedServer.JDBI);
        return (jdbi.onDemand(ProjectConfigDAO.class));
    }

    /**
     * TODO
     */
    private void checkAuthentication() {

    }
}
