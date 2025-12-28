package com.infotrode.support_triage.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class OllamaClient {

    private final RestClient rc;
    private final ObjectMapper om;

    public OllamaClient(RestClient rc, ObjectMapper om) {
        this.rc = rc;
        this.om = om;
    }

    public JsonNode generateJson(String model, String system, String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "system", system,
                "prompt", prompt,
                "stream", false,
                "format", "json"
        );

        // Read as String to avoid content-type quirks
        String raw = rc.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            return om.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Ollama response as JSON. Raw: " + raw, e);
        }
    }

    public static String extractResponseText(JsonNode json) {
        if (json == null) return null;
        return json.path("response").asText(null);
    }
}
