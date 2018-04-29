package com.messners.gitlab.codereview.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.gitlab4j.codereview.dao.ProjectConfig;
import org.gitlab4j.codereview.dao.ProjectConfig.MailToType;
import org.gitlab4j.codereview.dao.ProjectConfigDAO;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestProjectConfigDAO {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void test() {

        JdbcConnectionPool ds = JdbcConnectionPool.create("jdbc:h2:./dbdata/simple-cr-test", "admin", "!nimda!");
        Jdbi jdbi = Jdbi.create(ds);
        jdbi.installPlugin(new SqlObjectPlugin());
        try (Handle handle = jdbi.open()) {

            ProjectConfigDAO dao = handle.attach(ProjectConfigDAO.class);
            dao.dropTable();
            dao.createTable();
            ProjectConfig projectConfig = new ProjectConfig();
            projectConfig.setProjectId(1234);
            projectConfig.setMailToType(MailToType.PROJECT);
            projectConfig.setEnabled(true);
            dao.insert(projectConfig);
            ProjectConfig projectConfig1 = dao.find(1234);
            assertNotNull(projectConfig1);
            assertTrue(projectConfig1.getProjectId() == 1234);
            assertTrue(projectConfig1.getMailToType() == MailToType.PROJECT);

            int rows = dao.delete(projectConfig1);
            assertTrue(rows == 1);

            rows = dao.insert(projectConfig);
            assertTrue(rows == 1);

            thrown.expect(UnableToExecuteStatementException.class);
            dao.insert(projectConfig);
        }
    }
}
