package com.example.workflow.definition.catalog;

import com.example.workflow.autoconfigure.WorkflowEngineProperties;
import com.example.workflow.definition.WorkflowDefinitionLoader;
import com.example.workflow.dsl.WorkflowDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowDefinitionCatalogService {
    private final WorkflowEngineProperties properties;
    private final WorkflowDefinitionLoader loader;

    public WorkflowDefinitionCatalogService(WorkflowEngineProperties properties, WorkflowDefinitionLoader loader) {
        this.properties = properties;
        this.loader = loader;
    }

    public List<String> listDefinitionIds() {
        String source = properties.getDefinition().getSource().toLowerCase();
        return switch (source) {
            case "file" -> listFileDefinitionIds(properties.getDefinition().getPath());
            case "classpath" -> listClasspathDefinitionIds(properties.getDefinition().getPath());
            default -> List.of();
        };
    }

    public List<WorkflowDefinition> listDefinitions() {
        Map<String, WorkflowDefinition> unique = new LinkedHashMap<>();
        for (String definitionId : listDefinitionIds()) {
            WorkflowDefinition loaded = loader.load(definitionId);
            unique.putIfAbsent(loaded.id(), loaded);
        }
        return unique.values().stream()
            .sorted(Comparator.comparing(WorkflowDefinition::id))
            .toList();
    }

    public WorkflowDefinition getDefinition(String definitionId) {
        return loader.load(definitionId);
    }

    private List<String> listFileDefinitionIds(String basePath) {
        File dir = new File(basePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return List.of();
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (File file : files) {
            String name = file.getName();
            ids.add(name.substring(0, name.length() - 5));
        }
        ids.sort(String::compareTo);
        return ids;
    }

    private List<String> listClasspathDefinitionIds(String basePath) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:" + basePath + "/*.json");
            List<String> ids = new ArrayList<>();
            for (Resource resource : resources) {
                String name = resource.getFilename();
                if (name != null && name.endsWith(".json")) {
                    ids.add(name.substring(0, name.length() - 5));
                }
            }
            ids.sort(String::compareTo);
            return ids;
        } catch (Exception ex) {
            return List.of();
        }
    }
}
