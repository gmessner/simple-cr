package org.gitlab4j.codereview.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class Push {

    private int id;
    private Date received;
    private int userId;
    private String branch;
    private int projectId;
    private String before;
    private String after;
    private int mergeRequestId;
    private Date mergeStatusDate;
    private String mergeState;
    private String mergeStatus;

    private int mergedById;

    public Push() {
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
    public Date getReceived() {
        return received;
    }

    /**
     * @param received the received to set
     */
    public void setReceived(Date received) {
        this.received = received;
    }

    /**
     * @return the userId
     */
    public int getUserId() {
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * @return the branch
     */
    public String getBranch() {
        return branch;
    }

    /**
     * @param branch the branch to set
     */
    public void setBranch(String branch) {
        this.branch = branch;
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
     * @return the before
     */
    public String getBefore() {
        return before;
    }

    /**
     * @param before the before to set
     */
    public void setBefore(String before) {
        this.before = before;
    }

    /**
     * @return the after
     */
    public String getAfter() {
        return after;
    }

    /**
     * @param after the after to set
     */
    public void setAfter(String after) {
        this.after = after;
    }

    /**
     * @return the mergeRequestId
     */
    public int getMergeRequestId() {
        return mergeRequestId;
    }

    /**
     * @param mergeRequestId the mergeRequestId to set
     */
    public void setMergeRequestId(int mergeRequestId) {
        this.mergeRequestId = mergeRequestId;
    }

    /**
     * @return the mergeStatusDate
     */
    public Date getMergeStatusDate() {
        return mergeStatusDate;
    }

    /**
     * @param mergeStatusDate the merge status date to set
     */
    public void setMergeStatusDate(Date mergeStatusDate) {
        this.mergeStatusDate = mergeStatusDate;
    }

    /**
     * @return the mergeStatus
     */
    public String getMergeStatus() {
        return mergeStatus;
    }

    /**
     * @param mergeStatus the mergeStatus to set
     */
    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    /**
     * @return the mergeState
     */
    public String getMergeState() {
        return mergeState;
    }

    /**
     * @param mergeState the mergeState to set
     */
    public void setMergeState(String mergeState) {
        this.mergeState = mergeState;
    }

    /**
     * @return the mergedById
     */
    public int getMergedById() {
        return mergedById;
    }

    /**
     * @param mergedById the mergedById to set
     */
    public void setMergedById(int mergedById) {
        this.mergedById = mergedById;
    }

    public static class PushMapper implements RowMapper<Push> {

        public Push map(ResultSet rs, StatementContext context) throws SQLException {

            Push push = new Push();
            push.id = rs.getInt("id");
            Timestamp ts = rs.getTimestamp("received");
            push.received = (ts != null ? new Date(ts.getTime()) : null);
            push.userId = rs.getInt("user_id");
            push.branch = rs.getString("branch");
            push.projectId = rs.getInt("project_id");
            push.before = rs.getString("before");
            push.after = rs.getString("after");
            push.mergeRequestId = rs.getInt("merge_request_id");
            ts = rs.getTimestamp("merge_status_date");
            push.mergeStatusDate = (ts != null ? new Date(ts.getTime()) : null);
            push.mergeStatus = rs.getString("merge_status");
            push.mergeState = rs.getString("merge_state");
            push.mergedById = rs.getInt("merged_by_id");
            return (push);
        }
    }
}
