package com.k8sgame.service;

import com.k8sgame.model.ValidationResult;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.AppsAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private KubernetesClient kubernetesClient;

    @InjectMocks
    private ValidationService validationService;

    private static final String NS = "game-session-test";

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubPod(String name, Pod pod) {
        MixedOperation podOp = mock(MixedOperation.class);
        MixedOperation podNsOp = mock(MixedOperation.class);
        PodResource podResource = mock(PodResource.class);
        doReturn(podOp).when(kubernetesClient).pods();
        doReturn(podNsOp).when(podOp).inNamespace(NS);
        doReturn(podResource).when(podNsOp).withName(name);
        doReturn(pod).when(podResource).get();
    }

    @Test
    void checkPodExists_returnsTrueWhenPodFound() {
        stubPod("web-server", new Pod());
        assertThat(validationService.checkPodExists(NS, "web-server")).isTrue();
    }

    @Test
    void checkPodExists_returnsFalseWhenPodMissing() {
        stubPod("web-server", null);
        assertThat(validationService.checkPodExists(NS, "web-server")).isFalse();
    }

    @Test
    void checkPodRunning_returnsTrueWhenRunning() {
        Pod pod = new Pod();
        PodStatus status = new PodStatus();
        status.setPhase("Running");
        pod.setStatus(status);
        stubPod("web-server", pod);
        assertThat(validationService.checkPodRunning(NS, "web-server")).isTrue();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void checkDeploymentReady_returnsTrueWhenReplicasReady() {
        Deployment dep = new Deployment();
        DeploymentSpec spec = new DeploymentSpec();
        spec.setReplicas(3);
        dep.setSpec(spec);
        DeploymentStatus status = new DeploymentStatus();
        status.setReadyReplicas(3);
        dep.setStatus(status);

        AppsAPIGroupDSL appsOp = mock(AppsAPIGroupDSL.class);
        MixedOperation depOp = mock(MixedOperation.class);
        MixedOperation depNsOp = mock(MixedOperation.class);
        io.fabric8.kubernetes.client.dsl.RollableScalableResource depResource =
                mock(io.fabric8.kubernetes.client.dsl.RollableScalableResource.class);
        doReturn(appsOp).when(kubernetesClient).apps();
        doReturn(depOp).when(appsOp).deployments();
        doReturn(depNsOp).when(depOp).inNamespace(NS);
        doReturn(depResource).when(depNsOp).withName("my-app");
        doReturn(dep).when(depResource).get();

        assertThat(validationService.checkDeploymentReady(NS, "my-app", Map.of())).isTrue();
    }

    @Test
    void validate_returnsSuccessWhenAllCriteriaMet() {
        Pod pod = new Pod();
        PodStatus status = new PodStatus();
        status.setPhase("Running");
        pod.setStatus(status);
        stubPod("web-server", pod);

        List<Map<String, Object>> criteria = List.of(
                Map.of("type", "pod_exists", "name", "web-server"),
                Map.of("type", "pod_running", "name", "web-server")
        );

        ValidationResult result = validationService.validate(NS, criteria);
        assertThat(result.success()).isTrue();
    }

    @Test
    void validate_returnsFailureWhenCriterionUnmet() {
        stubPod("web-server", null);

        List<Map<String, Object>> criteria = List.of(
                Map.of("type", "pod_exists", "name", "web-server")
        );

        ValidationResult result = validationService.validate(NS, criteria);
        assertThat(result.success()).isFalse();
        assertThat(result.unmetCriteria()).contains("pod_exists 'web-server'");
    }
}
