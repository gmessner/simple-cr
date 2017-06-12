package com.messners.gitlab.codereview.db;

import static org.junit.Assert.*;

import java.util.List;

import org.gitlab4j.codereview.dao.Push;
import org.gitlab4j.codereview.dao.PushDAO;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;

public class TestPushDAO {

    @Test
    public void test() {

        JdbcConnectionPool ds = JdbcConnectionPool.create("jdbc:h2:./dbdata/simple-cr-test", "admin", "!nimda!");
        DBI dbi = new DBI(ds);
        PushDAO dao = dbi.open(PushDAO.class);
        dao.dropTable();
        dao.createTable();
        dao.insert(417, 123, "new-feature", "0000000000", "01234567890");
        List<Push> pushList = dao.find(417, 123, "new-feature");
        for (Push push : pushList) {
            System.out.println("id=" + push.getId());
        }
        assertNotNull(pushList);
    }

}
