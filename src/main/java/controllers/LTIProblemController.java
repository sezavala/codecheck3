package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import services.CodeCheck;
import oauth.signpost.exception.OAuthException;
import services.ServiceException;

import javax.script.ScriptException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@RequestScoped
@jakarta.ws.rs.Path("/")
public class LTIProblemController {
    @Inject services.LTIProblem problemService;

    @Context UriInfo uriInfo;
    @Context HttpHeaders headers;

    @POST
    @jakarta.ws.rs.Path("/lti/problem")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response launch(@QueryParam("qid") String qid,
                           MultivaluedMap<String, String> formParams) throws IOException {
        try {
            String url = HttpUtil.absolutePath(uriInfo, headers);
            Map<String, String[]> postParams = HttpUtil.paramsMap(formParams);
            String result = problemService.launch(url, qid, postParams);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/files/{repo}/{problem}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response launchCodeCheck(@PathParam("repo") String repo, @PathParam("problem") String problem,
                           @CookieParam("ccid") String ccid,
                           MultivaluedMap<String, String> formParams) throws IOException, ScriptException, NoSuchMethodException {
        try {
            String url = HttpUtil.absolutePath(uriInfo, headers);
            Map<String, String[]> postParams = HttpUtil.paramsMap(formParams);
            if (ccid == null) ccid = com.horstmann.codecheck.checker.Util.createPronouncableUID();
            String result = problemService.launchCodeCheck(url, repo, problem, ccid, postParams);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/files/{problem}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response launchCodeCheck(@PathParam("problem") String problem,
                                    @CookieParam("ccid") String ccid,
                                    MultivaluedMap<String, String> formParams) throws IOException, ScriptException, NoSuchMethodException {
        return launchCodeCheck(CodeCheck.DEFAULT_REPO, problem, ccid, formParams);
    }

    @POST
    @jakarta.ws.rs.Path("/tracer/{repo}/{problem}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response launchTracer(@PathParam("repo") String repo, @PathParam("problem") String problem,
                                    @CookieParam("ccid") String ccid,
                                    MultivaluedMap<String, String> formParams) throws IOException, ScriptException, NoSuchMethodException {
        try {
            String url = HttpUtil.absolutePath(uriInfo, headers);
            Map<String, String[]> postParams = HttpUtil.paramsMap(formParams);
            if (ccid == null) ccid = com.horstmann.codecheck.checker.Util.createPronouncableUID();
            String result = problemService.launchTracer(url, repo, problem, ccid, postParams);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/tracer/{problem}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response launchTracer(@PathParam("problem") String problem,
                                    @CookieParam("ccid") String ccid,
                                    MultivaluedMap<String, String> formParams) throws IOException, ScriptException, NoSuchMethodException {
        return launchTracer(CodeCheck.DEFAULT_REPO, problem, ccid, formParams);
    }

    @POST
    @jakarta.ws.rs.Path("/lti/send")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response send(JsonNode params) throws OAuthException, IOException, NoSuchAlgorithmException, URISyntaxException {
        try {
            ObjectNode result = problemService.send(params);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/lti/retrieve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieve(JsonNode params) throws IOException {
        try {
            ObjectNode result = problemService.retrieve(params);
            return Response.ok(result).build();
        } catch (ServiceException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}