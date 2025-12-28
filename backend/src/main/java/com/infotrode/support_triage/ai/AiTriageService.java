package com.infotrode.support_triage.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotrode.support_triage.ai.dto.AiTriageSuggestion;
import com.infotrode.support_triage.ticket.Ticket;
import com.infotrode.support_triage.ticket.TicketPriority;
import com.infotrode.support_triage.ticket.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiTriageService {

    private final TicketRepository ticketRepository;
    private final AiRunRepository aiRunRepository;
    private final OllamaClient ollama;
    private final OllamaProperties props;
    private final ObjectMapper om;

    public AiTriageService(
            TicketRepository ticketRepository,
            AiRunRepository aiRunRepository,
            OllamaClient ollama,
            OllamaProperties props,
            ObjectMapper om
    ) {
        this.ticketRepository = ticketRepository;
        this.aiRunRepository = aiRunRepository;
        this.ollama = ollama;
        this.props = props;
        this.om = om;
    }

    @Transactional
    public AiTriageSuggestion triage(long ticketId) {
        Ticket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

        JsonNode schema = triageSchema();

        String schemaJson;
        try { schemaJson = om.writeValueAsString(schema); }
        catch (Exception e) { schemaJson = "{}"; }

        String prompt = PromptTemplates.triageUserPrompt(
                t.getSubject(),
                t.getBody(),
                t.getRequesterEmail(),
                schemaJson
        );

        AiRun run = new AiRun();
        run.setTicketId(ticketId);
        run.setType("TRIAGE");
        run.setProvider("ollama");
        run.setModel(props.getModel());
        run.setPromptVersion(PromptTemplates.TRIAGE_V1);

        // ✅ ALWAYS set input_json (null-safe: DO NOT use Map.of)
        Map<String, Object> ticketSnapshot = new LinkedHashMap<>();
        ticketSnapshot.put("id", t.getId());
        ticketSnapshot.put("subject", t.getSubject());
        ticketSnapshot.put("requesterEmail", t.getRequesterEmail());
        ticketSnapshot.put("body", t.getBody());
        ticketSnapshot.put("status", String.valueOf(t.getStatus()));
        ticketSnapshot.put("priority", String.valueOf(t.getPriority()));
        ticketSnapshot.put("category", t.getCategory()); // can be null -> OK
        ticketSnapshot.put("tags", t.getTags());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("system", PromptTemplates.SYSTEM);
        payload.put("prompt", prompt);
        payload.put("schema", schema);
        payload.put("ticketSnapshot", ticketSnapshot);

        run.setInputJson(om.valueToTree(payload));

        Instant start = Instant.now();

        try {
            JsonNode raw = ollama.generateJson(props.getModel(), PromptTemplates.SYSTEM, prompt);

            int latencyMs = (int) Duration.between(start, Instant.now()).toMillis();
            run.setLatencyMs(latencyMs);

            String responseText = OllamaClient.extractResponseText(raw);
            if (responseText == null || responseText.isBlank()) {
                throw new RuntimeException("Ollama returned empty response text.");
            }

            JsonNode output;
            try {
                output = om.readTree(responseText);
            } catch (Exception parseEx) {
                throw new RuntimeException("Ollama response was not valid JSON: " + responseText, parseEx);
            }

            run.setOutputJson(output);
            run.setStatus(AiRun.Status.SUCCESS);

            AiTriageSuggestion parsed = om.treeToValue(output, AiTriageSuggestion.class);

            AiRun saved = aiRunRepository.save(run);

            return new AiTriageSuggestion(
                parsed.category(),
                parsed.priority(),
                parsed.tags(),
                parsed.rationale(),
                parsed.entities(),
                saved.getId()
            );

        } catch (Exception e) {
            String msg = (e.getMessage() == null || e.getMessage().isBlank())
                    ? e.getClass().getSimpleName()
                    : e.getMessage();

            run.setStatus(AiRun.Status.ERROR);
            run.setErrorMessage(msg);
            run.setOutputJson(null);
            run.setLatencyMs(null);

            // input_json already set above; extra safety:
            if (run.getInputJson() == null) {
                run.setInputJson(om.createObjectNode());
            }

            try { aiRunRepository.save(run); } catch (Exception ignored) {}

            throw new RuntimeException("AI triage failed: " + msg, e);
        }
    }

    private JsonNode triageSchema() {
        List<String> priorityEnums = Arrays.stream(TicketPriority.values())
                .map(Enum::name)
                .toList();

        Map<String, Object> schema = Map.of(
                "type", "object",
                "required", List.of("category", "priority", "tags", "rationale", "entities"),
                "properties", Map.of(
                        "category", Map.of(
                                "type", "string",
                                "enum", List.of("billing", "bug", "feature", "account", "incident", "question", "other")
                        ),
                        "priority", Map.of(
                                "type", "string",
                                "enum", priorityEnums
                        ),
                        "tags", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")
                        ),
                        "rationale", Map.of(
                                "type", "string"
                        ),
                        "entities", Map.of(
                                "type", "object",
                                "required", List.of("requesterEmail", "orderId", "product", "errorCode"),
                                "properties", Map.of(
                                        "requesterEmail", Map.of("type", "string"),
                                        "orderId", Map.of("type", "string"),
                                        "product", Map.of("type", "string"),
                                        "errorCode", Map.of("type", "string")
                                )
                        )
                )
        );

        return om.valueToTree(schema);
    }
}
