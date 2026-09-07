package com.gulvisha.backend.workflow;

import java.util.UUID;

/**
 * A single executable workflow action.
 *
 * Implementations are Spring beans resolved by the WorkflowEngine via their
 * {@link #type()} key. Adding a new action type = adding a new bean — no engine
 * changes required (open/closed principle).
 *
 * Actions receive the tenant organizationId explicitly; they MUST validate that
 * the target entity belongs to that organization before operating on it.
 */
public interface WorkflowAction {

    /** Unique action key stored in WorkflowStep.actionType, e.g. CREATE_LEAD. */
    String type();

    /**
     * Executes the action against the given entity.
     *
     * @param organizationId tenant owning the workflow and the entity
     * @param entityId       id of the entity the trigger fired for
     * @param config         optional per-step configuration (e.g. note text)
     * @return human-readable log line describing what happened
     */
    String execute(UUID organizationId, UUID entityId, String config);
}
