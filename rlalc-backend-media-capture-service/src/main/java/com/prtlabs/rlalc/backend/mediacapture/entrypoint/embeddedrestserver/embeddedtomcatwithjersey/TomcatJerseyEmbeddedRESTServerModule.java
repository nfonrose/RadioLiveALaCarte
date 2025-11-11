package com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.embeddedtomcatwithjersey;

import com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.IEmbeddedRESTServerModule;
import com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.embeddedtomcatwithjersey.config.jaxrs.JerseyConfig;
import com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.embeddedtomcatwithjersey.config.openapi.OpenAPIGeneratorConfig;
import com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.embeddedtomcatwithjersey.staticcontentserving.StaticContentServlet;
import com.prtlabs.rlalc.exceptions.RLALCExceptionCodesEnum;
import com.prtlabs.utils.exceptions.PrtTechnicalRuntimeException;
import jakarta.servlet.http.HttpServlet;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TomcatJerseyEmbeddedRESTServerModule implements IEmbeddedRESTServerModule {

    private static final Logger logger = LoggerFactory.getLogger(TomcatJerseyEmbeddedRESTServerModule.class);

    @Override
    public void start(int port, String logMessagePrefix) {
        try {
            logger.info("{}EmbeddedRESTServer starting ...", (logMessagePrefix.isEmpty() ? "" : (logMessagePrefix+" ")));

            // Create a Tomcat instance and configure it to listen on port 8080
            //  - Create the Tomcat instance
            Tomcat tomcat = new Tomcat();
            //  - Create a Connector and assign it a port
            //    Remark: Cf https://stackoverflow.com/a/71506128 on why we don't just do tomcat.setPort(8287). If we did, we would
            //            still need to call tomcat.getConnector() for a connector instance to be created and declared, which is very
            //            confusing.
            Connector zeTomcatConnector = tomcat.getConnector();
            zeTomcatConnector.setPort(port);


            // Define a 'context' bound to the '/' URL and add the Servlets

            //
            //  - Jersey servlet
            //
            //    IMPORTANT: The Jersey servlet parameters are defined in the `JerseyConfig` class. This is, for instance, the
            //         case for the "jersey.config.server.provider.packages" parameter
            //
            // Define a 'context' bound to the '/' URL and add the JerseyServlet to it
            HttpServlet jerseyServlet = createJerseyServlet();
            Context jerseyServletContext = tomcat.addContext("", null);
            Wrapper jerseyServletWrapper = Tomcat.addServlet(jerseyServletContext, "jerseyServlet", jerseyServlet);
            jerseyServletContext.addServletMappingDecoded("/api/*", "jerseyServlet");
            jerseyServletWrapper.setLoadOnStartup(1);

            //
            //  - Static content serving servlet
            //
            //    REMARK: Neither EmbeddedTomcat not EmbeddedJetty can serve static files natively!!! You need to use a Servlet
            //         to handle this case. And associate that servlet with a specific prefix (static, spa, ...). The files
            //         can be served from a directory or directly from the .JAR file containing the API implementation.
            //
            //    INFORMATION: On the Angular side, you need to specific the base href when using `ng build` (for instance `--base-href=/spa/`)
            //                 to ensure that all static files are loaded using the right path.
            //                 You can also use `--output-path` to directly build the SPA into it target hosting folder
            //
            //  - Serve the static files of the Angular SPA (SinglePageApp)
            HttpServlet staticContentServlet = new StaticContentServlet();
            Wrapper staticContentServletWrapper = Tomcat.addServlet(jerseyServletContext, "staticContentServlet", staticContentServlet);
            jerseyServletContext.addServletMappingDecoded("/spa/*", "staticContentServlet");

            //
            //  - Swagger Generator configuration servlet
            //
            //    IMPORTANT: This servlet actually serves no traffic. It is only there as a configuration mechanism via its init method.
            //               That's the reason why there is no mapping declared for it. To access the OpenAPI description, you actually need
            //               to go through the Jersey endpoint.
            //
            //               In this example, this means fetching the 'openapi.json' virtual document using the '/api' prefix and
            //               not '/openapiGenerator').
            //
            //                  curl -s "http://localhost:8287/api/openapi.json" | jq .
            //
            //     REMARK: The contextPath has NO impact on the URL from which to fetch the OpenAPI description
            //
            HttpServlet openAPIGeneratorServlet = createOpenAPIGeneratorConfigServlet();
            Context openAPIGeneratorContext = tomcat.addContext("/openapiGenerator", null);
            Wrapper openAPIGeneratorServletWrapper = Tomcat.addServlet(openAPIGeneratorContext, "openAPIGeneratorConfigServlet", openAPIGeneratorServlet);
            openAPIGeneratorServletWrapper.setLoadOnStartup(2);


            // Start the Tomcat embedded server and wait for it
            tomcat.start();
            logger.info(" -> Tomcat started on http://localhost:{} (access the API description at [http://localhost:{}/api/openapi.json]) ", port, port);    // %n is like \n but which works for Windows too
            logger.info("    Ctrl+C to stop it");
            tomcat.getServer().await();
        } catch (Exception e) {
            throw new PrtTechnicalRuntimeException(RLALCExceptionCodesEnum.RLAC_009_CannotStartEmbeddedHttpRestServer.name(), e.getMessage(), e);
        }
    }


    //
    //
    // IMPLEMENTATION
    //
    //


    /**
     * Create the JerseyServlet
     * @return
     */
    private HttpServlet createJerseyServlet() {
        ResourceConfig config = new JerseyConfig();
        HttpServlet jerseyServlet = new ServletContainer(config);
        return jerseyServlet;
    }


    /**
     * Create the JerseyServlet
     * @return
     */
    private HttpServlet createOpenAPIGeneratorConfigServlet() {
        HttpServlet openAPIGeneratorConfigServlet = new OpenAPIGeneratorConfig();
        return openAPIGeneratorConfigServlet;
    }

}
