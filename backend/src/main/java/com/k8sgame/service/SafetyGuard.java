package com.k8sgame.service;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Guards against dangerous or out-of-scope Kubernetes operations.
 *
 * <p>All command executions must pass through {@link #validateOperation} before
 * being forwarded to the Kubernetes API.
 */
@Component
public class SafetyGuard {

    private static final Set<String> PROTECTED_NAMESPACES = Set.of(
            "kube-system", "kube-public", "default", "kube-node-lease"
    );

    private static final Set<String> RESTRICTED_RESOURCE_TYPES = Set.of(
            "namespace", "namespaces",
            "persistentvolume", "persistentvolumes",
            "clusterrole", "clusterroles",
            "clusterrolebinding", "clusterrolebindings",
            "node", "nodes"
    );

    private static final Set<String> DESTRUCTIVE_OPERATIONS = Set.of("delete", "drain", "cordon");

    /**
     * Validates whether an operation is safe to execute.
     *
     * @param operation    the kubectl verb (get, create, delete, …)
     * @param namespace    the target namespace
     * @param resourceType the Kubernetes resource type
     * @return a {@link ValidationOutcome} indicating safety status and reason
     */
    public ValidationOutcome validateOperation(String operation, String namespace, String resourceType) {
        if (namespace != null && PROTECTED_NAMESPACES.contains(namespace.toLowerCase())) {
            return ValidationOutcome.unsafe(
                    "Cannot operate on protected namespace '" + namespace + "'. "
                    + "Your game namespace starts with 'game-session-'.");
        }

        String lowerResource = resourceType == null ? "" : resourceType.toLowerCase();
        if (DESTRUCTIVE_OPERATIONS.contains(operation) && RESTRICTED_RESOURCE_TYPES.contains(lowerResource)) {
            return ValidationOutcome.unsafe(
                    "Deletion of '" + resourceType + "' is not allowed in this game environment.");
        }

        if (RESTRICTED_RESOURCE_TYPES.contains(lowerResource) && !"get".equals(operation) && !"describe".equals(operation)) {
            return ValidationOutcome.unsafe(
                    "Modifying '" + resourceType + "' is not allowed in this game environment.");
        }

        return ValidationOutcome.safe();
    }

    /**
     * Checks whether a namespace is a player-owned game namespace.
     *
     * @param namespace the namespace to check
     * @return {@code true} if the namespace belongs to a game session
     */
    public boolean isGameNamespace(String namespace) {
        return namespace != null && namespace.startsWith("game-session-");
    }

    // -----------------------------------------------------------------------
    // Inner result type
    // -----------------------------------------------------------------------

    /**
     * Immutable result of a safety validation check.
     */
    public record ValidationOutcome(boolean allowed, String message) {

        public static ValidationOutcome safe() {
            return new ValidationOutcome(true, null);
        }

        public static ValidationOutcome unsafe(String reason) {
            return new ValidationOutcome(false, reason);
        }
    }
}
