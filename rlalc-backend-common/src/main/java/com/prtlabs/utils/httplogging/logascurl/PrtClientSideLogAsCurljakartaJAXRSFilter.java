package com.prtlabs.utils.httplogging.logascurl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prtlabs.utils.json.PrtJsonUtils;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.spi.PostInvocationInterceptor;
import org.glassfish.jersey.client.spi.PreInvocationInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * Used to log REST HTTP requests as curl statements in the logs
 *
 * Use with:
 *     Client client = ClientBuilder.newClient(restClientConfig);
 *     client.register(TeevityLogAsCurlJaxRSInterceptor.class);
 *
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
public class PrtClientSideLogAsCurljakartaJAXRSFilter implements ClientRequestFilter, ClientResponseFilter, PreInvocationInterceptor, PostInvocationInterceptor {


	private static Logger log = LoggerFactory.getLogger(PrtClientSideLogAsCurljakartaJAXRSFilter.class);
	private static ObjectMapper mapper = PrtJsonUtils.getFasterXmlObjectMapper();


	
	/**
	 * Implementation of JAX-RS ClientRequestFilter.filter (called before every request sent by a JAX-RS Jersey Client (configured to use it))
	 */
	@Override
	public void filter(ClientRequestContext clientSideRequestContext) throws IOException {
		//log.debug("[PrtLabs/HTTPLogging/ClientSent/BEFORE-ClientRequestFilter] - " + buildCURLCommand(clientSideRequestContext));
	}
    
	
	/**
	 * Implementation of JAX-RS ClientResponseFilter.filter (called after every *successful* request sent by a JAX-RS Jersey Client (configured to use it))
	 */
	@Override
	public void filter(ClientRequestContext clientSideRequestContext, ClientResponseContext clientSideResponseContext) {
		//log.debug("[PrtLabs/HTTPLogging/ClientSent/AFTER-ClientResponseFilter] - " + buildCURLCommand(clientSideRequestContext));
	}


	/**
	 * Implementation of Jersey PreInvocationInterceptor.beforeRequest
	 */
	@Override
	public void beforeRequest(ClientRequestContext clientSideRequestContext) {
        //// TODO - Check in a dedicated AppConfig property if we should log everything or just errors.
        ////        BONUS: The AppConfig property stored in the file could be changed dynamically once
        ////               the component is already sending requests
		//if (AppConfig.shouldLogAllHttpInteractions()) {
		//	log.info("[PrtLabs/HTTPLogging/ClientSent/BEFORE-PreInvocationInterceptor] - " + buildCURLCommand(clientSideRequestContext));
		//}
	}

	
	/**
	 * Implementation of Jersey PostInvocationInterceptor.afterRequest
	 */
	@Override
	public void afterRequest(ClientRequestContext clientSideRequestContext, ClientResponseContext clientSideResponseContext) {
		//log.debug("[PrtLabs/HTTPLogging/ClientSent/AFTER-PostInvocationInterceptor-afterRequest] - " + buildCURLCommand(clientSideRequestContext) + ", clientSideResponseContext=" + clientSideResponseContext);
	}


	/**
	 * Implementation of Jersey PostInvocationInterceptor.onException
	 */
	@Override
	public void onException(ClientRequestContext clientSideRequestContext, ExceptionContext exceptionContext) {
		log.info("[PrtLabs/HTTPLogging/ClientSent/AFTER/PostInvocationInterceptor-onException] - " + buildCURLCommand(clientSideRequestContext) + "\n    -> exceptionContext=" + exceptionContext);
	}




    /**
     * Build the CURL request
     * @param serverSideRequestContext
     * @return
     */
    private String buildShortCURLCommand(ClientRequestContext requestContext) {
        String requestURI = requestContext.getUri().toString();
        
        // Build the curl command
        //  - Method
        StringBuilder curlCommand = new StringBuilder("[" + requestURI + "] ");
        curlCommand.append(requestContext.getMethod());

        return curlCommand.toString();
    }
    

    /**
     * Build the CURL request
     * @param requestContext
     * @return
     */
    private String buildCURLCommand(ClientRequestContext requestContext) {

    	String requestURI = requestContext.getUri().toString();

    	// Build the curl command
    	//  - Server
    	String serverAddress = requestContext.getUri().toString();
    	//  - Method
        StringBuilder curlCommand = new StringBuilder("[" + requestURI + "] - curl -s -X ");
        curlCommand.append(requestContext.getMethod());
        //  - Headers
        requestContext.getHeaders().forEach((name, values) -> {
            values.forEach(value -> curlCommand.append(" -H '").append(name).append(": ").append(value).append("'"));
        });
        //  - Request body
        try {
        	String requestBody = "";
	        if (requestContext.getEntity() != null) {
	            try {
	            	requestBody = mapper.writeValueAsString(requestContext.getEntity());
	            } catch (Exception ex) {
	            	log.warn("Failed to extact request body with message=[{}]", ex.getMessage());
	            }
	        }
	        if (requestBody != null && !requestBody.isEmpty()) {
	            curlCommand.append(" -d '").append(requestBody).append("'");
	        }
        } catch (Exception ex) {
        	log.warn("client-side buildCURLCommand failed to extract RequestBody with message=[]. Continuing but curlCommand might be incomplete", ex.getMessage());
        }
        //  - URL
        //requestContext.get
        curlCommand.append(" '").append(serverAddress).append("'");

        return curlCommand.toString();
    }
    



    //
    //
    // INFORMAL TEST CODE 
    //
    //

    public static void main(String[] args) {

        // Create a JaxRS ClientConfig
        ClientConfig restClientConfig = new ClientConfig();
		restClientConfig.register(new ClientRequestFilter() {
			@Override
			public void filter(ClientRequestContext requestContext) throws IOException {
				final String portalOrigin = "http://cloudcost-app:8888/api";
				requestContext.getHeaders().add("X-Teevity-Portal-Origin", portalOrigin);
			}
		});
        //restClientConfig.register(new JacksonFeature());        

        // Create a JaxRS Client and configure it
        Client client = ClientBuilder.newClient(restClientConfig);
        client.property(ClientProperties.CONNECT_TIMEOUT, 1000);
        client.property(ClientProperties.READ_TIMEOUT,    60000);
        client.register(PrtClientSideLogAsCurljakartaJAXRSFilter.class);


        // Perform an HTTP call
        //  - Create some request body
        //      GetCloudServiceInfoRequest getCloudServiceInfoRequest = new GetCloudServiceInfoRequest();
        //      String encryptedCredentialsEnrichedWithDebugValues = "customer=wile@acme.com;context=prod";
        //      getCloudServiceInfoRequest.setEncryptedCredentials(encryptedCredentialsEnrichedWithDebugValues);
        //      CloudServiceDiscoveryInfoFetchingParameters cloudServiceDiscoveryInfoFetchingParameters = new CloudServiceDiscoveryInfoFetchingParameters();
        //      getCloudServiceInfoRequest.setCloudServiceDiscoveryOptions(cloudServiceDiscoveryInfoFetchingParameters);
        //      String csp = "aws";
        //      //  - Perform the call
        //      String probeControlCenterWebAPIURL = "http://127.0.0.1:8888";
        //      String urlToCall = probeControlCenterWebAPIURL + "/cloudPlatformManagementHelperService/cloudServiceDeclarationHelper/cloudServiceInfo/details/" + csp + "/withCloudDiscoveryParameters";
        //      WebTarget resource = client.target(urlToCall);
        //      Invocation.Builder request = resource
        //          .request()
        //          .accept(MediaType.APPLICATION_JSON);
        //      Response clientResponse = request
        //          .post(Entity.entity(getCloudServiceInfoRequest, MediaType.APPLICATION_JSON));
        //
        //      @SuppressWarnings("unused")
        //      int responseStatus = clientResponse.getStatus();
    }


}
