package com.messners.gitlab.codereview.db;

import static org.junit.Assert.*;

import java.util.List;

import org.gitlab4j.codereview.dao.Push;
import org.gitlab4j.codereview.dao.PushDAO;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.Test;

public class TestPushDAO {

    @Test
    public void test() {

        JdbcConnectionPool ds = JdbcConnectionPool.create("jdbc:h2:./dbdata/simple-cr-test", "admin", "!nimda!");
        Jdbi jdbi = Jdbi.create(ds);
        jdbi.installPlugin(new SqlObjectPlugin());
        try (Handle handle = jdbi.open()) {
            PushDAO dao = handle.attach(PushDAO.class);
            List<Push> pushList = null;
            dao.dropTable();
            dao.createTable();
            dao.insert(417, 123, "new-feature", "0000000000", "01234567890");
            pushList = dao.find(417, 123, "new-feature");
            for (Push push : pushList) {
                System.out.println("id=" + push.getId());
            }
            assertNotNull(pushList);
        }
    }
}
