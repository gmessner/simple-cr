package org.gitlab4j.codereview.dao;

import java.util.Date;
import java.util.List;

import org.gitlab4j.codereview.dao.Push.PushMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(PushMapper.class)
public interface PushDAO {
    
    @SqlUpdate("CREATE TABLE IF NOT EXISTS push (" +
            "  id INT AUTO_INCREMENT(1, 1) PRIMARY KEY" +
            ", received TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ", user_id INT NOT NULL" +
            ", branch VARCHAR(64) NOT NULL" +
            ", project_id INT NOT NULL" +
            ", before VARCHAR(64)" +
            ", after VARCHAR(64)" +
            ", merge_request_id INT DEFAULT 0" +
            ", merge_status_date TIMESTAMP" +            
            ", merge_state VARCHAR(32)" +
            ", merge_status VARCHAR(32)" +
            ", merged_by_id INT);" +
            "  CREATE INDEX IF NOT EXISTS push_index ON push(user_id, project_id, branch, merge_request_id)")
    void createTable();
    
    @SqlUpdate("DROP TABLE IF EXISTS push")
    void dropTable();

    @SqlUpdate("INSERT INTO push (user_id, project_id, branch, before, after) values (:userId, :projectId, :branch, :before, :after)")
    int insert(@Bind("userId") int userId, @Bind("projectId") int projectId, @Bind("branch") String branch,
            @Bind("before") String before, @Bind("after") String after);
    @SqlUpdate("INSERT INTO push (user_id, project_id, branch, before, after) values (:userId, :projectId, :branch, :before, :after)")
    int insert(@BindBean Push push);
    @SqlUpdate("UPDATE push SET merge_status_date = :mergeStatusDate" +
            ", merge_status = :mergeStatus, merge_state = :mergeState, merged_by_id = :mergedById  WHERE id = :id")
    void updateMergeStatus(@Bind("id") int id, 
            @Bind("mergeStatusDate") Date mergeStatusDate, @Bind("mergeStatus") String mergeStatus,
            @Bind("mergeState") String mergeState, @Bind("mergedById") int mergedById);
    
    @SqlUpdate("UPDATE push SET merge_request_id = :mergeRequestId WHERE id = :id")
    void setMergeRequest(@Bind("id") int id, @Bind("mergeRequestId") int mergeRequestId);

    @SqlQuery("SELECT id, received, user_id, branch, project_id, before, after"
            + ", merge_request_id, merge_status_date, merge_state, merge_status, merged_by_id FROM push WHERE id = :id ORDER BY rececived DESC")
    Push find(@Bind("id") int id);
    
    @SqlQuery("SELECT id, received, user_id, branch, project_id, before, after, merge_request_id, merge_status_date, merge_state, merge_status, merged_by_id" +
            " FROM push WHERE project_id = :projectId AND branch = :branch AND user_id = :userId AND merge_request_id = :mergeRequestId" +
            " ORDER BY received DESC")
    List<Push> find(@Bind("userId") int userId, @Bind("projectId") int projectId, @Bind("branch") String branch, @Bind("mergeRequestId") int mergeRequestId);
    
    @SqlQuery("SELECT id, received, user_id, branch, project_id, before, after, merge_request_id, merge_status_date, merge_state, merge_status, merged_by_id" +
            " FROM push WHERE project_id = :projectId AND branch = :branch AND user_id = :userId ORDER BY received DESC")
    List<Push> find(@Bind("userId") int userId, @Bind("projectId") int projectId, @Bind("branch") String branch);
    
    @SqlQuery("SELECT id, received, user_id, branch, project_id, before, after, merge_request_id, merge_status_date, merge_state, merge_status, merged_by_id" +
            " FROM push WHERE project_id = :projectId AND branch = :branch AND user_id = :userId" +
            " AND merge_request_id > 0 AND merge_status IS NULL ORDER BY received DESC")
    List<Push> findPendingReviews(@Bind("userId") int userId, @Bind("projectId") int projectId, @Bind("branch") String branch);
    
    @SqlQuery("SELECT id, received, user_id, branch, project_id, before, after, merge_request_id, merge_status_date, merge_state, merge_status, merged_by_id" +
            " FROM push WHERE project_id = :projectId AND merge_request_id = :mergeRequestId ORDER BY received DESC")
    List<Push> find(@Bind("projectId") int projectId, @Bind("mergeRequestId") int mergeRequestId);

    void close();
}
