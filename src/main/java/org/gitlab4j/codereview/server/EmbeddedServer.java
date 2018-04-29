package org.gitlab4j.codereview.server;

public interface EmbeddedServer {

    public static final String GITLAB_API = "gitlab-api";
    public static final String CONFIG = "config";
    public static final String CONNECTION_POOL = "connection-pool";
    public static final String JDBI = "jdbi";
    public static final String WEBHOOK_MANAGER = "webhook-manager";

    public abstract void start() throws Exception;

    public abstract void stop() throws Exception;

    public abstract void join() throws Exception;

    public void setAttribute(String name, Object value);
}