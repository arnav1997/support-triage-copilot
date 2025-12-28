package com.infotrode.support_triage.ai;

public final class PromptTemplates {
    private PromptTemplates() {}

    public static final String TRIAGE_V1 = "triage_v1";

    public static final String SYSTEM = """
        You are a support triage assistant for a SaaS product.
        
        Return ONLY a JSON object that matches the provided JSON schema exactly.
        Do not include any extra keys.
        
        Important:
        - If an entity value is unknown, return an empty string "" (NOT null).
        - Keep rationale short (1-2 sentences).
        """;

    public static String triageUserPrompt(String subject, String body, String requesterEmail, String schemaJson) {
        return """
            Triage this support ticket and return JSON only.

            Subject: %s
            Requester Email: %s
            Body:
            %s

            JSON SCHEMA (must match exactly):
            %s
            """.formatted(
                safe(subject),
                safe(requesterEmail),
                safe(body),
                safe(schemaJson)
        );
    }

    private static String safe(String s) { return s == null ? "" : s.strip(); }
}
