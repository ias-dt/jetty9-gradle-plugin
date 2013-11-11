/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.veil.gradle.plugins.jetty9;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veil.gradle.plugins.jetty9.internal.Jetty9PluginServer;
import org.veil.gradle.plugins.jetty9.internal.JettyPluginServer;
import org.eclipse.jetty.util.Scanner;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.gradle.api.tasks.InputFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Deploys a WAR to an embedded Jetty web container.</p>
 *
 * <p> Once started, the web container can be configured to run continuously, scanning for changes to the war file and automatically performing a hot redeploy when necessary. </p>
 */
public class JettyRunWar extends AbstractJettyRunTask {
    private static Logger logger = LoggerFactory.getLogger(JettyRunWar.class);

    /**
     * The location of the war file.
     */
    private File webApp;

    public void configureWebApplication() throws Exception {
        super.configureWebApplication();

        getWebAppConfig().setWar(getWebApp().getCanonicalPath());
    }

    public void validateConfiguration() {
    }

    /* (non-Javadoc)
    * @see AbstractJettyRunTask#configureScanner
    */
    public void configureScanner() {
        List<File> scanList = new ArrayList<File>();
        scanList.add(getProject().getBuildFile());
        scanList.add(getWebApp());
        getScanner().setScanDirs(scanList);

        List<Scanner.Listener> listeners = new ArrayList<Scanner.Listener>();
        listeners.add(new Scanner.BulkListener() {
            public void filesChanged(List changes) {
                try {
                    boolean reconfigure = changes.contains(getProject().getBuildFile().getCanonicalPath());
                    restartWebApp(reconfigure);
                } catch (Exception e) {
                    logger.error("Error reconfiguring/restarting webapp after change in watched files", e);
                }
            }
        });
        setScannerListeners(listeners);
    }

    public void restartWebApp(boolean reconfigureScanner) throws Exception {
        logger.info("Restarting webapp ...");
        logger.debug("Stopping webapp ...");
        getWebAppConfig().stop();
        logger.debug("Reconfiguring webapp ...");

        validateConfiguration();

        // check if we need to reconfigure the scanner
        if (reconfigureScanner) {
            logger.info("Reconfiguring scanner ...");
            List<File> scanList = new ArrayList<File>();
            scanList.add(getProject().getBuildFile());
            scanList.add(getWebApp());
            getScanner().setScanDirs(scanList);
        }

        logger.debug("Restarting webapp ...");
        getWebAppConfig().start();
        logger.info("Restart completed.");
    }

    public void finishConfigurationBeforeStart() {
    }

    /**
     * Returns the web application to deploy.
     */
    @InputFile
    public File getWebApp() {
        return webApp;
    }

    public void setWebApp(File webApp) {
        this.webApp = webApp;
    }

    public void applyJettyXml() throws Exception {

        if (getJettyConfig() == null) {
            return;
        }

        logger.info("Configuring Jetty from xml configuration file = {}", getJettyConfig());
        XmlConfiguration xmlConfiguration = new XmlConfiguration(getJettyConfig().toURI().toURL());
        xmlConfiguration.configure(getServer().getProxiedObject());
    }

    public JettyPluginServer createServer() throws Exception {
        return new Jetty9PluginServer();
    }
}
