package com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.embeddedtomcatwithjersey.config.openapi;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Servlet used to configure the Swagger/OpenAPI generation
 * 
 * Cf: https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Integration-and-Configuration#programmatic-configuration
 */
public class OpenAPIGeneratorConfig extends HttpServlet {

    private static Logger logger = LoggerFactory.getLogger(OpenAPIGeneratorConfig.class);


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            OpenAPI oas = new OpenAPI().info(new Info()
                    .title("RadioLiveALaCarte API")
                    .description("PrtLabs RadioLiveALaCarte API")
                    .termsOfService("https://tee-xprmnts.com/terms/")
                    .contact(new Contact()
                        .email("hello@groovymorning.tee-xprmt.com"))
                    .license(new License()
                        .name("MIT License")
                        .url("https://mit-license.org/"))
                    .version("1.0.0"));
            OpenAPIConfiguration oacAPIConfiguration = new SwaggerConfiguration()
                    .openAPI(oas)
                    .resourcePackages(Stream.of(
                        "com.prtlabs.rlalc.backend.mediacapture.services.management"
                        ).collect(Collectors.toSet()));

            new JaxrsOpenApiContextBuilder<>()
                    .servletConfig(config)
                    .openApiConfiguration(oacAPIConfiguration)
                    .buildContext(true);

        } catch (OpenApiConfigurationException ex) {
            logger.error("Failed to initialize configuration for OpenAPIGeneratorConfig with message=[{}]", ex.getMessage(), ex);
        }
    }
}
