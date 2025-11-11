package com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.embeddedtomcatwithjersey.staticcontentserving;

import com.google.common.io.ByteStreams;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Needed to serve static resources for Tomcat. Not needed for Jetty.
 *
 * REMARK[NFO]: The code below expect the static resources to:
 *                - be located inside the Classpath
 *                - inside a subfolder called /static
 *              For instance, if you want to serve an HTML/JS/Angular Web application, you could:
 *                - store its code and assets inside `./src/main/resources/static/index.html` in this project
 *                - it will then be accessible at `http://localhost:8080/spa/index.html`
 *
 * Created by cassius on 29/04/14.
 * Modified by Nicolas Fonrose on 29/08/2024
 */
public class StaticContentServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(StaticContentServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // Turn the resourceRequestPath into a localResourcePath (cf large comment above for details)
        String resourceRequestPath = req.getPathInfo();
        if ( (resourceRequestPath == null) || ("".equals(resourceRequestPath)) || ("/".equals(resourceRequestPath)) ) {
            resourceRequestPath = "/index.html";
        }
        String localResourcePath = String.format("/static/%s", resourceRequestPath.replaceFirst("^/", ""));
        System.out.println("resourcePath=[" + req.getPathInfo() + "] -> turned into localResourcePath=[" + localResourcePath + "]");

        // Load the resource and serve it
        URL resource = getClass().getResource(localResourcePath);
        if (resource == null) {
            resp.setContentType("text/plain");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write(String.format("Not Found ()"));
            resp.getWriter().close();
        } else {
            resp.setContentType(getContentType(resourceRequestPath));
            ByteStreams.copy(getClass().getResourceAsStream(localResourcePath), resp.getOutputStream());
            resp.getOutputStream().close();
        }
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith("js")) {
            return "application/javascript";
        } else if (fileName.endsWith("css")) {
            return "text/css";
        } else if (fileName.endsWith("ico")) {
            return "image/x-icon";
        } else {
            // This code causes 'java.lang.ClassNotFoundException: com.sun.activation.registries.LogSupport' at Runtime
            //    String contentType = URLConnection.guessContentTypeFromName(fileName);
            //    return contentType != null ? contentType : FileTypeMap.getDefaultFileTypeMap().getContentType(fileName);
            return "";
        }
    }
}