package com.infotrode.support_triage.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotrode.support_triage.ai.dto.AiReplyDraftResponse;
import com.infotrode.support_triage.ticket.Ticket;
import com.infotrode.support_triage.ticket.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiReplyDraftService {

    private final TicketRepository ticketRepository;
    private final AiRunRepository aiRunRepository;
    private final OllamaClient ollama;
    private final OllamaProperties props;
    private final ObjectMapper om;

    public AiReplyDraftService(
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
    public AiReplyDraftResponse draftReply(long ticketId, ReplyTone tone) {
        Ticket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

        JsonNode schema = replySchema();

        String schemaJson;
        try { schemaJson = om.writeValueAsString(schema); }
        catch (Exception e) { schemaJson = "{}"; }

        String prompt = PromptTemplates.replyDraftUserPrompt(
                t.getSubject(),
                t.getBody(),
                t.getRequesterEmail(),
                t.getCategory(),
                String.valueOf(t.getStatus()),
                String.valueOf(t.getPriority()),
                (t.getTags() == null ? List.<String>of() : t.getTags()),
                tone,
                schemaJson
        );

        AiRun run = new AiRun();
        run.setTicketId(ticketId);
        run.setType("REPLY_DRAFT");
        run.setProvider("ollama");
        run.setModel(props.getModel());
        run.setPromptVersion(PromptTemplates.REPLY_V1);

        // ticket snapshot
        Map<String, Object> ticketSnapshot = new LinkedHashMap<>();
        ticketSnapshot.put("id", t.getId());
        ticketSnapshot.put("subject", t.getSubject());
        ticketSnapshot.put("requesterEmail", t.getRequesterEmail());
        ticketSnapshot.put("body", t.getBody());
        ticketSnapshot.put("status", String.valueOf(t.getStatus()));
        ticketSnapshot.put("priority", String.valueOf(t.getPriority()));
        ticketSnapshot.put("category", t.getCategory());
        ticketSnapshot.put("tags", t.getTags());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("system", PromptTemplates.REPLY_SYSTEM);
        payload.put("prompt", prompt);
        payload.put("schema", schema);
        payload.put("ticketSnapshot", ticketSnapshot);
        payload.put("tone", tone.name());

        run.setInputJson(om.valueToTree(payload));

        Instant start = Instant.now();

        try {
            JsonNode raw = ollama.generateJson(props.getModel(), PromptTemplates.REPLY_SYSTEM, prompt);

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

            String draft = output.path("draft").asText("").trim();
            if (draft.isEmpty()) {
                throw new RuntimeException("AI reply draft missing required field: draft");
            }

            // tiny cleanup: avoid trailing spaces / keep within reason
            draft = draft.replaceAll("[ \\t]+\\n", "\n").trim();

            AiRun saved = aiRunRepository.save(run);

            return new AiReplyDraftResponse(ticketId, tone, draft, saved.getId());

        } catch (Exception e) {
            String msg = (e.getMessage() == null || e.getMessage().isBlank())
                    ? e.getClass().getSimpleName()
                    : e.getMessage();

            run.setStatus(AiRun.Status.ERROR);
            run.setErrorMessage(msg);
            run.setOutputJson(null);
            run.setLatencyMs(null);

            if (run.getInputJson() == null) {
                run.setInputJson(om.createObjectNode());
            }

            try { aiRunRepository.save(run); } catch (Exception ignored) {}

            throw new RuntimeException("AI reply draft failed: " + msg, e);
        }
    }

    private JsonNode replySchema() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "required", List.of("draft"),
                "properties", Map.of(
                        "draft", Map.of(
                                "type", "string",
                                "minLength", 20,
                                "maxLength", 2500
                        )
                )
        );
        return om.valueToTree(schema);
    }
}
