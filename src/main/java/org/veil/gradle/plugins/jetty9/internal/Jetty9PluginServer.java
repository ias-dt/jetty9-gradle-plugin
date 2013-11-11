package org.veil.gradle.plugins.jetty9.internal;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty9PluginServer <p/> Jetty9 version of a wrapper for the Server class.
 */
public class Jetty9PluginServer implements JettyPluginServer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Jetty9PluginServer.class);
	
	public static final int DEFAULT_MAX_IDLE_TIME = 30000;
	private final Server server;
    private ContextHandlerCollection contexts; //the list of ContextHandlers
    private HandlerCollection handlers; //the list of lists of Handlers
    private RequestLogHandler requestLogHandler; //the request log handler
    private DefaultHandler defaultHandler; //default handler

    private RequestLog requestLog; //the particular request log implementation
	
	public Jetty9PluginServer() {
		this.server = new Server();
		this.server.setStopAtShutdown(true);
		//make sure Jetty does not use URLConnection caches with the plugin
		Resource.setDefaultUseCaches(false);
	}

    /**
     * @see Jetty6PluginServer#setConnectors(Object[])
     */
    public void setConnectors(Object[] connectors) {
        if (connectors == null || connectors.length == 0) {
			return;
		}
		
        for (int i = 0; i < connectors.length; i++) {
            Connector connector = (Connector) connectors[i];
            LOGGER.debug("Setting Connector: " + connector.getClass().getName());
			this.server.addConnector(connector);
		}
	}

    /**
     * @see org.gradle.api.plugins.jetty.internal.JettyPluginServer#getConnectors()
     */
	public Object[] getConnectors() {
		return this.server.getConnectors();
	}
	
	public void setRequestLog(Object requestLog) {
		this.requestLog = (RequestLog)requestLog;
	}

	public Object getRequestLog() {
		return this.requestLog;
	}
	
	/**
     * @see org.gradle.api.plugins.jetty.internal.JettyPluginServer#start()
     */
    public void start() throws Exception {
        LOGGER.info("Starting jetty " + this.server.getClass().getPackage().getImplementationVersion() + " ...");
        this.server.start();
    }
	
    /**
     * @see org.gradle.api.plugins.jetty.internal.Proxy#getProxiedObject()
     */
	public Object getProxiedObject() {
		return this.server;
	}

    /**
     * @see Jetty6PluginServer#addWebApplication
     */
	public void addWebApplication(WebAppContext webapp) throws Exception {
		contexts.addHandler(webapp);
	}
	
    /**
     * Set up the handler structure to receive a webapp. Also put in a DefaultHandler so we get a nice page than a 404
     * if we hit the root and the webapp's context isn't at root.
     */
	public void configureHandlers() throws Exception {
		this.defaultHandler = new DefaultHandler();
		this.requestLogHandler = new RequestLogHandler();
		if (this.requestLog != null) {
			this.requestLogHandler.setRequestLog(this.requestLog);
		}

		this.contexts = (ContextHandlerCollection) server.getChildHandlerByClass(ContextHandlerCollection.class);
		if (this.contexts == null) {
			this.contexts = new ContextHandlerCollection();
			this.handlers = (HandlerCollection) server.getChildHandlerByClass(HandlerCollection.class);
			if (this.handlers ==null) {
				this.handlers = new HandlerCollection();
				this.server.setHandler(handlers);
				this.handlers.setHandlers(new Handler[] {this.contexts, this.defaultHandler, this.requestLogHandler});
			} else {
				this.handlers.addHandler(this.contexts);
			}
		}
	}

	public Object createDefaultConnector(int port) throws Exception {
		ServerConnector connector = new ServerConnector(this.server);
		connector.setPort(port);
		connector.setIdleTimeout(DEFAULT_MAX_IDLE_TIME);
		
		return connector;
	}

	public void join() throws Exception {
		this.server.getThreadPool().join();
	}
}
