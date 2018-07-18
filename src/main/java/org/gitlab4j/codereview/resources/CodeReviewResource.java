
package org.gitlab4j.codereview.resources;

import java.io.InputStream;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.gitlab4j.codereview.CodeReviewConfiguration;
import org.gitlab4j.codereview.CodeReviewMailer;
import org.gitlab4j.codereview.beans.AppResponse;
import org.gitlab4j.codereview.beans.CodeReviewInfo;
import org.gitlab4j.codereview.dao.ProjectConfig;
import org.gitlab4j.codereview.dao.ProjectConfigDAO;
import org.gitlab4j.codereview.dao.Push;
import org.gitlab4j.codereview.dao.PushDAO;
import org.gitlab4j.codereview.server.EmbeddedServer;
import org.gitlab4j.codereview.utils.HashUtils;
import org.jdbi.v3.core.Jdbi;

/**
 * CodeReviewResource
 * 
 * This class provides the endpoints for Simple-CR web client.
 */
@Path("/rest")
public class CodeReviewResource {

    private static Logger logger = LogManager.getLogger();

    @Context
    ServletContext servletContext;
    @Context
    HttpServletRequest request;

    @GET
    @Path("/{projectId}/{branchName}/{userId}/{signature}")
    @Produces(MediaType.TEXT_HTML)
    public Response getForm(@PathParam("projectId") int projectId, @PathParam("branchName") String branchName, @PathParam("userId") int userId,
            @PathParam("signature") String signature) {

        logger.info("getForm: projectId=" + projectId + ", branchName=" + branchName + ", userId=" + userId + ", signature=" + signature);

        if (!HashUtils.isValidHash(signature, HashUtils.SHORT_HASH, projectId, branchName, userId)) {
            System.err.println("WARNING: invalid signature");
            return (Response.status(Status.BAD_REQUEST).entity("Bad code review request").type("text/plain").build());
        }

        InputStream htmlIn = CodeReviewResource.class.getResourceAsStream("/index.html");
        return (Response.ok().entity(htmlIn).type(MediaType.TEXT_HTML).build());
    }

    @GET
    @Path("/load")
    @Produces(MediaType.APPLICATION_JSON)
    public Response load() {
        logger.warn("load() called without parameters");
        return (AppResponse.getMessageResponse(false, "No branch specified, nothing to review here."));
    }

    @GET
    @Path("/load/{projectId}/{branchName}/{userId}/{signature}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response load(@PathParam("projectId") int projectId, @PathParam("branchName") String branchName, @PathParam("userId") int userId,
            @PathParam("signature") String signature) {

        logger.info("load: projectId=" + projectId + ", branchName=" + branchName + ", userId=" + userId + ", signature=" + signature);

        if (!HashUtils.isValidHash(signature, HashUtils.SHORT_HASH, projectId, branchName, userId)) {
            logger.warn("Invalid signature");
            return (AppResponse.getMessageResponse(false, "Bad code review data request"));
        }

        GitLabApi gitlabApi = (GitLabApi) servletContext.getAttribute(EmbeddedServer.GITLAB_API);
        CodeReviewConfiguration config = (CodeReviewConfiguration) servletContext.getAttribute(EmbeddedServer.CONFIG);

        Project project;
        try {
            project = gitlabApi.getProjectApi().getProject(projectId);
        } catch (GitLabApiException gle) {
            logger.error("Problem getting project info" + ", httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return (AppResponse.getMessageResponse(false, "Could not load project info for code review"));
        }

        if (project.getId() == null || !project.getId().equals(projectId)) {
            logger.error("Problem getting project info, projectId=" + projectId + ", project.id=" + project.getId());
            return (AppResponse.getMessageResponse(false, "Could not load project info for code review"));
        }

        User user;
        try {
            user = gitlabApi.getUserApi().getUser(userId);
        } catch (GitLabApiException gle) {
            logger.error("Problem getting user info, httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return (AppResponse.getMessageResponse(false, "Could not load project info for code review"));
        }

        if (user.getId() == null || !user.getId().equals(userId)) {
            logger.error("Problem getting user info, userId=" + userId + ", user.id=" + user.getId());
            return (AppResponse.getMessageResponse(false, "Could not load user info for code review"));
        }

        // We default the status to success, with an empty statusText message
        AppResponse.Status status = AppResponse.Status.OK;
        String statusText = null;

        // Make sure that we don't have a pending code review for this branch
        PushDAO dao = getPushDAO();
        List<Push> pushList = dao.findPendingReviews(userId, projectId, branchName);
        String title = null;
        String description = null;
        List<String> targetBranches = null;
        if (pushList != null && pushList.size() > 0) {

            logger.info("This branch is already pending review" + ", userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName);
            statusText = "This branch push is already pending review.";
            status = AppResponse.Status.NO_ACTION;

            try {
                MergeRequest mergeRequest = gitlabApi.getMergeRequestApi().getMergeRequest(projectId, pushList.get(0).getMergeRequestId());
                title = mergeRequest.getTitle();
                description = mergeRequest.getDescription();
            } catch (GitLabApiException gle) {
                logger.warn("Problem getting merge request info, httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            }

        } else {

            // Make sure we have a push record that has not been submitted for code review
            pushList = dao.find(userId, projectId, branchName, 0);
            if (pushList == null || pushList.size() == 0) {
                logger.info("No branch pushes are available for review" + ", userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName);

                pushList = dao.find(userId, projectId, branchName);

                if (pushList == null || pushList.size() == 0) {
                    statusText = "This branch push has already been reviewed.";
                    status = AppResponse.Status.NO_ACTION;
                } else {
                    Push push = pushList.get(0);
                    statusText = "This branch push has already been reviewed and " + push.getMergeState() + ".";
                    status = AppResponse.Status.NO_ACTION;
                }
            }
        }

        CodeReviewInfo codeReviewInfo = new CodeReviewInfo();
        codeReviewInfo.setGroup(project.getNamespace().getName());
        codeReviewInfo.setProjectId(projectId);
        codeReviewInfo.setProjectName(project.getName());
        codeReviewInfo.setProjectUrl(project.getWebUrl());
        codeReviewInfo.setSourceBranch(branchName);
        codeReviewInfo.setTargetBranch("master");
        codeReviewInfo.setUserId(userId);
        codeReviewInfo.setName(user.getName());
        codeReviewInfo.setEmail(user.getEmail());
        codeReviewInfo.setGitlabWebUrl(config.getGitLabWebUrl());
        codeReviewInfo.setTargetBranches(targetBranches);
        codeReviewInfo.setTitle(title);
        codeReviewInfo.setDescription(description);
        return (AppResponse.getResponse(status, statusText, codeReviewInfo));
    }

    @POST
    @Path("/submit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response submit(@FormParam("merge_request[user_id]") int userId, @FormParam("merge_request[source_project_id]") int sourceProjectId,
            @FormParam("merge_request[source_branch]") String sourceBranch, @FormParam("merge_request[target_project_id]") int targetProjectId,
            @FormParam("merge_request[target_branch]") String targetBranch, @FormParam("merge_request[title]") String title,
            @FormParam("merge_request[description]") String description) {

        logger.info("submit: user_id=" + userId + ", source_project_id=" + sourceProjectId + ", sourceBranch=" + sourceBranch + ", targetProjectId=" + targetProjectId
                + ", targetBranch=" + targetBranch + ", title=" + title + ", description=" + description);

        ServletContext servletContext = request.getServletContext();
        GitLabApi gitlabApi = (GitLabApi) servletContext.getAttribute(EmbeddedServer.GITLAB_API);

        // Make sure we have this project in the system and it is enabled
        ProjectConfigDAO projectConfigDao = getProjectConfigDAO();
        ProjectConfig projectConfig = projectConfigDao.find(targetProjectId);
        if (projectConfig == null) {
            logger.info("The target project is not in the simple-cr system, targetProjectId=" + targetProjectId);
            String message = "The specified project was not found in simple-cr the system.";
            return (AppResponse.getMessageResponse(AppResponse.Status.NO_ACTION, message));
        }

        if (!projectConfig.getEnabled()) {
            logger.info("The target project does not have code reviews enabled, targetProjectId=" + targetProjectId);
            String message = "The target project does not have code reviews enabled.";
            return (AppResponse.getMessageResponse(AppResponse.Status.NO_ACTION, message));
        }

        // Make sure we have a push record that has not been submitted for code review
        PushDAO dao = getPushDAO();
        List<Push> pushList = dao.find(userId, sourceProjectId, sourceBranch, 0);
        if (pushList == null || pushList.size() == 0) {
            logger.info("No branch pushes are available for review" + ", userId=" + userId + ", projectId=" + sourceProjectId + ", branch=" + sourceBranch);
            return (AppResponse.getMessageResponse(AppResponse.Status.NO_ACTION, "This branch is already pending review."));
        }

        MergeRequest mergeRequest;
        try {
            mergeRequest = gitlabApi.getMergeRequestApi().createMergeRequest(targetProjectId, sourceBranch, targetBranch, title, description, null);
        } catch (GitLabApiException gle) {
            logger.error("Problem creating merge request" + ", httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return (AppResponse.getMessageResponse(AppResponse.Status.NO_ACTION, "This branch has already been merged or deleted"));
        }

        // Update the Push record
        dao.setMergeRequest(pushList.get(0).getId(), mergeRequest.getIid());

        CodeReviewConfiguration config = (CodeReviewConfiguration) servletContext.getAttribute(EmbeddedServer.CONFIG);
        CodeReviewMailer mailer = new CodeReviewMailer(config, gitlabApi);
        mailer.sendMergeRequestEmail(projectConfig, mergeRequest);

        return (AppResponse.getMessageResponse(true, "Your request for code review and merge has been submitted."));
    }

    @SuppressWarnings("unused")
    private Integer getDefaultAssignee(ProjectConfig projectConfig) {

        List<String> additionalMailToList = projectConfig.getAdditionalMailToAsList();
        if (additionalMailToList != null && additionalMailToList.size() > 0) {
            
        } else if (projectConfig.getIncludeDefaultMailTo()) {
            // config.getDefaultReviewers());
        }

        return (0);
    }

    private ProjectConfigDAO getProjectConfigDAO() {
        Jdbi jdbi = (Jdbi) servletContext.getAttribute(EmbeddedServer.JDBI);
        return (jdbi.onDemand(ProjectConfigDAO.class));
    }

    private PushDAO getPushDAO() {
        Jdbi jdbi = (Jdbi) servletContext.getAttribute(EmbeddedServer.JDBI);
        return (jdbi.onDemand(PushDAO.class));
    }
}
