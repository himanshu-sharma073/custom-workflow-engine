package com.example.hostapp.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service("documentService")
public class DocumentService {

    public Map<String, Object> quickValidate(Map<String, Object> context) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("validated", true);
        updates.put("validatedAt", Instant.now().toString());
        updates.put("validationSummary", "quick-validation-passed");
        return updates;
    }

    public Map<String, Object> publish(Map<String, Object> context) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("published", true);
        updates.put("publishedAt", Instant.now().toString());
        return updates;
    }

    public Map<String, Object> reject(Map<String, Object> context) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("published", false);
        updates.put("rejectedAt", Instant.now().toString());
        updates.put("rejectionReason", context.getOrDefault("reason", "rejected-by-workflow"));
        return updates;
    }
}
