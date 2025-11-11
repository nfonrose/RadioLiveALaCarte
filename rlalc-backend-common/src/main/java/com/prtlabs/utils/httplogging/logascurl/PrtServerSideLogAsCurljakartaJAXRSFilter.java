package com.prtlabs.utils.httplogging.logascurl;

import com.google.common.base.Joiner;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


/**
 * Used to log REST HTTP requests as curl statements in the logs
 *
 * Use with:
 *
 *   - For a web.xml based Jersey configuration (Portal)
 *
 *         <init-param>
 *             <param-name>jersey.config.server.provider.packages</param-name>
 *             <!-- REMARK: The 'com.teevity.utils.httplogging.logascurl' part of for TeevityServerSideLogAsCurlJAXRSFilter -->             
 *             <param-value>
 *                 io.swagger.jaxrs.json,
 *                 io.swagger.jaxrs.listing,
 *                 com.teevity,
 *                 com.teevity.utils.httplogging.logascurl
 *             </param-value>
 *             <!-- <param-value>com.teevity, dbmanagementscripts, test.com.teevity</param-value> -->
 *         </init-param>
 *
 *   - For an Server Embedded configuration (ProbeControlCenter REST Service)
 *
 *         public class JerseyConfig  extends ResourceConfig {
 *             
 *             public JerseyConfig() {
 *         
 *                 //    - Teevity REST Resources
 *                 this.packages("com.teevity");
 *                 //  - Register resources one by one
 *                 this.register(CloudPlatformManagementHelperService.class);
 *                 this.register(HistoricalChargesAndUsageFetcherService.class);
 *         
 *                 //  - Register server-side logAsCurl filter
 *                 this.register(TeevityServerSideLogAsCurlJAXRSFilter.class);
 *             }
 *         
 *         }
 *
 * Inspiration from https://stackoverflow.com/a/27578248/1403417 
 *   
 *      A ContainerRequestFilter (Server Side) example would be to do some authorization/authentication, 
 *      which a pretty common use case for server side filter. The filter will be invoked before reaching any of your resources
 * 
 *          Client --->  Internet ---> Server ---> Filter ---> Resource
 *          
 *      A ClientRequestFilter (Client Side) example would be implement some client side cache (sort of mocking a browser cache). 
 *      Or a a case (which already has been implemented) is a filter to encode a user name and password for BASIC authentication. 
 *      Before the request actually gets sent to the server, the client filter will get invoked.
 *      
 *          Client ---> Filter ---> Internet ---> Server ---> Resource
 *          
 * 
 *   IMPORTANT
 * 
 *    These classes are here (and not in Teevity-Core-Common) because they are `jakarta.*` based (and not `javax.*`).
 *    And the whole Teevity project cannot move to `jakarta.*` because of the Portal living on 'AppEngine Standard Java with Bundled Service'
 * 
 * 
 */
@Provider
public class PrtServerSideLogAsCurljakartaJAXRSFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static Logger log = LoggerFactory.getLogger(PrtServerSideLogAsCurljakartaJAXRSFilter.class);
	
	
    /**
     * Called BEFORE every request received by the JAX-RS Jersey Servlet
     * @param requestContext
     * @param responseContext
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext serverSideRequestContext) throws IOException {
    	// Some requests should never be loggedAsCurl (for performance reason, for instance)
		if (!filteredOutRequest(serverSideRequestContext)) {
			// Save the 'prebuiltLoggedAsCurlCommand' in case the request is a failure and we want to log it later
			// REMARK: We can't build the 'curl command' in the "after request filter" because the request entity (aka body)
			//         has already been consumed by Jersey  
	    	String curlCommand = buildCURLCommand(serverSideRequestContext);
			serverSideRequestContext.setProperty("savedLoggedAsCurlCommand", curlCommand);
	
	    	//// Check in a dedicated AppConfig property if we should log everything or just errors.
	        //// BONUS: The AppConfig property stored in the file could be changed dynamically once
	        ////        the component is already serving requests
	    	//if (AppConfig.shouldLogAllHttpInteractions()) {
	    	//	// And either log it or save it for later (after request check)
			//	log.info("[Teevity/HTTPLogging/ServerReceived] - " + curlCommand);
	    	//}
		}
    }
    
    
    /**
     * Called AFTER every request received by the JAX-RS Jersey Servlet
     * @param requestContext
     * @param responseContext
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext serverSideRequestContext, ContainerResponseContext serverSideResponseContext) throws IOException {
        boolean hasRequestBeenSuccessful = (serverSideResponseContext.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL);
        if (!hasRequestBeenSuccessful) {
        	// Look for the curlCommand which has been previously built, if it has
            Object savedLoggedAsCurlCommand = serverSideRequestContext.getProperty("savedLoggedAsCurlCommand");
        	String previouslyBuiltLoggedAsCurlCommand = (savedLoggedAsCurlCommand==null) ? "<Failed to get 'savedLoggedAsCurlCommand'>" : savedLoggedAsCurlCommand.toString();
        	if (previouslyBuiltLoggedAsCurlCommand != null) {
        		log.warn("[Teevity/HTTPLogging/ServerReceived/requestFailed] - " + previouslyBuiltLoggedAsCurlCommand);
        	}
        }
    }
    
    
    
    
    
    
    
    
    
    //
    //
    // IMPLEMENTATION
    //
    //
    

    /**
     * Return true if a request should not be logged as curl
     * @param serverSideRequestContext
     * @return
     */
    private boolean filteredOutRequest(ContainerRequestContext serverSideRequestContext) {
    	String path = serverSideRequestContext.getUriInfo().getPath();
		// Nothing to filter out with the current users of this class
    	// Remark: It should be done like this: 
    	//             if (path.startsWith("externalFeatures/getProperty/forFeature/detailedcostanalytics.aws.ice/forLoggedCustomer/propertyName/detailedcostanalytics.status")) return true;
		return false;
	}
    

    /**
     * Build the CURL request
     * @param serverSideRequestContext
     * @return
     */
    private String buildShortCURLCommand(ContainerRequestContext serverSideRequestContext) {
        String requestURI = serverSideRequestContext.getUriInfo().getPath();
        
        // Build the curl command
        //  - Method
        StringBuilder curlCommand = new StringBuilder("[" + requestURI + "] - curl/");
        curlCommand.append(serverSideRequestContext.getMethod());

        return curlCommand.toString();
    }
    

    /**
     * Build the CURL request
     * @param serverSideRequestContext
     * @return
     */
    private String buildCURLCommand(ContainerRequestContext serverSideRequestContext) {

    	String requestURI = serverSideRequestContext.getUriInfo().getPath();
    	
    	// Build the curl command
    	//  - Server
    	String serverAddress = serverSideRequestContext.getUriInfo().getAbsolutePath().toString();
    	//  - Method
        StringBuilder curlCommand = new StringBuilder("[" + requestURI + "] - curl -s -X ");
        curlCommand.append(serverSideRequestContext.getMethod());
        //  - Headers
        serverSideRequestContext.getHeaders().forEach((name, values) -> {
        	// We want to exclude some cookies or some values from the log:
        	//  - User-Agent which does not add value to replay API based interactions
        	//  - Google Analytics cookies which are useless to replay API based interactions
        	if (!"User-Agent".equals(name)) {
        		values.forEach(value -> curlCommand.append(" -H '").append(name).append(": ").append(stripValueOfUTMCookies(value, name)).append("'"));
        	}
        });
        //  - Request body
        String requestBody = "";
        if (serverSideRequestContext.hasEntity()) {
            try {                
                // Consume the body of the request (to be able to log it)
                // REMARK: This means that it will need to be reinjected in the request so that Jersey can see it
                //
                //     We tried to use the mark()/reset() methods of the Stream but it didn't work
                //
                //     //  - First 'mark' the steam to be able to reset it just after the logAsCurl
                //     int streamResetMaxSize = 2*1024*1024;    // max 2Mb (it's the max buffer size used to re-emit the stream)
                //     serverSideRequestContext.getEntityStream().mark(streamResetMaxSize);
                //    
                //     ... read and process the body
                //    
                //     //  - Reset the steam to the beginning (so that Jersey can actually process it)
                //     serverSideRequestContext.getEntityStream().reset();
                //
                //  - Extract the content/body of the HTTP request
            	List<String> httpBodyLines = IOUtils.readLines(serverSideRequestContext.getEntityStream(), Charset.defaultCharset());
                requestBody = Joiner.on("").join(httpBodyLines);
                // Re-inject the consumed request body back into the request
                serverSideRequestContext.setEntityStream(convertToInputStream(httpBodyLines));
            } catch (Exception ex) {
            	log.warn("Failed to extact request body with message=[{}]", ex.getMessage());
            }
        }
        if (requestBody != null && !requestBody.isEmpty()) {
            curlCommand.append(" -d '").append(requestBody).append("'");
        }
        //  - URL
        //requestContext.get
        curlCommand.append(" '").append(serverAddress).append("'");

        return curlCommand.toString();
    }
    

    String stripValueOfUTMCookies(String cookieValue, String headerName) {
    	if ("Cookie".equals(headerName)) {
    		return removeGoogleAnalyticsCookies(cookieValue);
    	} else {
    		String valueWhichWasNotACookie = cookieValue;
    		return valueWhichWasNotACookie;
    	}
    }
    
    /**
     * Created using this ChatGPT prompt (I need to write a Java function that can take an HTTP Cookie value like this one and remove all the Google Analytics related cookies from it, while preserving the other ones) 
     * @param cookieValue
     * @return
     */
    public static String removeGoogleAnalyticsCookies(String cookieValue) {
        // Split the cookie string into individual cookies
        List<String> cookies = Arrays.asList(cookieValue.split(";"));

        // Filter out Google Analytics related cookies
        List<String> filteredCookies = cookies.stream()
                .filter(cookie -> !cookie.trim().startsWith("__utm"))
                .collect(java.util.stream.Collectors.toList());

        // Join the filtered cookies to reconstruct the modified cookie string
        String modifiedCookieValue = String.join("; ", filteredCookies);

        return modifiedCookieValue;
    }


    public static ByteArrayInputStream convertToInputStream(List<String> httpBodyLines) {
        // Join the list of strings to form the complete request body
        String requestBody = String.join("\n", httpBodyLines);
        // Convert the string to a byte array
        byte[] requestBodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
        // Create a ByteArrayInputStream from the byte array
        return new ByteArrayInputStream(requestBodyBytes);
    }

}
