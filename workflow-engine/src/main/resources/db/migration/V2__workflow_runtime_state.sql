ALTER TABLE workflow_instances ADD COLUMN IF NOT EXISTS definition_id VARCHAR(120) DEFAULT 'unknown';
ALTER TABLE workflow_instances ADD COLUMN IF NOT EXISTS current_step VARCHAR(120);
ALTER TABLE workflow_instances ADD COLUMN IF NOT EXISTS context_json VARCHAR(4000);

ALTER TABLE workflow_steps ADD COLUMN IF NOT EXISTS context_snapshot VARCHAR(4000);

CREATE INDEX IF NOT EXISTS idx_workflow_instances_workflow_id ON workflow_instances(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_steps_instance_id ON workflow_steps(instance_id);
