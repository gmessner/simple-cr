package org.gitlab4j.codereview.dao;

import java.util.List;

import org.gitlab4j.codereview.dao.ProjectConfig.ProjectConfigMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

@RegisterMapper(ProjectConfigMapper.class)
public interface ProjectConfigDAO {

    @SqlUpdate("CREATE TABLE IF NOT EXISTS project_config (" +
            "  id INT AUTO_INCREMENT(1, 1) PRIMARY KEY" + 
            ", created TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ", project_id INT NOT NULL" + 
            ", hook_id int NOT NULL" + 
            ", enabled BOOLEAN DEFAULT TRUE" + ", mail_to VARCHAR(16) DEFAULT 'NONE'" +
            ", additional_mail_to VARCHAR(1024)" +
            ", exclude_mail_to VARCHAR(1024)" +
            ", include_default_mail_to BOOLEAN DEFAULT FALSE);" +
            "  CREATE UNIQUE INDEX IF NOT EXISTS project_config_index ON project_config(project_id)")
    void createTable();

    @SqlUpdate("DROP TABLE IF EXISTS project_config")
    void dropTable();

    @SqlUpdate("INSERT INTO project_config" +
            " (project_id, hook_id, enabled, mail_to, additional_mail_to, exclude_mail_to, include_default_mail_to)" +
            " VALUES(:projectId, :hookId, :enabled, :mailTo, :additionalMailTo, :excludeMailTo, :includeDefaultMailTo)")
    int insert(@BindBean ProjectConfig projectConfig);

    @SqlUpdate("UPDATE project_config (enabled, mail_to, additional_mail_to, exclude_mail_to, include_default_mail_to)" +
            " VALUES(:enabled, :mailTo, :additionalMailTo, :excludeMailTo, :includeDefaultMailTo)")
    int update(@BindBean ProjectConfig projectConfig);

    @SqlUpdate("DELETE FROM project_config WHERE project_id = :projectId")
    int delete(@BindBean ProjectConfig projectConfig);

    @SqlUpdate("DELETE FROM project_config WHERE project_id = :projectId")
    int delete(@Bind("projectId") int projectId);

    @SqlQuery("SELECT * from project_config WHERE project_id = :projectId")
    ProjectConfig find(@Bind("projectId") int projectId);

    @SqlQuery("SELECT * from project_config ORDER by created")
    List<ProjectConfig> list();

    void close();
}
