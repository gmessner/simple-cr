package org.gitlab4j.codereview;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.webhook.WebHookManager;
import org.gitlab4j.codereview.dao.ProjectConfigDAO;
import org.gitlab4j.codereview.dao.PushDAO;
import org.gitlab4j.codereview.server.DefaultEmbeddedServer;
import org.gitlab4j.codereview.server.EmbeddedServer;
import org.gitlab4j.codereview.server.EmbeddedServerWithSsl;
import org.h2.jdbcx.JdbcConnectionPool;
import org.skife.jdbi.v2.DBI;

public class CodeReviewServer {

    static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");
    }
    private static Log log = LogFactory.getLog(CodeReviewServer.class);

    private CodeReviewConfiguration config;
    private EmbeddedServer server;
    private GitLabApi gitlabApi;
    private JdbcConnectionPool connectionPool;
    private WebHookManager webHookManager;

    private CodeReviewServer() {

        try {
            config = new CodeReviewConfiguration("simple-cr.properties");
        } catch (Exception e) {
            config = new CodeReviewConfiguration();
        }
    }

    private boolean processCommandLine(String[] args) {
        return (config.processCommandLine(args));
    }

    private void init() throws Exception {

        gitlabApi = new GitLabApi(config.getGitLabApiUrl(), config.getGitLabApiToken());
        initializeDatabase();

        if (config.getSslPort() < 1) {
            server = new DefaultEmbeddedServer(config.getPath(), config.getPort());
        } else {
            server = new EmbeddedServerWithSsl(config.getPath(), config.getPort(), config.getSslPort());
        }
        
        webHookManager = new WebHookManager();

        server.setAttribute(EmbeddedServer.CONFIG, config);
        server.setAttribute(EmbeddedServer.GITLAB_API, gitlabApi);
        server.setAttribute(EmbeddedServer.CONNECTION_POOL, connectionPool);
        server.setAttribute(WebHookManager.class.getSimpleName(), webHookManager);
        
        CodeReviewWebHookListener handler = new CodeReviewWebHookListener(config, gitlabApi, connectionPool);
        webHookManager.addListener(handler);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {

                try {
                    log.info("Waiting for Simple-CR server to stop");
                    server.stop();
                    server.join();
                } catch (Exception e) {
                }

            }
        });
    }

    private void initializeDatabase() {

        connectionPool = JdbcConnectionPool.create("jdbc:h2:" + config.getDbName(), config.getDbUser(), config.getDbPassword());
        DBI dbi = new DBI(connectionPool);
        PushDAO pushDao = dbi.open(PushDAO.class);
        pushDao.createTable();
        ProjectConfigDAO projectConfigDao = dbi.open(ProjectConfigDAO.class);
        projectConfigDao.createTable();
    }

    private EmbeddedServer getServer() {
        return (server);
    }

    private boolean isInteractive() {
        return (config.isInteractive());
    }

    public static void main(String[] args) throws Exception {

        CodeReviewServer codeReviewServer = new CodeReviewServer();
        if (!codeReviewServer.processCommandLine(args)) {
            System.exit(1);
        }

        log.info("Starting Simple-CR server");
        codeReviewServer.init();

        EmbeddedServer server = codeReviewServer.getServer();
        server.start();
        log.info("Simple-CR server started and ready");

        if (codeReviewServer.isInteractive()) {

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            while (true) {

                System.out.println("Enter \"stop\" to terminate server: ");
                String line;

                line = console.readLine();

                if (line != null && "stop".equalsIgnoreCase(line)) {
                    break;
                }
            }

            log.info("Waiting for Simple-CR server to stop");
            server.stop();
        }

        server.join();
    }
}
