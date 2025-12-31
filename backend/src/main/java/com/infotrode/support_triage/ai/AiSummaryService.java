package com.infotrode.support_triage.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotrode.support_triage.ai.dto.AiSummaryResponse;
import com.infotrode.support_triage.notes.TicketNote;
import com.infotrode.support_triage.notes.TicketNoteRepository;
import com.infotrode.support_triage.ticket.Ticket;
import com.infotrode.support_triage.ticket.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiSummaryService {

    private final TicketRepository ticketRepository;
    private final TicketNoteRepository noteRepository;
    private final AiRunRepository aiRunRepository;
    private final OllamaClient ollama;
    private final OllamaProperties props;
    private final ObjectMapper om;

    public AiSummaryService(
            TicketRepository ticketRepository,
            TicketNoteRepository noteRepository,
            AiRunRepository aiRunRepository,
            OllamaClient ollama,
            OllamaProperties props,
            ObjectMapper om
    ) {
        this.ticketRepository = ticketRepository;
        this.noteRepository = noteRepository;
        this.aiRunRepository = aiRunRepository;
        this.ollama = ollama;
        this.props = props;
        this.om = om;
    }

    @Transactional
    public AiSummaryResponse summarize(long ticketId, boolean saveAsNote) {
        Ticket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

        JsonNode schema = summarySchema();

        String schemaJson;
        try { schemaJson = om.writeValueAsString(schema); }
        catch (Exception e) { schemaJson = "{}"; }

        String prompt = PromptTemplates.summaryUserPrompt(
                t.getSubject(),
                t.getBody(),
                t.getRequesterEmail(),
                t.getCategory(),
                String.valueOf(t.getStatus()),
                String.valueOf(t.getPriority()),
                schemaJson
        );

        AiRun run = new AiRun();
        run.setTicketId(ticketId);
        run.setType("SUMMARY");
        run.setProvider("ollama");
        run.setModel(props.getModel());
        run.setPromptVersion(PromptTemplates.SUMMARY_V1);

        // input_json is NOT NULL in AiRun, so always set it.
        Map<String, Object> ticketSnapshot = new LinkedHashMap<>();
        ticketSnapshot.put("id", t.getId());
        ticketSnapshot.put("subject", t.getSubject());
        ticketSnapshot.put("requesterEmail", t.getRequesterEmail());
        ticketSnapshot.put("body", t.getBody());
        ticketSnapshot.put("status", String.valueOf(t.getStatus()));
        ticketSnapshot.put("priority", String.valueOf(t.getPriority()));
        ticketSnapshot.put("category", t.getCategory()); // can be null
        ticketSnapshot.put("tags", t.getTags());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("system", PromptTemplates.SUMMARY_SYSTEM);
        payload.put("prompt", prompt);
        payload.put("schema", schema);
        payload.put("ticketSnapshot", ticketSnapshot);
        payload.put("saveAsNote", saveAsNote);

        run.setInputJson(om.valueToTree(payload));

        Instant start = Instant.now();

        try {
            JsonNode raw = ollama.generateJson(props.getModel(), PromptTemplates.SUMMARY_SYSTEM, prompt);

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

            String summary = output.path("summary").asText("").trim();

            List<String> keyPoints = new ArrayList<>();
            JsonNode kp = output.path("keyPoints");
            if (kp.isArray()) {
                for (JsonNode n : kp) {
                    String s = n.asText("").trim();
                    if (!s.isEmpty()) keyPoints.add(s);
                }
            }

            if (summary.isEmpty()) {
                throw new RuntimeException("AI summary missing required field: summary");
            }

            // ✅ Clean up noisy placeholders like [Unknown] / unknown
            keyPoints = keyPoints.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.equalsIgnoreCase("unknown"))
                    .filter(s -> !s.equalsIgnoreCase("[unknown]"))
                    .collect(Collectors.toList());

            Long savedNoteId = null;
            if (saveAsNote) {
                TicketNote note = new TicketNote();
                note.setTicketId(ticketId);
                note.setType("ai_summary");
                note.setBody(formatNoteBody(t.getSubject(), summary, keyPoints));
                savedNoteId = noteRepository.save(note).getId();
            }

            aiRunRepository.save(run);

            return new AiSummaryResponse(ticketId, summary, keyPoints, savedNoteId);

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

            throw new RuntimeException("AI summary failed: " + msg, e);
        }
    }

    private JsonNode summarySchema() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "required", List.of("summary", "keyPoints"),
                "properties", Map.of(
                        "summary", Map.of("type", "string"),
                        "keyPoints", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")
                        )
                )
        );
        return om.valueToTree(schema);
    }

    private String formatNoteBody(String subject, String summary, List<String> keyPoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("AI Summary");
        if (subject != null && !subject.trim().isEmpty()) {
            sb.append(" — ").append(subject.trim());
        }
        sb.append("\n\n");
        sb.append(summary.trim()).append("\n");

        if (keyPoints != null && !keyPoints.isEmpty()) {
            sb.append("\nKey points:\n");
            for (String kp : keyPoints) {
                if (kp == null) continue;
                String s = kp.trim();
                if (s.isEmpty()) continue;
                sb.append("- ").append(s).append("\n");
            }
        }

        return sb.toString().trim();
    }
}
