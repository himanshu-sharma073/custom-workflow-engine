# Configuration Reference

## Root switch
- `workflow.engine.enabled` (default `true`)

## API
- `workflow.engine.api.enabled` (default `true`)
- `workflow.engine.api.base-path` (default `/workflows`)

## UI metadata (separate UI app)
- `workflow.engine.ui.enabled` (default `true`)
- `workflow.engine.ui.path` (default `/workflow-ui`)

## Definition loading
- `workflow.engine.definition.source` (`file` | `classpath` | `db`)
- `workflow.engine.definition.path` (directory/base path)
- `workflow.engine.definition.cache-enabled` (`true`/`false`)

## Execution
- `workflow.engine.execution.max-retries` (default `3`)
- `workflow.engine.execution.retry-backoff-ms` (default `2000`)

## Persistence
- `workflow.engine.persistence.type` (default `jpa`)

## Security metadata
- `workflow.engine.security.enabled` (default `false`)

## Host sample defaults
See: `host-app-example/src/main/resources/application.properties`
