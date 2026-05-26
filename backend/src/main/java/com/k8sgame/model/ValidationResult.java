package com.k8sgame.model;

import java.util.List;

/**
 * Result of validating a challenge's success criteria against the cluster state.
 *
 * @param success  whether all criteria are met
 * @param feedback human-readable feedback for the player
 * @param unmetCriteria list of criteria descriptions that are not yet satisfied
 */
public record ValidationResult(boolean success, String feedback, List<String> unmetCriteria) {

    public static ValidationResult success(String feedback) {
        return new ValidationResult(true, feedback, List.of());
    }

    public static ValidationResult failure(String feedback, List<String> unmet) {
        return new ValidationResult(false, feedback, unmet);
    }
}
