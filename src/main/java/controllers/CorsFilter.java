package controllers;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

// https://stackoverflow.com/questions/68230460/cloud-run-http2-breaks-cors
// https://stackoverflow.com/questions/56959505/quarkus-blocked-by-cors-policy

@Provider
public class CorsFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        var origins = requestContext.getHeaders().get("Origin");
        responseContext.getHeaders().add("Access-Control-Allow-Origin", origins != null && origins.size() > 0 ? origins.get(0) : "*");
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
        //responseContext.getHeaders().add("Access-Control-Allow-Methods", requestContext.getHeaders().get("Access-Control-Request-Method").get(0));
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        //responseContext.getHeaders().add("Access-Control-Max-Age", "100000");
    }
}

