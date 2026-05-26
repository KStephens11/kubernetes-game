package com.k8sgame.service;

import com.k8sgame.service.SafetyGuard.ValidationOutcome;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyGuardTest {

    private final SafetyGuard guard = new SafetyGuard();

    @Test
    void allowsSafeGetOperation() {
        ValidationOutcome result = guard.validateOperation("get", "game-session-abc", "pods");
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void blocksOperationOnProtectedNamespace() {
        ValidationOutcome result = guard.validateOperation("delete", "kube-system", "pods");
        assertThat(result.allowed()).isFalse();
        assertThat(result.message()).contains("kube-system");
    }

    @Test
    void blocksDeleteOfClusterRole() {
        ValidationOutcome result = guard.validateOperation("delete", "game-session-abc", "clusterrole");
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void blocksDeleteOfNamespace() {
        ValidationOutcome result = guard.validateOperation("delete", "game-session-abc", "namespace");
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void allowsGetOnRestrictedResourceType() {
        ValidationOutcome result = guard.validateOperation("get", "game-session-abc", "nodes");
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void isGameNamespace_trueForGamePrefix() {
        assertThat(guard.isGameNamespace("game-session-xyz")).isTrue();
    }

    @Test
    void isGameNamespace_falseForDefault() {
        assertThat(guard.isGameNamespace("default")).isFalse();
    }
}
