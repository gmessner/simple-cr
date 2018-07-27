package org.gitlab4j.codereview.dao;

import java.util.List;

import org.gitlab4j.codereview.dao.ProjectConfig.ProjectConfigMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(ProjectConfigMapper.class)
public interface ProjectConfigDAO {

    @SqlUpdate("CREATE TABLE IF NOT EXISTS project_config (" +
            "  id INT AUTO_INCREMENT(1, 1) PRIMARY KEY" + 
            ", created TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ", project_id INT NOT NULL" +
            ", hook_id int NOT NULL" + 
            ", enabled BOOLEAN DEFAULT TRUE" +
            ", branch_regex VARCHAR(256)" +
            ", target_branch_regex VARCHAR(256)" +
            ", mail_to VARCHAR(16) DEFAULT 'NONE'" +
            ", additional_mail_to VARCHAR(1024)" +
            ", exclude_mail_to VARCHAR(1024)" +
            ", include_default_mail_to BOOLEAN DEFAULT FALSE);" +
            "  CREATE UNIQUE INDEX IF NOT EXISTS project_config_index ON project_config(project_id)")
    void createTable();

    @SqlUpdate("DROP TABLE IF EXISTS project_config")
    void dropTable();

    @SqlUpdate("INSERT INTO project_config" +
            " (project_id, hook_id, enabled, branch_regex, target_branch_regex, mail_to, additional_mail_to, exclude_mail_to, include_default_mail_to)" +
            " VALUES(:projectId, :hookId, :enabled, :branchRegex, :targetBranchRegex, :mailTo, :additionalMailTo, :excludeMailTo, :includeDefaultMailTo)")
    int insert(@BindBean ProjectConfig projectConfig);

    @SqlUpdate("UPDATE project_config" +
            " SET enabled = :enabled, branch_regex = :branchRegex, target_branch_regex = :targetBranchRegex, mail_to = :mailTo" +
            ", additional_mail_to = :additionalMailTo, exclude_mail_to = :excludeMailTo, include_default_mail_to = :includeDefaultMailTo" +
            " WHERE project_id = :projectId")
    int update(@BindBean ProjectConfig projectConfig);

    @SqlUpdate("DELETE FROM project_config WHERE project_id = :projectId")
    int delete(@BindBean ProjectConfig projectConfig);

    @SqlUpdate("DELETE FROM project_config WHERE project_id = :projectId")
    int delete(@Bind("projectId") int projectId);

    @SqlQuery("SELECT * from project_config WHERE project_id = :projectId")
    ProjectConfig find(@Bind("projectId") int projectId);

    @SqlQuery("SELECT * from project_config ORDER by created")
    List<ProjectConfig> list();

    @SqlUpdate("")
    void close();
}
