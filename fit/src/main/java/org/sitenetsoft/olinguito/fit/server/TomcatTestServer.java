/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Copyright 2026 SiteNetSoft - Code quality improvements
 * Copyright 2026 SiteNetSoft - Replaced commons-io with Java standard library
 * Copyright 2026 SiteNetSoft - Removed servlet types from builder API
 ******************************************************************************/
package org.sitenetsoft.olinguito.fit.server;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server for integration tests using embedded Tomcat.
 */
public class TomcatTestServer implements TestServer {
  private static final Logger LOG = LoggerFactory.getLogger(TomcatTestServer.class);

  private final Tomcat tomcat;
  private final int port;

  private TomcatTestServer(final Tomcat tomcat, final int port) {
    this.tomcat = tomcat;
    this.port = port;
  }

  @Override
  public void start() throws LifecycleException {
    // Server is started via builder, this is for interface compliance
    if (tomcat.getServer().getState() != LifecycleState.STARTED) {
      tomcat.start();
    }
  }

  @Override
  public void stop() throws LifecycleException {
    if (tomcat.getServer() != null
            && tomcat.getServer().getState() != LifecycleState.DESTROYED) {
      if (tomcat.getServer().getState() != LifecycleState.STOPPED) {
        tomcat.stop();
      }
      tomcat.destroy();
    }
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public String getBaseUrl() {
    return "http://localhost:" + port;
  }

  @Override
  public boolean isRunning() {
    return tomcat.getServer() != null
            && tomcat.getServer().getState() == LifecycleState.STARTED;
  }

  private static void deleteDirectory(File dir) throws IOException {
    if (!dir.exists()) {
      return;
    }
    Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }
      @Override
      public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
        if (exc != null) {
          throw exc;
        }
        Files.delete(d);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private static void copyDirectory(File src, File dest) throws IOException {
    Path srcPath = src.toPath();
    Path destPath = dest.toPath();
    Files.walkFileTree(srcPath, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Files.createDirectories(destPath.resolve(srcPath.relativize(dir)));
        return FileVisitResult.CONTINUE;
      }
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.copy(file, destPath.resolve(srcPath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public static void main(final String[] params) throws LifecycleException {
    TomcatTestServerBuilder serverBuilder = null;
    try {
      LOG.trace("Start tomcat embedded server from main()");
      serverBuilder = TomcatTestServer.init(9180)
              .addTecsvcWebApp(true)
          .addStaticContent("/stub/StaticService/V40/OpenType.svc/$metadata", "V40/openTypeMetadata.xml")
          .addStaticContent("/stub/StaticService/V40/Demo.svc/$metadata", "V40/demoMetadata.xml")
          .addStaticContent("/stub/StaticService/V40/Static.svc/$metadata", "V40/metadata.xml");

      serverBuilder.enableLogging(Level.ALL);

      boolean keepRunning = false;
      for (String param : params) {
        if (param.equalsIgnoreCase("keeprunning")) {
          keepRunning = true;
        } else if (param.equalsIgnoreCase("addwebapp")) {
          serverBuilder.addWebApp();
        } else if (param.startsWith("port")) {
          serverBuilder.atPort(extractPortParam(param));
        }
      }

      if (keepRunning) {
        LOG.info("...and keep server running.");
        serverBuilder.startAndWait();
      } else {
        LOG.info("...and run as long as the thread is running.");
        serverBuilder.start();
      }
    } catch (IOException | LifecycleException e) {
      throw new RuntimeException("Failed to start Tomcat server from main method.", e);
    } finally {
        assert serverBuilder != null;
        serverBuilder.stop();
    }
  }

  public static int extractPortParam(final String portParameter) {
    String[] portParam = portParameter.split("=");
    if (portParam.length == 2) {
      try {
        return Integer.parseInt(portParam[1]);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Port parameter (" + portParameter + ") could not be parsed.");
      }
    }
    throw new IllegalArgumentException("Port parameter (" + portParameter + ") could not be parsed.");
  }

  public static class StaticContent extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 6850459331131987539L;
    private final String uri;
    private final String resource;

    public StaticContent(final String uri, final String resource) {
      this.uri = uri;
      this.resource = resource;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException {

      String result;
      File resourcePath = new File(resource);
      if (resourcePath.exists() && resourcePath.isFile()) {
        FileInputStream fin = new FileInputStream(resourcePath);
        result = new String(fin.readAllBytes(), StandardCharsets.UTF_8);
        LOG.info("Mapped uri '{}' to resource '{}'.", uri, resource);
        LOG.trace("Resource content {\n\n{}\n\n}", result);
      } else {
        LOG.debug("Unable to load resource for path {} as stream.", uri);
        result = "<html><head/><body>No resource for path found</body>";
      }
      resp.getOutputStream().write(result.getBytes());
    }
  }

  public static class ClasspathContent extends HttpServlet {
    @Serial
    private static final long serialVersionUID = -6663569573355398997L;
    private final String resourceName;

    public ClasspathContent(final String resourceName) {
      this.resourceName = resourceName;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException {
      resp.getOutputStream().write(
          Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName).readAllBytes());
    }
  }

  private static TomcatTestServerBuilder builder;

  public static TomcatTestServerBuilder init(final int port) {
    if (builder == null) {
      builder = new TomcatTestServerBuilder(port);
    }
    return builder;
  }

  public static class TomcatTestServerBuilder implements TestServerBuilder<TomcatTestServerBuilder> {
    private static final String TOMCAT_BASE_DIR = "tomcat-base-dir";
    private static final String PROJECT_RESOURCES_DIR = "project-resource-dir";
    private static final String PROJECT_WEB_APP_DIR = "project-web-app-dir";

    private final Tomcat tomcat;
    private final File baseDir;
    private final File resourceDir;
    private TomcatTestServer server;
    private Properties properties;

    public TomcatTestServerBuilder addTecsvcWebApp(final boolean copy) throws IOException {
      File tecsvcDir = new File("lib/server-tecsvc-servlet/target/odata-server-tecsvc-servlet-5.0.1-SNAPSHOT");
      if (!tecsvcDir.exists()) {
        throw new RuntimeException("tecsvc webapp dir not found: " + tecsvcDir.getAbsolutePath()
                + " (build module odata-server-tecsvc-servlet first)");
      }

      final File webAppDir;
      if (copy) {
        webAppDir = new File(baseDir, tecsvcDir.getName());
        deleteDirectory(webAppDir);
        copyDirectory(tecsvcDir, webAppDir);
      } else {
        webAppDir = tecsvcDir;
      }

      String contextPath = "/odata-server-tecsvc";
      Context ctx = tomcat.addWebapp(tomcat.getHost(), contextPath, webAppDir.getAbsolutePath());
      LOG.info("Tecsvc webapp {} at context {}.", webAppDir.getName(), contextPath);
      return this;
    }

    private TomcatTestServerBuilder(final int fixedPort) {
      initializeProperties();
      baseDir = getFileForDirProperty(TOMCAT_BASE_DIR);      
      if (!baseDir.exists() && !baseDir.mkdirs()) {
        throw new RuntimeException("Unable to create temporary test directory at {" + baseDir.getAbsolutePath() + "}");
      }
      resourceDir = getFileForDirProperty(PROJECT_RESOURCES_DIR);
      if(!resourceDir.exists()){
          throw new RuntimeException("Unable to load resources");
      }

      tomcat = new Tomcat();
      tomcat.getServer().setParentClassLoader(
              TomcatTestServer.class.getClassLoader()
      );
      tomcat.setBaseDir(baseDir.getParentFile().getAbsolutePath());
      tomcat.setPort(fixedPort);
      tomcat.getHost().setAppBase(baseDir.getAbsolutePath());
      tomcat.getHost().setDeployOnStartup(true);
      tomcat.getConnector().setSecure(false);
      tomcat.setSilent(true);
      tomcat.addUser("odatajclient", "odatajclient");
      tomcat.addRole("odatajclient", "odatajclient");
    }

    private void initializeProperties() {
      /*
       * The property file is build with a maven plugin (properties-maven-plugin) defined in pom.xml of the FIT module. 
       * Since the property file is build with maven its located inside the resource folder of the project.
       */
      InputStream propertiesFile =
          Thread.currentThread().getContextClassLoader().getResourceAsStream("mavenBuild.properties");
      try {
        properties = new Properties();
        properties.load(propertiesFile);
      } catch (IOException e) {
        LOG.error("Unable to load properties for embedded tomcat test server.");
        throw new RuntimeException("Unable to load properties for embedded tomcat test server.");
      }
    }

    public void enableLogging(final Level level) {
      tomcat.setSilent(false);
      try {
        Handler fileHandler = new FileHandler(tomcat.getHost().getAppBase() + "/catalina.out", true);
        fileHandler.setFormatter(new SimpleFormatter());
        fileHandler.setLevel(level);
        java.util.logging.Logger.getLogger("").addHandler(fileHandler);
      } catch (IOException e) {
        throw new RuntimeException("Unable to configure embedded tomcat logging.");
      }
    }

    public void atPort(final int port) {
      tomcat.setPort(port);
    }

    @Override
    public TomcatTestServerBuilder port(int port) {
      tomcat.setPort(port);
      return this;
    }

    public TomcatTestServerBuilder addWebApp() throws IOException {
      return addWebApp(true);
    }

    public TomcatTestServerBuilder addWebApp(final boolean copy) throws IOException {

      if (server != null) {
        return this;
      }

      File webAppProjectDir = getFileForDirProperty(PROJECT_WEB_APP_DIR);
      final File webAppDir;
      if (copy) {
        webAppDir = new File(baseDir, webAppProjectDir.getName());
        deleteDirectory(webAppDir);
        if (!webAppDir.mkdirs()) {
          throw new RuntimeException("Unable to create temporary directory at {" + webAppDir.getAbsolutePath() + "}");
        }
        copyDirectory(webAppProjectDir, webAppDir);
      } else {
        webAppDir = webAppProjectDir;
      }

      String contextPath = "/stub";

      Context context = tomcat.addWebapp(tomcat.getHost(), contextPath, webAppDir.getAbsolutePath());
      WebappLoader webappLoader = new WebappLoader();
      WebappClassLoaderBase webappClassLoaderBase =
              new WebappClassLoader(Thread.currentThread().getContextClassLoader());
      webappLoader.setLoaderInstance(webappClassLoaderBase);
      context.setLoader(webappLoader);
      LOG.info("Webapp {} at context {}.", webAppDir.getName(), contextPath);

      return this;
    }

    private File getFileForDirProperty(final String propertyName) {
      File targetFile = new File(properties.getProperty(propertyName));
      if (targetFile.exists() && targetFile.isDirectory()) {
        return targetFile;
      } else if (targetFile.mkdirs()) {
        return targetFile;
      }

      URL targetURL = Thread.currentThread().getContextClassLoader().getResource(targetFile.getPath());
      if (targetURL == null) {
        throw new RuntimeException("Project target was not found at '" +
            properties.getProperty(propertyName) + "'.");
      }
      return new File(targetURL.getFile());
    }

    @Override
    public TomcatTestServerBuilder addServlet(final String servletClassName, final String path)
        throws Exception {
      if (server != null) {
        return this;
      }
      HttpServlet httpServlet = (HttpServlet) Class.forName(servletClassName).getDeclaredConstructor().newInstance();
      Context cxt = getContext();
      String randomServletId = UUID.randomUUID().toString();
      Tomcat.addServlet(cxt, randomServletId, httpServlet);
      cxt.addServletMappingDecoded(path, randomServletId);
      LOG.info("Added servlet {} at context {} (mapping id={}).", servletClassName, path, randomServletId);
      return this;
    }

    public TomcatTestServerBuilder addAuthServlet(final String servletClassName,
            final String servletPath, final String contextPath)
        throws Exception {
      if (server != null) {
        return this;
      }
      final String TOMCAT_WEB_XML = "web.xml";
      String webXMLPath = Objects.requireNonNull(
          Thread.currentThread().getContextClassLoader().getResource(TOMCAT_WEB_XML)).getPath();
      HttpServlet httpServlet = (HttpServlet) Class.forName(servletClassName).getDeclaredConstructor().newInstance();
      Context cxt = tomcat.addWebapp(servletPath, baseDir.getAbsolutePath());
      cxt.setAltDDName(webXMLPath);
      String randomServletId = UUID.randomUUID().toString();
      Tomcat.addServlet(cxt, randomServletId, httpServlet);
      cxt.addServletMappingDecoded(contextPath, randomServletId);

      return this;
    }

    @Override
    public TomcatTestServerBuilder addStaticContent(final String uri, final String resourceName) throws IOException {
      String resource = new File(resourceDir, resourceName).getAbsolutePath();
      LOG.info("Added static content from '{}' at uri '{}'.", resource, uri);
      StaticContent staticContent = new StaticContent(uri, resource);
      return addServletInstance(staticContent, String.valueOf(uri.hashCode()), uri);
    }

    @Override
    public TomcatTestServerBuilder addClasspathContent(final String uri, final String resourceName) {
      LOG.info("Added classpath content '{}' at uri '{}'.", resourceName, uri);
      ClasspathContent classpathContent = new ClasspathContent(resourceName);
      return addServletInstance(classpathContent, String.valueOf(uri.hashCode()), uri);
    }

    @Override
    public TomcatTestServerBuilder addServletInstance(final Object servlet, final String path) {
      String name = UUID.randomUUID().toString();
      return addServletInstance(servlet, name, path);
    }

    public TomcatTestServerBuilder addServletInstance(final Object servlet, final String name, final String path) {
      if (server != null) {
        return this;
      }
      Context cxt = getContext();
      Tomcat.addServlet(cxt, name, (HttpServlet) servlet);
      cxt.addServletMappingDecoded(path, name);
      LOG.info("Added servlet {} at context {}.", name, path);
      return this;
    }

    private Context baseContext = null;

    private Context getContext() {
      if (baseContext == null) {
        baseContext = tomcat.addContext("", baseDir.getAbsolutePath());
      }
      return baseContext;
    }

    @Override
    public TestServer build() throws LifecycleException {
      return start();
    }

    public TomcatTestServer start() throws LifecycleException {
      if (server != null) {
        return server;
      }
      if (baseContext != null) {
        baseContext.addApplicationListener(SessionHolder.class.getName());
      }
      tomcat.start();

      int actualPort = tomcat.getConnector().getPort();
      LOG.info("Started server at endpoint {}:{} (with base dir: {}",
          tomcat.getServer().getAddress(), actualPort, baseDir.getAbsolutePath());

      server = new TomcatTestServer(tomcat, actualPort);
      return server;
    }

    public void startAndWait() throws LifecycleException {
      start();
      tomcat.getServer().await();
    }

    public void stop() throws LifecycleException {
      if (tomcat.getServer() != null
              && tomcat.getServer().getState() != LifecycleState.DESTROYED) {
        if (tomcat.getServer().getState() != LifecycleState.STOPPED) {
          tomcat.stop();
        }
        tomcat.destroy();
      }
    }
  }

  @Override
  public void invalidateAllSessions() {
    SessionHolder.invalidateAllSession();
  }

  public static class SessionHolder implements HttpSessionListener {

    private static final Map<ServletContext, Set<HttpSession>> ALL_SESSIONS =
            Collections.synchronizedMap(new HashMap<>());

    @Override
    public void sessionCreated(HttpSessionEvent se) {
      LOG.info("Created session: {}", se);

      ServletContext c = se.getSession().getServletContext();
        Set<HttpSession> set = ALL_SESSIONS.computeIfAbsent(c, k -> new HashSet<>());
        set.add(se.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
      LOG.info("Destroy session: {}", se);
    }

    public static void invalidateAllSession() {
      synchronized (ALL_SESSIONS) {
        LOG.info("Invalidated sessions...");
        for (Map.Entry<ServletContext, Set<HttpSession>> e : ALL_SESSIONS.entrySet()) {
          for (HttpSession s : e.getValue()) {
            s.invalidate();
          }
        }
        ALL_SESSIONS.clear();
        LOG.info("...Invalidated all sessions.");
      }
    }
  }
}