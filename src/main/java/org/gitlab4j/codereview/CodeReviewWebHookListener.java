package org.gitlab4j.codereview;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.WebHookListener;
import org.gitlab4j.codereview.dao.ProjectConfig;
import org.gitlab4j.codereview.dao.ProjectConfigDAO;
import org.gitlab4j.codereview.dao.Push;
import org.gitlab4j.codereview.dao.PushDAO;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

/**
 * This class listens for Web Hook events and processes them. Basically a push event
 * for a branch will result in a code review request email being sent to whoever pushed the branch.
 * 
 * We track the lifecycle of the code review request here and update a Push record. This makes sure
 * we are not doing additional requests on the same branch that has yet to be reviewed.
 *
 */
public class CodeReviewWebHookListener implements WebHookListener {

    private Logger logger = LogManager.getLogger();

    private CodeReviewConfiguration config;
    private GitLabApi gitlabApi;
    private Jdbi jdbi;

    CodeReviewWebHookListener(CodeReviewConfiguration config, GitLabApi gitlabApi, Jdbi jdbi) {
        this.config = config;
        this.gitlabApi = gitlabApi;
        this.jdbi = jdbi;
    }

    /**
     * This method is called when a merge request is either created or changes state. We use it to update the Push record
     * for the branch. this allows us t eliminate sending out multiple emails
     * 
     * @param mergeRequestEvent
     */
    @Override
    public void onMergeRequestEvent(MergeRequestEvent mergeRequestEvent) {

        ObjectAttributes attributes = mergeRequestEvent.getObjectAttributes();
        String branchName = attributes.getSourceBranch();
        int userId = attributes.getAuthorId();
        int projectId = attributes.getTargetProjectId();
        int mergeRequestId = attributes.getIid();
        String mergeState = attributes.getState();
        String mergeStatus = attributes.getMergeStatus();

        logger.info("Merge request notification received, userId=" + userId + ", projectId=" + projectId + ", mergRequestId=" + mergeRequestId + ", mergeStatus=" + mergeStatus
                + ", mergeState=" + mergeState);

        // We only operate on merged or closed state changes
        if (!"merged".equals(mergeState) && !"closed".equals(mergeState)) {
            return;
        }

        // Make sure the merge request is valid
        MergeRequest mergeRequest = null;
        try {
            mergeRequest = gitlabApi.getMergeRequestApi().getMergeRequest(projectId, attributes.getId());
        } catch (GitLabApiException gle) {
            logger.error("Problem getting merge request info" + ", httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return;
        }

        // Now find and update the push record
        try (Handle handle = jdbi.open()) {

            PushDAO dao = handle.attach(PushDAO.class);

            // Make sure we have a push record that has not been submitted for code review
            List<Push> pushList = dao.find(userId, projectId, branchName, mergeRequestId);
            if (pushList == null || pushList.size() == 0) {
                logger.warn("Could not locate push record for merge request" + ", userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName + ", mergeRequestId="
                        + mergeRequestId);
                return;
            }

            // Update the Push record for this push if not already updated
            Push push = pushList.get(0);
            if (!mergeState.equals(push.getMergeState())) {

                int pushId = push.getId();

                // If the MR was merged, get the merged by ID
                int mergedById = 0;
                if ("merged".equals(mergeState)) {
                    User user = mergeRequestEvent.getUser();
                    if (user != null) {
                        if (user.getId() != null) {
                            mergedById = user.getId();
                        } else if (StringUtils.isNotEmpty(user.getUsername())) {
                            try {
                                List<User> users = gitlabApi.getUserApi().findUsers(user.getUsername());
                                if (users != null && !users.isEmpty())
                                    mergedById = users.get(0).getId();
                            } catch (GitLabApiException gle) {
                                logger.warn("Error trying to determine merged by ID, message=" + gle.getMessage());
                            }
                        }
                    }

                    if (mergedById == 0)
                        mergedById = (mergeRequest.getAssignee() != null ? mergeRequest.getAssignee().getId() : 0);
                }

                dao.updateMergeStatus(pushId, attributes.getUpdatedAt(), mergeStatus, mergeState, mergedById);
                logger.info("Updated push record, userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName + ", mergeRequestId=" + mergeRequestId + ", mergedState="
                        + mergeState + ", mergeStatus=" + mergeStatus);
            } else {

                logger.info("Push record already updated, userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName + ", mergeRequestId=" + mergeRequestId
                        + ", mergedState=" + mergeState + ", mergeStatus=" + mergeStatus);
            }
        }
    }

    /**
     * This method is called when a push notification is received. We make sure the state of all the associated objects
     * are correct and if so create a Push record and send an email to the user with a link to a code review submittal form.
     * We also make sure that we don't send multiple emails to the user for additional pushes of a branch that is
     * already pending review.
     * 
     * @param pushEvent
     */
    @Override
    public void onPushEvent(PushEvent pushEvent) {

        int userId = pushEvent.getUserId();
        int projectId = pushEvent.getProjectId();
        String branchName = pushEvent.getBranch();
        logger.info("A branch has been pushed, userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName);

        ProjectConfigDAO projectConfigDao = jdbi.onDemand(ProjectConfigDAO.class);
        ProjectConfig projectConfig = projectConfigDao.find(projectId);
        if (projectConfig == null) {
            logger.info("This project is not in the Simple-CR system, projectId=" + projectId);
            return;
        }

        if (StringUtils.isEmpty(branchName)) {
            logger.warn("branch name is either null or not valid, ref=" + pushEvent.getRef());
            return;
        }

        if (branchName.equals("master")) {
            logger.info("No code reviews are done on master.");
            return;
        }

        // If a branchRegex is configured, make sure the branch matches the regex
        String branchRegex = projectConfig.getBranchRegex();
        if (branchRegex != null && branchRegex.trim().length() > 0) {
           if (!branchName.matches(branchRegex)) {
               logger.info("The pushed branch is not configured to trigger Simple-CR, pushed branch=" +
                       branchName + ", branchRegex=" + branchRegex);
               return;
           }
        }

        Project project = null;
        try {
            project = gitlabApi.getProjectApi().getProject(projectId);
        } catch (GitLabApiException gle) {
            logger.error("Problem getting project info" + ", httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return;
        }

        User user;
        try {
            user = gitlabApi.getUserApi().getUser(userId);
            if (StringUtils.isEmpty(user.getEmail()))
                user.setEmail(pushEvent.getUserEmail());
        } catch (GitLabApiException gle) {
            logger.error("Problem getting user info" + ", httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return;
        }

        // Make sure that the branch is still valid (not deleted).
        try {
            gitlabApi.getRepositoryApi().getBranch(projectId, branchName);
        } catch (GitLabApiException gle) {
            logger.error("Problem getting branch info" + ", httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return;
        }

        // If after is all "0" this indicates that this notification is for the deletion of that branch.
        String after = pushEvent.getAfter();
        if (StringUtils.containsOnly(after, "0")) {
            logger.info("The branch has been deleted nothing to do here, before=" + pushEvent.getBefore() + ", after=" + after + ".\n");
            return;
        }

        try (Handle handle = jdbi.open()) {

            PushDAO dao = handle.attach(PushDAO.class);

            // Make sure that we DO NOT have a pending code review for this branch
            List<Push> pushList = dao.findPendingReviews(userId, projectId, branchName);
            if (pushList != null && pushList.size() > 0) {
                logger.info("The branch is already pending review and merge" + ", userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName);
                return;
            }

            // Make sure we DO NOT have a push record that has not been submitted for code review
            pushList = dao.find(userId, projectId, branchName, 0);
            if (pushList != null && pushList.size() > 0) {
                logger.info("Branch push notification has already been sent" + ", userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName);
                return;
            }

            // Add a Push record for this push event
            dao.insert(userId, projectId, branchName, pushEvent.getBefore(), pushEvent.getAfter());

        }

        CodeReviewMailer mailer = new CodeReviewMailer(config, gitlabApi);
        mailer.sendCodeReviewEmail(user, project, branchName);
    }
}
