package com.prtlabs.rlalc.backend.mediacapture.entrypoint.embeddedrestserver.embeddedtomcatwithjersey.config.jaxrs;

import com.prtlabs.utils.httplogging.logascurl.PrtServerSideLogAsCurljakartaJAXRSFilter;
import org.glassfish.jersey.server.ResourceConfig;


public class JerseyConfig  extends ResourceConfig {
    
    public JerseyConfig() {

        //
        // Configure Resources exposed via Jersey
        //
        //  - Register a full package
        //    - OpenAPI generation servlet
        this.packages("io.swagger.v3.jaxrs2.integration.resources");
        this.packages("io.swagger.jaxrs.json");
        this.packages("io.swagger.jaxrs.listing");
        //    - Teevity REST Resources
        // REMARK: We don't register full packages anymore because we want full control over what's being taken into account by Jersey

        //  - Register resources one by one
        //    - Teevity REST Resources
        //    - Register server-side logAsCurl filter
        this.register(PrtServerSideLogAsCurljakartaJAXRSFilter.class);

/*
        //
        // Configure Exception Handlers to tune result returned (response) to errors
        //
        // - TeevityExceptions
        this.register(TeevityRestAPIsExceptionMapper_TeevityBaseException.class);     // TEE-BIZNESS-ERR
        // - Generic exceptions
        this.register(TeevityRestAPIsExceptionMapper_WebApplicationException.class);  // TEE-UNXPCTD-ERR-LVL2
        this.register(TeevityRestAPIsExceptionMapper_Exception.class);                // TEE-UNXPCTD-ERR-LVL1
        this.register(TeevityRestAPIsExceptionMapper_RuntimeException.class);         // TEE-UNXPCTD-ERR-LVL0
        this.register(TeevityRestAPIsExceptionMapper_Error.class);                    // TEE-UNXPCTD-ERR-LVL0
*/

    }

}
