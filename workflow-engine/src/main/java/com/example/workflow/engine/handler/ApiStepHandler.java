package com.example.workflow.engine.handler;

import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import com.example.workflow.autoconfigure.WorkflowEngineProperties;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Map;

@Component
public class ApiStepHandler implements StepHandler {
    private final RestTemplate restTemplate;
    private final WorkflowEngineProperties properties;

    public ApiStepHandler(WorkflowEngineProperties properties) {
        this.properties = properties;
        this.restTemplate = properties.getApi().isInsecureSsl()
            ? insecureSslRestTemplate()
            : new RestTemplate();
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

    private RestTemplate insecureSslRestTemplate() {
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            final TrustManager[] trustAllManagers = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            } };
            sslContext.init(null, trustAllManagers, new java.security.SecureRandom());
            final SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            final HostnameVerifier allowAllHostnames = (host, session) -> true;

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    super.prepareConnection(connection, httpMethod);
                    if (connection instanceof HttpsURLConnection https) {
                        https.setSSLSocketFactory(socketFactory);
                        https.setHostnameVerifier(allowAllHostnames);
                    }
                }
            };
            return new RestTemplate(factory);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to initialize insecure SSL RestTemplate", ex);
        }
    }
}
