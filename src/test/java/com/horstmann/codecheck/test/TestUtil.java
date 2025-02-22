package com.horstmann.codecheck.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@QuarkusTest
public class TestUtil {
    @TestHTTPResource("/")
    protected URL baseUrl;
    protected HttpClient client = HttpClient.newHttpClient();
    private ObjectMapper mapper = new ObjectMapper();

    public String resolve(String urlSuffix) throws URISyntaxException {
        return baseUrl.toURI().resolve(urlSuffix).toString();
    }

    public String getHTML(String urlSuffix) throws IOException, URISyntaxException {
        try (InputStream in = (baseUrl.toURI().resolve(urlSuffix).toURL()).openStream()) {
            String contents = new String(in.readAllBytes());
            return contents;
        }
    }

    public JsonNode postJson(String urlSuffix, String requestBody) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUrl.toURI().resolve(urlSuffix))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> responseBody = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode jsonNode = mapper.readTree(responseBody.body());
        return jsonNode;
    }

    public String postFormData(String urlSuffix, Map<String, String> data) throws URISyntaxException, IOException, InterruptedException {
        var uri = baseUrl.toURI().resolve(urlSuffix);
        String contentType = "application/x-www-form-urlencoded";
        HttpRequest.BodyPublisher publisher = ofFormData(data);
        HttpRequest request  = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", contentType)
                .POST(publisher)
                .build();

        HttpResponse<String> response
                = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public HttpRequest.BodyPublisher ofFormData(Map<? extends Object, ? extends Object> data)
    {
        boolean first = true;
        var builder = new StringBuilder();
        for (var entry : data.entrySet())
        {
            if (first) first = false;
            else builder.append("&");
            builder.append(URLEncoder.encode(entry.getKey().toString(),
                    StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(),
                    StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    public HttpRequest.BodyPublisher ofMimeMultipartData(Map<String, List<?>> data, String boundary)
            throws IOException
    {
        var bps = new ArrayList<HttpRequest.BodyPublisher>();
        var header = new StringBuilder();
        for (Map.Entry<String, List<?>> entry : data.entrySet())
        {
            for (Object value : entry.getValue())
            {
                header.append("--%s\r\nContent-Disposition: form-data; name=%s".formatted(
                        boundary, entry.getKey()));

                if (value instanceof Path path)
                {
                    header.append("; filename=\"%s\"\r\n".formatted(path.getFileName()));
                    String mimeType = Files.probeContentType(path);
                    if (mimeType != null) header.append("Content-Type: %s\r\n".formatted(mimeType));
                    header.append("\r\n");
                    bps.add(HttpRequest.BodyPublishers.ofString(header.toString()));
                    bps.add(HttpRequest.BodyPublishers.ofFile(path));
                }
                else
                {
                    header.append("\r\n\r\n%s\r\n".formatted(value));
                    header.append("\r\n");
                    bps.add(HttpRequest.BodyPublishers.ofString(header.toString()));
                }
                header = new StringBuilder("\r\n");
            }
        }
        bps.add(HttpRequest.BodyPublishers.ofString("\r\n--%s--\r\n".formatted(boundary)));
        return HttpRequest.BodyPublishers.concat(bps.toArray(HttpRequest.BodyPublisher[]::new));
    }

    private System.Logger logger = System.getLogger("com.horstmann.codecheck");
    public void log(Object... args) {
        logger.log(System.Logger.Level.INFO, Stream.of(args).map(Object::toString).collect(Collectors.joining(" ")));
    }
}
