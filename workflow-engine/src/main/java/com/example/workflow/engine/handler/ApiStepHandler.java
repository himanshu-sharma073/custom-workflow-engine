package com.example.workflow.engine.handler;

import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import com.example.workflow.autoconfigure.WorkflowEngineProperties;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.Map;

@Component
public class ApiStepHandler implements StepHandler {
    private final RestTemplate restTemplate = new RestTemplate();
    private final WorkflowEngineProperties properties;

    public ApiStepHandler(WorkflowEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public StepType supports() { return StepType.API; }

    @Override
    public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
        String url = step.url();
        for (Map.Entry<String, Object> e : context.variables().entrySet()) {
            url = url.replace("${" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        HttpMethod method = step.method() == null ? HttpMethod.GET : HttpMethod.valueOf(step.method().toUpperCase(Locale.ROOT));
        HttpHeaders headers = new HttpHeaders();
        if (step.headers() != null) {
            step.headers().forEach(headers::add);
        }
        int attempts = 0;
        int max = step.retryAttempts() == null ? Math.max(1, properties.getExecution().getMaxRetries()) : Math.max(1, step.retryAttempts());
        while (attempts < max) {
            attempts++;
            try {
                ResponseEntity<String> response = restTemplate.exchange(url, method, new HttpEntity<>(step.body(), headers), String.class);
                context.variables().put("lastApiResponse", response.getBody());
                context.variables().put("lastApiStatus", response.getStatusCode().value());
                context.variables().put("lastApiAttempt", attempts);
                if (step.responseMapping() != null && response.getBody() != null) {
                    for (Map.Entry<String, String> mapping : step.responseMapping().entrySet()) {
                        Object mapped = JsonPath.read(response.getBody(), mapping.getValue());
                        context.variables().put(mapping.getKey(), mapped);
                    }
                }
                context.appendHistory(step, "API_SUCCESS");
                return step.next() == null ? StepExecutionResult.endState() : StepExecutionResult.next(step.next());
            } catch (Exception ex) {
                context.variables().put("lastApiError", ex.getMessage());
                context.variables().put("lastApiAttempt", attempts);
                context.appendHistory(step, "API_RETRY_" + attempts);
                if (attempts < max) {
                    try {
                        Thread.sleep(properties.getExecution().getRetryBackoffMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        context.appendHistory(step, "API_FAILED");
        return StepExecutionResult.endState();
    }
}
