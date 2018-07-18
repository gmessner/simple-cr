package org.gitlab4j.codereview;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.gitlab4j.codereview.dao.ProjectConfig;
import org.gitlab4j.codereview.dao.ProjectConfig.MailToType;
import org.gitlab4j.codereview.utils.HashUtils;
import org.gitlab4j.codereview.utils.StringUtils;
import org.gitlab4j.codereview.utils.VelocityUtils;

/**
 * 
 */
public class CodeReviewMailer {

    private static Logger logger = LogManager.getLogger();

    private static final String CODE_REVIEW_TEMPLATE = "/templates/code-review.vm";
    private static final String CODE_REVIEW_SUBJECT = "Your Branch Push";

    private static final String MERGE_REQUEST_TEMPLATE = "/templates/merge-request.vm";
    private static final String MERGE_REQUEST_SUBJECT = "Code Review/Merge Request";

    private CodeReviewConfiguration config;
    private GitLabApi gitlabApi;

    public CodeReviewMailer(CodeReviewConfiguration config, GitLabApi gitlabApi) {
        this.config = config;
        this.gitlabApi = gitlabApi;
    }

    public boolean sendMergeRequestEmail(ProjectConfig projectConfig, MergeRequest mergeRequest) {

        if (!isEmailEnabled()) {
            return (false);
        }

        Integer projectId = mergeRequest.getProjectId();
        Project project;
        try {
            project = gitlabApi.getProjectApi().getProject(projectId);
        } catch (GitLabApiException gle) {
            logger.error("Problem getting project info, httpStatus=" + gle.getHttpStatus() + ", error=" + gle.getMessage(), gle);
            return (false);
        }

        if (project.getId() == null || !project.getId().equals(projectId)) {
            logger.error("Problem getting project info, projectId=" + projectId + ", project.id=" + project.getId());
            return (false);
        }

        Author author = mergeRequest.getAuthor();
        String branch = mergeRequest.getSourceBranch();
        String projectName = project.getName().trim();
        String group = project.getNamespace().getName().trim();
        String mergeRequestLink = config.getGitLabWebUrl() + "/" + group + "/" + projectName + "/merge_requests/" + mergeRequest.getIid();

        Collection<String> reviewers = getReviewers(projectConfig, project.getNamespace().getId(), author);
        if (reviewers == null || reviewers.size() < 1) {
            logger.warn("No reviewers are configured for this project.");
            return (false);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("gitlabWebUrl", config.getGitLabWebUrl());
        data.put("mergeRequestLink", mergeRequestLink);
        data.put("mergeRequest", mergeRequest);
        data.put("projectName", projectName);
        data.put("project", project);
        data.put("branch", branch);
        data.put("group", group);
        data.put("author", author);
        System.out.println("Template data: " + data);

        try {

            Reader reader = new InputStreamReader(CodeReviewMailer.class.getResourceAsStream(MERGE_REQUEST_TEMPLATE));
            String htmlContent = VelocityUtils.getTextBody(reader, data);
            reader.close();

            send(reviewers, MERGE_REQUEST_SUBJECT, htmlContent);
            return (true);

        } catch (Exception e) {
            logger.error("Something went wrong while sending code review email, error=" + e.getMessage(), e);
            return (false);
        }
    }

    boolean sendCodeReviewEmail(User user, Project project, String branch) {

        if (!isEmailEnabled()) {
            return (false);
        }

        /*
         * Set up all the data for the code review request email and send it to the user that
         * initiated the branch push.
         */
        Integer userId = user.getId();
        Integer projectId = project.getId();
        String encodedBranch = StringUtils.urlEncodeString(branch);
        String signature = HashUtils.makeHash(HashUtils.SHORT_HASH, projectId, branch, userId);
        String codeReviewLink = StringUtils.buildUrlString(config.getSimpleCrUrl(), config.getPath(), "app",
                "?p=" + projectId + "&b=" + encodedBranch + "&u=" + userId + "&s=" + signature);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("codeReviewLink", codeReviewLink);
        data.put("gitlabWebUrl", config.getGitLabWebUrl());
        data.put("projectName", project.getName());
        data.put("project", project);
        data.put("branch", branch);
        data.put("group", project.getNamespace().getName());
        data.put("user", user);
        System.out.println("Template data: " + data + "\n");

        try {

            Reader reader = new InputStreamReader(CodeReviewMailer.class.getResourceAsStream(CODE_REVIEW_TEMPLATE));
            String htmlContent = VelocityUtils.getTextBody(reader, data);
            logger.debug("Email body: \n" + htmlContent);
            reader.close();

            send(user.getEmail(), user.getName(), CODE_REVIEW_SUBJECT, htmlContent);
            return (true);

        } catch (Exception e) {
            logger.error("Something went wrong while sending code review email, error=" + e.getMessage(), e);
            return (false);
        }
    }

    void send(String email, String name, String subject, String htmlContent) throws EmailException {
        EmailAddress emailAddress = new EmailAddress(email, name);
        sendMail(Arrays.asList(emailAddress), subject, htmlContent);
    }

    void send(Collection<String> emailList, String subject, String htmlContent) throws EmailException {

        List<EmailAddress> toEmailList = new ArrayList<EmailAddress>(emailList.size());
        for (String email : emailList) {
            toEmailList.add(new EmailAddress(email, null));
        }

        sendMail(toEmailList, subject, htmlContent);
    }

    private boolean isEmailEnabled() {
        String smtpHost = config.getSmtpHost();
        int smtpPort = config.getSmtpPort();
        return (smtpHost != null && smtpPort > 0);
    }

    private void sendMail(List<EmailAddress> toEmailList, String subject, String htmlContent) throws EmailException {

        String smtpHost = config.getSmtpHost();
        int smtpPort = config.getSmtpPort();
        if (smtpHost == null || smtpPort < 1) {
            throw new EmailException("SMTP is not configured");
        }

        HtmlEmail email = new HtmlEmail();
        email.setHostName(smtpHost);
        email.setSmtpPort(smtpPort);

        if (config.getSmtpEnableStartTls()) {
            email.setStartTLSEnabled(true);
        }

        for (EmailAddress toEmail : toEmailList) {
            email.addTo(toEmail.email, toEmail.name);
        }

        email.setFrom(config.getFromEmail(), config.getFromName());
        email.setSubject(subject);
        String smtpUsername = config.getSmtpUsername();
        String smtpPassword = config.getSmtpPassword();
        if (smtpUsername != null && smtpPassword != null) {
            email.setAuthenticator(new DefaultAuthenticator(smtpUsername, smtpPassword));
        }

        email.setHtmlMsg(htmlContent);
        email.send();
    }

    private class EmailAddress {

        private String email;
        private String name;

        EmailAddress(String email, String name) {
            this.email = email;
            this.name = name;
        }
    }

    /**
     * Get the set of reviewer email addresses for the ProjectConfig. This list will always exclude the
     * author's email unless the author is the only reviewer.
     * 
     * @param projectConfig
     * @param author
     * @return
     */
    private Collection<String> getReviewers(ProjectConfig projectConfig, int groupId, Author author) {

        TreeSet<String> reviewers = new TreeSet<String>();
        List<Member> members = null;
        if (MailToType.GROUP.equals(projectConfig.getMailToType())) {

            try {
                members = gitlabApi.getGroupApi().getMembers(groupId);
                logger.info("GROUP reviewer list, numMembers=" + (members == null ? 0 : members.size()));
            } catch (GitLabApiException e) {
                logger.error("Something went wrong while getting group members, error=" + e.getMessage(), e);
            }

        } else if (MailToType.PROJECT.equals(projectConfig.getMailToType())) {

            try {
                members = gitlabApi.getProjectApi().getMembers(projectConfig.getProjectId());
                logger.info("PROJECT reviewer list, numMembers=" + (members == null ? 0 : members.size()));
            } catch (GitLabApiException e) {
                logger.error("Something went wrong while getting project members, error=" + e.getMessage(), e);
            }
        }

        if (members != null) {
            for (Member member : members) {
                Optional<User> optionalUser = gitlabApi.getUserApi().getOptionalUser(member.getId());
                if (optionalUser.isPresent()) {
                    String email = optionalUser.get().getEmail();
                    if (email != null && email.trim().length() > 0)
                        reviewers.add(email);
                }
            }
        }

        List<String> additionalMailToList = projectConfig.getAdditionalMailToAsList();
        if (additionalMailToList != null) {
            reviewers.addAll(additionalMailToList);
        }

        if (projectConfig.getIncludeDefaultMailTo()) {
            reviewers.addAll(config.getDefaultReviewers());
        }

        if (reviewers == null || reviewers.size() == 0) {
            return (null);
        }

        // Get the list of excluded emails and remove them from the reviewers list
        List<String> excludeEmails = projectConfig.getExcludelMailToAsList();
        if (excludeEmails != null) {
            reviewers.removeAll(excludeEmails);
        }

        // If the list > 1 in length make sure the author is not in the list
        if (reviewers.size() > 1) {
            String authorEmail = author.getEmail();
            reviewers.remove(authorEmail);
        }

        return (reviewers);
    }
}
