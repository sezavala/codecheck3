package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.horstmann.codecheck.checker.Util;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import services.JWT;
import oauth.signpost.exception.OAuthException;
import services.ServiceException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@RequestScoped
@jakarta.ws.rs.Path("/")
public class LTIAssignmentController {
    @Inject services.LTIAssignment assignmentService;
    @Inject JWT jwt;
    @Context UriInfo uriInfo;
    @Context HttpHeaders headers;

    @GET
    @jakarta.ws.rs.Path("/lti/config")
    @Produces(MediaType.APPLICATION_XML)
    public Response config() throws IOException {
        String host = uriInfo.getBaseUri().getHost();
        String result = assignmentService.config(host);
        return Response.ok(result).build();
    }

    @POST
    @jakarta.ws.rs.Path("/lti/createAssignment")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response createAssignment(MultivaluedMap<String, String> formParams) throws IOException {
        try {
            String url = HttpUtil.prefix(uriInfo, headers) + uriInfo.getPath();
            Map<String, String[]> postParams = HttpUtil.paramsMap(formParams);
            String result = assignmentService.createAssignment(url, postParams);
            // TODO Shouldn't that change the assignment in the auth token?
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/lti/saveAssignment")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveAssignment(JsonNode params) throws IOException {
        try {
            String host = uriInfo.getBaseUri().getHost();
            ObjectNode result = assignmentService.saveAssignment(host, params);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @jakarta.ws.rs.Path("/lti/viewSubmissions")
    @Produces(MediaType.TEXT_HTML)
    public Response viewSubmissions(@CookieParam("ccauth") String ccauth, @QueryParam("resourceID") String resourceID) throws IOException {
        try {
            Map<String, Object> auth = jwt.verify(ccauth);
            // TODO Verify that instructor is authorized to view this assignment
            String result = assignmentService.viewSubmissions(resourceID);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @jakarta.ws.rs.Path("/lti/viewSubmission")
    @Produces(MediaType.TEXT_HTML)
    public Response viewSubmission(@CookieParam("ccauth") String ccauth, @QueryParam("resourceID") String resourceID, @QueryParam("workID") String workID) throws IOException {
        try {
            Map<String, Object> auth = jwt.verify(ccauth);
            // TODO Verify that instructor is authorized to view this assignment
            String result = assignmentService.viewSubmission(resourceID, workID);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @jakarta.ws.rs.Path("/lti/editAssignment/{assignmentID}")
    @Produces(MediaType.TEXT_HTML)
    public Response editAssignment(@CookieParam("ccauth") String ccauth, @PathParam("assignmentID") String assignmentID) throws IOException {
        try {
            Map<String, Object> auth = jwt.verify(ccauth);
            String editKey = auth.get("user").toString();
            // TODO How do we know from the assignment ID that this user is authorized to edit it?
            if (!assignmentID.equals(auth.get("assignment"))) throw new ServiceException("Not authorized");
            String result = assignmentService.editAssignment(assignmentID, editKey);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/{a:assignment|viewAssignment}/{assignmentID}") // in case someone posts a viewAssignment URL instead of cloning it
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response launch(@PathParam("assignmentID") String assignmentID,
                           MultivaluedMap<String, String> formParams) throws IOException {
        try {
            String url = HttpUtil.prefix(uriInfo, headers) + uriInfo.getPath();
            Map<String, String[]> postParams = HttpUtil.paramsMap(formParams);
            String result = assignmentService.launch(url, assignmentID, postParams);
            if (services.LTIAssignment.isInstructor(postParams)) {
                String toolConsumerID = Util.getParam(postParams, "tool_consumer_instance_guid");
                String userID = Util.getParam(postParams, "user_id");
                String userLMSID = toolConsumerID + "/" + userID;

                String ccauth = jwt.generate(Map.of("assignment", assignmentID, "user", userLMSID));
                return Response.ok(result).cookie(HttpUtil.buildCookie("ccauth", ccauth)).build();
            } else { // Student
                return Response.ok(result).build();
            }
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/lti/bridge")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response launchBridge(@QueryParam("url") String url,
                           MultivaluedMap<String, String> formParams) throws IOException {
        return launch(url, formParams);
    }

    @GET
    @jakarta.ws.rs.Path("/lti/allSubmissions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response allSubmissions(@CookieParam("ccauth") String ccauth, @QueryParam("resourceID") String resourceID) throws IOException {
        try {
            Map<String, Object> auth = jwt.verify(ccauth);
            // TODO Verify that instructor is authorized for this assignment
            ObjectNode result = assignmentService.allSubmissions(resourceID);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/lti/saveWork")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveWork(JsonNode params) throws IOException, OAuthException, NoSuchAlgorithmException, URISyntaxException {
        try {
            ObjectNode result = assignmentService.saveWork(params);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/lti/sendScore")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendScore(JsonNode params) throws IOException, OAuthException, NoSuchAlgorithmException, URISyntaxException {
        try {
            ObjectNode result = assignmentService.sendScore(params);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}