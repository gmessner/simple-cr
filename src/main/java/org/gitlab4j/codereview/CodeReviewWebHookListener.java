package org.gitlab4j.codereview;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.jdbcx.JdbcConnectionPool;
import org.skife.jdbi.v2.DBI;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.webhook.*;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;
import org.gitlab4j.codereview.dao.Push;
import org.gitlab4j.codereview.dao.PushDAO;

/**
 * This class listens for Web Hook events and processes them. Basically a push event
 * for a branch will result in a code review request email being sent to whoever pushed the branch.
 * 
 * We track the lifecycle of the code review request here and update a Push record. This makes sure
 * we are not doing additional requests on the same branch that has yet to be reviewed.
 * 
 * @author greg@messners.com
 *
 */
public class CodeReviewWebHookListener implements WebHookListener {

    private Log log = LogFactory.getLog(CodeReviewWebHookListener.class);

    private CodeReviewConfiguration config;
    private GitLabApi gitlabApi;
    private JdbcConnectionPool connectionPool;

    CodeReviewWebHookListener(CodeReviewConfiguration config, GitLabApi gitlabApi, JdbcConnectionPool connectionPool) {
        this.config = config;
        this.gitlabApi = gitlabApi;
        this.connectionPool = connectionPool;
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

        log.info("Merge request notification received, userId=" + userId + ", projectId=" + projectId + ", mergRequestId=" + mergeRequestId + ", mergeStatus=" + mergeStatus
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
            log.error("Problem getting merge request info" + ", httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return;
        }

        // Now find and update the push record
        PushDAO dao = getPushDAO();
        try {

            // Make sure we have a push record that has not been submitted for code review
            List<Push> pushList = dao.find(userId, projectId, branchName, mergeRequestId);
            if (pushList == null || pushList.size() == 0) {
                log.warn("Could not locate push record for merge request" + ", userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName + ", mergeRequestId="
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
                                log.warn("Error trying to determine merged by ID, message=" + gle.getMessage());
                            }
                        }
                    }

                    if (mergedById == 0)
                        mergedById = (mergeRequest.getAssignee() != null ? mergeRequest.getAssignee().getId() : 0);
                }

                dao.updateMergeStatus(pushId, attributes.getUpdatedAt(), mergeStatus, mergeState, mergedById);
                log.info("Updated push record, userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName + ", mergeRequestId=" + mergeRequestId + ", mergedState="
                        + mergeState + ", mergeStatus=" + mergeStatus);
            } else {

                log.info("Push record already updated, userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName + ", mergeRequestId=" + mergeRequestId
                        + ", mergedState=" + mergeState + ", mergeStatus=" + mergeStatus);
            }

        } finally {
            dao.close();
        }
    }

    /**
     * This method is called when a push notification is received. We make sure the state of all the associated objects
     * are correct and if so create a Push record and send an email to the user with a link to a code review submittal form.
     * We also make sure that we don''t send multiple emails to the user for additional pushes of a branch that is
     * already pending review.
     * 
     * @param pushEvent
     */
    @Override
    public void onPushEvent(PushEvent pushEvent) {

        int userId = pushEvent.getUserId();
        int projectId = pushEvent.getProjectId();
        String branchName = pushEvent.getBranch();
        log.info("A branch has been pushed, userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName);

        if (StringUtils.isEmpty(branchName)) {
            log.error("branch name is either null or not valid, ref=" + pushEvent.getRef());
            return;
        }

        if (branchName.equals("master")) {
            log.warn("No code reviews are done on master.");
            return;
        }

        Project project = null;
        try {
            project = gitlabApi.getProjectApi().getProject(projectId);
        } catch (GitLabApiException gle) {
            log.error("Problem getting project info" + ", httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return;
        }

        User user;
        try {
            user = gitlabApi.getUserApi().getUser(userId);
            if (StringUtils.isEmpty(user.getEmail()))
                user.setEmail(pushEvent.getUserEmail());
        } catch (GitLabApiException gle) {
            log.error("Problem getting user info" + ", httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return;
        }

        // Make sure that the branch is still valid (not deleted).
        try {
            gitlabApi.getRepositoryApi().getBranch(projectId, branchName);
        } catch (GitLabApiException gle) {
            log.error("Problem getting branch info" + ", httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage());
            return;
        }

        // If after is all "0" this indicates that this notification is for the deletion of that branch.
        String after = pushEvent.getAfter();
        if (StringUtils.containsOnly(after, "0")) {
            log.info("The branch has been deleted nothing to do here, before=" + pushEvent.getBefore() + ", after=" + after + ".\n");
            return;
        }

        PushDAO dao = getPushDAO();
        try {

            // Make sure that we DO NOT have a pending code review for this branch
            List<Push> pushList = dao.findPendingReviews(userId, projectId, branchName);
            if (pushList != null && pushList.size() > 0) {
                log.info("The branch is already pending review and merge" + ", userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName);
                return;
            }

            // Make sure we DO NOT have a push record that has not been submitted for code review
            pushList = dao.find(userId, projectId, branchName, 0);
            if (pushList != null && pushList.size() > 0) {
                log.info("Branch push notification has already been sent" + ", userId=" + userId + ", projectId=" + projectId + ", branch=" + branchName);
                return;
            }

            // Add a Push record for this push event
            dao.insert(userId, projectId, branchName, pushEvent.getBefore(), pushEvent.getAfter());

        } finally {
            dao.close();
        }

        CodeReviewMailer mailer = new CodeReviewMailer(config, gitlabApi);
        mailer.sendCodeReviewEmail(user, project, branchName);
    }

    /**
     * Get a PushDAO instance. Consumers of this need to make sure to close the PushDAO instance when finished with it.
     * 
     * @return a PushDAO instance
     */
    private PushDAO getPushDAO() {
        DBI dbi = new DBI(connectionPool);
        return (dbi.open(PushDAO.class));
    }

    @Override
    public void onBuildEvent(BuildEvent buildEvent) {
        log.warn("We do not handle build events in this webhook");
    }

    @Override
    public void onIssueEvent(IssueEvent issueEvent) {
        log.warn("We do not handle issues in this webhook");
    }

    @Override
    public void onNoteEvent(NoteEvent noteEvent) {
        log.warn("We do not handle note events in this webhook");
    }

    @Override
    public void onPipelineEvent(PipelineEvent pipelineEvent) {
        log.warn("We do not handle pipeline events in this webhook");
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        log.warn("We do not handle tag push events in this webhook");
    }

    @Override
    public void onWikiPageEvent(WikiPageEvent wikiEvent) {
        log.warn("We do not handle wiki page events in this webhook");
    }
}
