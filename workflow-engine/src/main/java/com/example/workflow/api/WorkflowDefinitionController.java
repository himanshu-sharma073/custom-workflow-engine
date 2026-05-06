package com.example.workflow.api;

import com.example.workflow.definition.catalog.WorkflowDefinitionCatalogService;
import com.example.workflow.dsl.WorkflowDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ConditionalOnProperty(prefix = "workflow.engine.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("${workflow.engine.api.base-path:/workflows}/definitions")
@Tag(name = "Workflow definitions", description = "Loaded workflow DSL definitions")
public class WorkflowDefinitionController {
    private final WorkflowDefinitionCatalogService catalogService;

    public WorkflowDefinitionController(WorkflowDefinitionCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "List definitions")
    @ApiResponse(responseCode = "200", description = "All known definitions")
    @GetMapping
    public List<WorkflowDefinition> list() {
        return catalogService.listDefinitions();
    }

    @Operation(summary = "Get definition by id")
    @ApiResponse(responseCode = "200", description = "Definition document")
    @GetMapping("/{id}")
    public WorkflowDefinition get(
        @Parameter(description = "Logical definition id (e.g. filename key)", required = true, example = "ratings-review-workflow")
        @PathVariable("id") String id) {
        return catalogService.getDefinition(id);
    }
}
