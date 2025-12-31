package com.infotrode.support_triage.ai;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class AiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AiExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(EntityNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "NOT_FOUND");
        body.put("message", ex.getMessage());
        return body;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleRse(ResponseStatusException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "REQUEST_FAILED");
        body.put("message", ex.getReason() == null ? ex.getMessage() : ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

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
        body.put("error", "AI_FAILED");
        body.put("message", msg);
        return body;
    }
}
