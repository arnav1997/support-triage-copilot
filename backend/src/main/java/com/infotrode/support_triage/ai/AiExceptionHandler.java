package com.infotrode.support_triage.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class AiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AiExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, Object> handleAi(RuntimeException ex) {
        log.error("AI request failed", ex);

        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            Throwable cause = ex.getCause();
            msg = (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank())
                    ? cause.getClass().getSimpleName() + ": " + cause.getMessage()
                    : ex.getClass().getSimpleName();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "AI_TRIAGE_FAILED");
        body.put("message", msg);
        return body;
    }
}
