package org.cometd.primer;

import java.lang.management.ManagementFactory;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.server.CometDServlet;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
	
	
	public static final int HTTP_PORT = 8080;
	public static final int HTTPS_PORT = 8443;
	public static final String CONTEXT_PATH = "/app";
	private static final String KEY_STORE = "/src/app/resources/keystore.jks";
	private static final String KEY_STORE_PASS = "storepwd";
	private static final String KEY_STORE_MANAGER_PASS = "keypwd";
	
	
    public static void main(String[] args) throws Exception {
       // SpringApplication.run(App.class, args);
    		start();
    }
    
    
    static Server start() throws Exception {
       
        // Setup and configure the thread pool.
        QueuedThreadPool threadPool = new QueuedThreadPool(); //jetty

        // The Jetty Server instance.
        Server server = new Server(threadPool);

        // Setup and configure a connector for clear-text http:// and ws://.
        HttpConfiguration httpConfig = new HttpConfiguration();
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
       
        //http start port
        connector.setPort(HTTP_PORT);
        
        server.addConnector(connector);

        // Setup and configure a connector for https:// and wss://.
       /* SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(KEY_STORE);
        sslContextFactory.setKeyStorePassword(KEY_STORE_PASS);
        sslContextFactory.setKeyManagerPassword(KEY_STORE_MANAGER_PASS);
        
        HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        
        ServerConnector sslConnector = new ServerConnector(server, sslContextFactory, new HttpConnectionFactory(httpsConfig));
        sslConnector.setPort(HTTPS_PORT);
        server.addConnector(sslConnector);*/

        // The context where the application is deployed.
        ServletContextHandler context = new ServletContextHandler(server, CONTEXT_PATH);

        // Configure WebSocket for the context.
        WebSocketServerContainerInitializer.configureContext(context);

        // Setup JMX.
        MBeanContainer mbeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbeanContainer);
        context.setInitParameter(ServletContextHandler.MANAGED_ATTRIBUTES, BayeuxServer.ATTRIBUTE);

        // Setup the default servlet to serve static files.
        context.addServlet(DefaultServlet.class, "/");

        // Setup the CometD servlet.
        String cometdURLMapping = "/cometd/*";
        ServletHolder cometdServletHolder = new ServletHolder(CometDServlet.class);
        context.addServlet(cometdServletHolder, cometdURLMapping);
        // Required parameter for WebSocket transport configuration.
        cometdServletHolder.setInitParameter("ws.cometdURLMapping", cometdURLMapping);
        // Optional parameter for BayeuxServer configuration.
        cometdServletHolder.setInitParameter("timeout", String.valueOf(15000));
        // Start the CometD servlet eagerly to show up in JMX.
        cometdServletHolder.setInitOrder(1);

        // Add your own listeners/filters/servlets here.

        server.start();
        // end::embedded-cometd[]

        return server;
    }
    
}