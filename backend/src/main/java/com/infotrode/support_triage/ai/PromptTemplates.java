package com.infotrode.support_triage.ai;

public final class PromptTemplates {
    private PromptTemplates() {}

    public static final String TRIAGE_V2 = "triage_v2";   // bump version
    public static final String SUMMARY_V1 = "summary_v1";

    public static final String SYSTEM = """
        You are a support triage assistant for a SaaS product.

        Return ONLY a JSON object that matches the provided JSON schema exactly.
        Do not include any extra keys.

        Hard requirements:
        - rationale MUST be non-empty (1–2 sentences).
        - tags MUST include 2–6 short, lowercase tags (no empty array).
        - If you are unsure, make a best guess based on the ticket content (do not leave fields blank).

        Important:
        - If an entity value is unknown, return an empty string "" (NOT null).
        - Keep rationale concise but informative (why category/priority/tags).
        """;

    public static final String SUMMARY_SYSTEM = """
        You are an internal support assistant.

        Return ONLY a JSON object that matches the provided JSON schema exactly.
        Do not include any extra keys.

        Rules:
        - summary: 2-4 sentences max
        - keyPoints: 3-6 short bullets
        - Do not guess unknown facts; say "Unknown" briefly if needed.
        """;

    public static String triageUserPrompt(String subject, String body, String requesterEmail, String schemaJson) {
        return """
            Triage this support ticket and return JSON only.

            Subject: %s
            Requester Email: %s
            Body:
            %s

            Guidance:
            - tags should reflect themes like: auth, billing, outage, latency, mobile, api, refund, subscription, data-loss, security, escalation, sev1, sev2
            - choose priority based on user impact and urgency

            JSON SCHEMA (must match exactly):
            %s
            """.formatted(
                safe(subject),
                safe(requesterEmail),
                safe(body),
                safe(schemaJson)
        );
    }

    public static String summaryUserPrompt(
            String subject,
            String body,
            String requesterEmail,
            String category,
            String status,
            String priority,
            String schemaJson
    ) {
        return """
            Create an internal summary for this support ticket and return JSON only.

            Subject: %s
            Requester Email: %s
            Category: %s
            Status: %s
            Priority: %s
            Body:
            %s

            JSON SCHEMA (must match exactly):
            %s
            """.formatted(
                safe(subject),
                safe(requesterEmail),
                safe(category),
                safe(status),
                safe(priority),
                safe(body),
                safe(schemaJson)
        );
    }

    private static String safe(String s) { return s == null ? "" : s.strip(); }
}
