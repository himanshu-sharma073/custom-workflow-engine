package com.example.workflow.api;

import com.example.workflow.definition.catalog.WorkflowDefinitionCatalogService;
import com.example.workflow.dsl.WorkflowDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ConditionalOnProperty(prefix = "workflow.engine.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${workflow.engine.api.base-path:/workflows}/definitions")
public class WorkflowDefinitionController {
    private final WorkflowDefinitionCatalogService catalogService;

    public WorkflowDefinitionController(WorkflowDefinitionCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<WorkflowDefinition> list() {
        return catalogService.listDefinitions();
    }

    @GetMapping("/{id}")
    public WorkflowDefinition get(@PathVariable("id") String id) {
        return catalogService.getDefinition(id);
    }
}
