package services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.script.ScriptException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.horstmann.codecheck.checker.Util;

@ApplicationScoped
public class Check {
    @Inject private CodeCheck codeCheck;

    // TODO: Legacy HTML report, used in Core Java for the Impatient 2e, 3e
    public String checkHTML(String repo, String problem, String ccid, Map<Path, String> submissionFiles)
            throws NoSuchMethodException, IOException, InterruptedException, ScriptException {
        long startTime = System.nanoTime();
        String report = codeCheck.run("HTML", repo, problem, ccid, submissionFiles);
        if (report == null || report.length() == 0) {
            double elapsed = (System.nanoTime() - startTime) / 1000000000.0;
            report = String.format("Timed out after %5.0f seconds\n", elapsed);
        }
        return report;
    }
    
    // Core Java, run with input
    public String run(Map<Path, String> submissionFiles)
            throws NoSuchMethodException, IOException, InterruptedException, ScriptException {
        long startTime = System.nanoTime();
        String report = codeCheck.run("Text", submissionFiles);
        double elapsed = (System.nanoTime() - startTime) / 1000000000.0;
        if (report == null || report.length() == 0) {
            report = String.format("Timed out after %5.0f seconds\n", elapsed);
        }
        return report;
    }

    // Core Java, run with input
    public ObjectNode runJSON(JsonNode json)
            throws NoSuchMethodException, IOException, InterruptedException, ScriptException {
        Map<Path, String> submissionFiles = new TreeMap<>();
        Iterator<Entry<String, JsonNode>> iter = json.fields();
        while (iter.hasNext()) {
            Entry<String, JsonNode> entry = iter.next();
            submissionFiles.put(Paths.get(entry.getKey()), entry.getValue().asText());         
        };
        String report = codeCheck.run("JSON", submissionFiles);
        return Util.fromJsonString(report);
    }

    // From JS UI
    public ObjectNode checkNJS(JsonNode json, String ccid)
            throws NoSuchMethodException, IOException, InterruptedException, ScriptException {
        String repo = CodeCheck.DEFAULT_REPO;
        String problem = null;
        Map<Path, String> submissionFiles = new TreeMap<>();
        ObjectNode studentWork = JsonNodeFactory.instance.objectNode();
        Map<Path, byte[]> reportZipFiles = new TreeMap<>();
        Iterator<Entry<String, JsonNode>> iter = json.fields();
        while (iter.hasNext()) {
            Entry<String, JsonNode> entry = iter.next();
            String key = entry.getKey();
            String value = entry.getValue().asText();
            if ("repo".equals(key)) repo = value;
            else if ("problem".equals(key)) problem = value;
            else {
                Path p = Paths.get(key);
                submissionFiles.put(p, value);
                reportZipFiles.put(p, value.getBytes(StandardCharsets.UTF_8));
                studentWork.set(key, entry.getValue());
            }
        }

        String report = codeCheck.run("NJS", repo, problem, ccid, submissionFiles);
        ObjectNode result = Util.fromJsonString(report);
        String reportHTML = result.get("report").asText();
        reportZipFiles.put(Paths.get("report.html"), reportHTML.getBytes(StandardCharsets.UTF_8));

        byte[] reportZipBytes = Util.zip(reportZipFiles);
        String reportZip = Base64.getEncoder().encodeToString(reportZipBytes);
        result.put("zip", reportZip);
        return result;
    }
    
    // For JS client
    public String setupReport(String repo, String problem, String ccid)
            throws NoSuchMethodException, IOException, InterruptedException, ScriptException {
        Map<Path, String> submissionFiles = new TreeMap<>();
        Map<Path, byte[]> problemFiles = codeCheck.loadProblem(repo, problem, ccid);
        for (Path key : problemFiles.keySet()) {
            String value = new String(problemFiles.get(key));
            if (key.startsWith("solution")) key = key.subpath(1, key.getNameCount());
            submissionFiles.put(key, value);
        }
        long startTime = System.nanoTime();         
        String report = codeCheck.run("Setup", repo, problem, ccid, submissionFiles);
        double elapsed = (System.nanoTime() - startTime) / 1000000000.0;
        if (report == null || report.length() == 0) {
            report = String.format("{ \"error\": \"Timed out after %5.0f seconds\"}", elapsed);
        }
        return report;
    }    
}
