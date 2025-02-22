package controllers;

import com.horstmann.codecheck.checker.Util;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequestScoped
@jakarta.ws.rs.Path("/")
public class Health {
    @Inject
    private Config config;
    private Pattern dfPattern = Pattern.compile("(?s:.*?(?<percent>[0-9]+)%.*)");

    @GET
    @jakarta.ws.rs.Path("/")
    public Response home() {
        return Response.seeOther(URI.create(config.getString("com.horstmann.codecheck.home"))).build();
    }


        @GET
    @jakarta.ws.rs.Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public Response health() {
        String df = Util.runProcess("/bin/df /", 1000);
        Matcher matcher = dfPattern.matcher(df);
        if (matcher.matches()) {
            String percent =  matcher.group("percent");
            if (Integer.parseInt(percent) > 95) {
                String message = "disk " + percent + "% full";
                return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
            }
            Runtime rt = Runtime.getRuntime();
            double mem = rt.freeMemory() * 100.0 / rt.totalMemory();
            // TODO: Analyze output
            // http://stackoverflow.com/questions/9229333/how-to-get-overall-cpu-usage-e-g-57-on-linux
            String responseText = "CodeCheck\n" + df + "\n" + mem + "% JVM memory free\n";
            responseText += "Profile: " + config.getProperty("quarkus.profile") + "\n";
            responseText += "quarkus.http.cors: " + config.getProperty("quarkus.http.cors") + "\n";
            responseText += "quarkus.http.cors.origins: " + config.getProperty("quarkus.http.cors.origins") + "\n";
            return Response.ok(responseText).build();
        }
        else return Response.ok("df output doesn't match pattern: " + df).build();
    }
}