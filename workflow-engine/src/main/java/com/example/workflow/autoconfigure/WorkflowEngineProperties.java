package com.example.workflow.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflow.engine")
public class WorkflowEngineProperties {
    private boolean enabled = true;
    private Api api = new Api();
    private Ui ui = new Ui();
    private Definition definition = new Definition();
    private Execution execution = new Execution();
    private Persistence persistence = new Persistence();
    private Security security = new Security();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }
    public Ui getUi() { return ui; }
    public void setUi(Ui ui) { this.ui = ui; }
    public Definition getDefinition() { return definition; }
    public void setDefinition(Definition definition) { this.definition = definition; }
    public Execution getExecution() { return execution; }
    public void setExecution(Execution execution) { this.execution = execution; }
    public Persistence getPersistence() { return persistence; }
    public void setPersistence(Persistence persistence) { this.persistence = persistence; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public static class Api {
        private boolean enabled = true;
        private String basePath = "/workflows";
        private boolean insecureSsl = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
        public boolean isInsecureSsl() { return insecureSsl; }
        public void setInsecureSsl(boolean insecureSsl) { this.insecureSsl = insecureSsl; }
    }

    public static class Ui {
        private boolean enabled = true;
        private String path = "/workflow-ui";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public static class Definition {
        private String source = "classpath";
        private String path = "workflows";
        private boolean cacheEnabled = true;
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isCacheEnabled() { return cacheEnabled; }
        public void setCacheEnabled(boolean cacheEnabled) { this.cacheEnabled = cacheEnabled; }
    }

    public static class Execution {
        private int maxRetries = 3;
        private long retryBackoffMs = 2000;
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public long getRetryBackoffMs() { return retryBackoffMs; }
        public void setRetryBackoffMs(long retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }
    }

    public static class Persistence {
        private String type = "jpa";
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class Security {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
