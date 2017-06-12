package org.gitlab4j.codereview.server;

import java.net.URL;
import java.security.ProtectionDomain;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.webapp.WebAppContext;

public class DefaultEmbeddedServer implements EmbeddedServer {

    private Server server;
    private WebAppContext servletContext;

    public DefaultEmbeddedServer(String contextPath, int port) throws Exception {

        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);

        server.setConnectors(new Connector[] { connector });

        servletContext = new WebAppContext();
        servletContext.setServer(server);
        servletContext.setContextPath(contextPath == null ? "/" : contextPath);

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(false);
        resource_handler.setWelcomeFiles(new String[] { "index.html" });
        resource_handler.setResourceBase(".");
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resource_handler, new DefaultHandler() });
        server.setHandler(handlers);

        ProtectionDomain protectionDomain = DefaultEmbeddedServer.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        servletContext.setWar(location.toExternalForm());

        server.setHandler(servletContext);
    }

    @Override
    public void start() throws Exception {
        server.start();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public void join() throws Exception {
        server.join();
    }

    @Override
    public void setAttribute(String name, Object value) {
        servletContext.setAttribute(name, value);
    }
}
