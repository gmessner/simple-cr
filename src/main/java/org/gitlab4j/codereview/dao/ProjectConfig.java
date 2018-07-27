package org.gitlab4j.codereview.dao;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.gitlab4j.api.utils.JacksonJson;
import org.gitlab4j.codereview.utils.StringUtils;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ProjectConfig {

    public enum MailToType {
        NONE, GROUP, PROJECT;

        public static final MailToType findByString(String s) {

            if (s == null) {
                return (null);
            }

            s = s.trim().toUpperCase();
            if (s.length() == 0) {
                return (null);
            }

            try {
                return (MailToType.valueOf(s));
            } catch (IllegalArgumentException iae) {
                return (null);
            }
        }
    }

    private int id;
    private Date created;
    private boolean enabled;
    private int projectId;
    private int hookId;
    private String branchRegex;
    private String targetBranchRegex; 
    private String mailTo;
    private String additionalMailTo;
    private String excludeMailTo;
    private boolean includeDefaultMailTo;

    @JsonIgnore
    private final JacksonJson jacksonJson = new JacksonJson();


    public ProjectConfig() {
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the received
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * @return the enabled flag
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the projectId
     */
    public int getProjectId() {
        return projectId;
    }

    /**
     * @param projectId the projectId to set
     */
    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    /**
     * @return the hookId
     */
    public int getHookId() {
        return hookId;
    }

    /**
     * @param hookId the hookId to set
     */
    public void setHookId(int hookId) {
        this.hookId = hookId;
    }

    /**
     * @return the branchRegex
     */
    public String getBranchRegex() {
        return branchRegex;
    }

    /**
     * @param branchRegex the branchRegex to set
     */
    public void setBranchRegex(String branchRegex) {
        this.branchRegex = branchRegex;
    }

    /**
     * @return the targetBranchRegex
     */
    public String getTargetBranchRegex() {
        return targetBranchRegex;
    }

    /**
     * @param targetBranchRegex the targetBranchRegex to set
     */
    public void setTargetBranchesRegex(String targetBranchRegex) {
        this.targetBranchRegex = targetBranchRegex;
    }

    /**
     * @return the mailTo
     */
    public String getMailTo() {
        return (mailTo);
    }

    /**
     * @return the mailTo
     */
    public MailToType getMailToType() {

        if (mailTo == null) {
            return (null);
        }

        return (MailToType.valueOf(mailTo));
    }

    /**
     * @param mailTo
     */
    public void setMailToType(MailToType mailTo) {
        this.mailTo = mailTo.name();
    }

    /**
     * @param mailTo
     */
    public void setMailTo(String mailTo) {
        this.mailTo = mailTo;
    }

    /**
     * @return the additionalMailTo
     */
    public String getAdditionalMailTo() {
        return additionalMailTo;
    }

    /**
     * @param additionalMailTo the additionalMailTo to set
     */
    public void setAdditionalMailTo(String additionalMailTo) {
        this.additionalMailTo = additionalMailTo;
    }

    /**
     * @return the excludeMailTo
     */
    public String getExcludeMailTo() {
        return excludeMailTo;
    }

    /**
     * @param excludeMailTo the excludeMailTo to set
     */
    public void setExcludeMailTo(String excludeMailTo) {
        this.excludeMailTo = excludeMailTo;
    }

    /**
     * @return the includeDefaultMailTo
     */
    public boolean getIncludeDefaultMailTo() {
        return includeDefaultMailTo;
    }

    /**
     * @param includeDefaultMailTo the includeDefaultMailTo to set
     */
    public void setIncludeDefaultMailTo(boolean includeDefaultMailTo) {
        this.includeDefaultMailTo = includeDefaultMailTo;
    }

    /**
     * Get a String list of additional email addresses to email to.
     * 
     * @return a String list of additional email addresses to email to
     */
    public List<String> getAdditionalMailToAsList() {
        return (StringUtils.getListFromString(additionalMailTo, ";"));
    }

    /**
     * Get a String list of additional email addresses to email to.
     * 
     * @return a String list of additional email addresses to email to
     */
    public List<String> getExcludelMailToAsList() {
        return (StringUtils.getListFromString(excludeMailTo, ";"));
    }

    public static class ProjectConfigMapper implements RowMapper<ProjectConfig> {

        public ProjectConfig map(ResultSet rs, StatementContext context) throws SQLException {

            ProjectConfig config = new ProjectConfig();
            config.id = rs.getInt("id");
            Timestamp ts = rs.getTimestamp("created");
            config.created = (ts != null ? new Date(ts.getTime()) : null);
            config.projectId = rs.getInt("project_id");
            config.hookId = rs.getInt("hook_id");
            config.enabled = rs.getBoolean("enabled");
            config.branchRegex = rs.getString("branch_regex");
            config.targetBranchRegex = rs.getString("target_branch_regex");
            config.mailTo = rs.getString("mail_to");
            config.additionalMailTo = rs.getString("additional_mail_to");
            config.excludeMailTo = rs.getString("exclude_mail_to");
            config.includeDefaultMailTo = rs.getBoolean("include_default_mail_to");

            return (config);
        }
    }
    
    public String toJson() throws IOException {
        return (jacksonJson.getObjectMapper().writeValueAsString(this));
    }
}
