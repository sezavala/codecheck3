package controllers;

import com.horstmann.codecheck.checker.Util;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import services.Upload;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

@RequestScoped
@jakarta.ws.rs.Path("/")
public class UploadController {
    @Inject services.Upload uploadService;
    @Context UriInfo uriInfo;
    @Context HttpHeaders headers;

    @POST
    @jakarta.ws.rs.Path("/uploadFiles")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response uploadFiles(MultivaluedMap<String, String> params) {
        return uploadFiles(null, null, params);
    }

    @POST
    @jakarta.ws.rs.Path("/editedFiles/{problem}/{editKey}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response uploadFiles(@PathParam("problem") String problem, @PathParam("editKey") String editKey,
                                MultivaluedMap<String, String> params) {
        try {
            int n = 1;
            Map<Path, byte[]> problemFiles = new TreeMap<>();
            while (params.containsKey("filename" + n)) {
                String filename = params.get("filename" + n).getFirst();
                if (filename.trim().length() > 0) {
                    String contents = params.get("contents" + n).getFirst().replaceAll("\r\n", "\n");
                    problemFiles.put(Path.of(filename), contents.getBytes(StandardCharsets.UTF_8));
                }
                n++;
            }

            String response = uploadService.checkAndSaveProblem(HttpUtil.prefix(uriInfo, headers),
                    problem, problemFiles, editKey);
            return Response.ok(response).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Util.getStackTrace(ex)).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/uploadProblem")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response uploadProblem(@FormParam("file") InputStream part) {
        return uploadProblem(null, null, part);
    }

    @POST
    @jakarta.ws.rs.Path("/editedProblem/{problem}/{editKey}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response uploadProblem(@PathParam("problem") String problem, @PathParam("editKey") String editKey,
                                  @FormParam("file") InputStream part) {
        try {
            byte[] problemZip = part.readAllBytes();
            String response = uploadService.checkAndSaveProblem(HttpUtil.prefix(uriInfo, headers), problem, problemZip, editKey);
            return Response.ok(response).type(MediaType.TEXT_HTML).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Util.getStackTrace(ex)).build();
        }
    }

    @GET
    @jakarta.ws.rs.Path("/private/problem/{problem}/{editKey}")
    @Produces(MediaType.TEXT_HTML)
    public Response editProblem(@PathParam("problem") String problem, @PathParam("editKey") String editKey) {
        try {
            String response = uploadService.editProblem(HttpUtil.prefix(uriInfo, headers), problem, editKey);
            return Response.ok(response).type(MediaType.TEXT_HTML).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Util.getStackTrace(ex)).build();
        }
    }
}