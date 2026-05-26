package com.k8sgame.service;

import com.k8sgame.model.CommandResponse;
import com.k8sgame.service.CommandService.ParsedCommand;
import com.k8sgame.service.SafetyGuard.ValidationOutcome;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private SafetyGuard safetyGuard;

    @InjectMocks
    private CommandService commandService;

    @Test
    void parse_extractsOperationAndResourceType() {
        ParsedCommand cmd = commandService.parse("kubectl get pods");
        assertThat(cmd.operation()).isEqualTo("get");
        assertThat(cmd.resourceType()).isEqualTo("pods");
        assertThat(cmd.resourceName()).isEmpty();
    }

    @Test
    void parse_extractsResourceName() {
        ParsedCommand cmd = commandService.parse("kubectl delete pod my-pod");
        assertThat(cmd.operation()).isEqualTo("delete");
        assertThat(cmd.resourceType()).isEqualTo("pod");
        assertThat(cmd.resourceName()).isEqualTo("my-pod");
    }

    @Test
    void parse_returnsNullForBareKubectl() {
        assertThat(commandService.parse("kubectl")).isNull();
    }

    @Test
    void execute_rejectsNonKubectlCommand() {
        CommandResponse resp = commandService.execute("game-session-abc", "ls -la");
        assertThat(resp.success()).isFalse();
        assertThat(resp.output()).contains("Only kubectl commands");
    }

    @Test
    void execute_returnsErrorWhenSafetyGuardBlocks() {
        when(safetyGuard.validateOperation(anyString(), anyString(), anyString()))
                .thenReturn(ValidationOutcome.unsafe("blocked"));

        CommandResponse resp = commandService.execute("game-session-abc", "kubectl delete namespace kube-system");
        assertThat(resp.success()).isFalse();
        assertThat(resp.output()).isEqualTo("blocked");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void execute_getPodsReturnsEmptyMessage() {
        when(safetyGuard.validateOperation(anyString(), anyString(), anyString()))
                .thenReturn(ValidationOutcome.safe());

        MixedOperation podOp = mock(MixedOperation.class);
        MixedOperation podNsOp = mock(MixedOperation.class);
        PodList podList = new PodList();
        podList.setItems(List.of());

        doReturn(podOp).when(kubernetesClient).pods();
        doReturn(podNsOp).when(podOp).inNamespace(any());
        doReturn(podList).when(podNsOp).list();

        CommandResponse resp = commandService.execute("game-session-abc", "kubectl get pods");
        assertThat(resp.success()).isTrue();
        assertThat(resp.output()).contains("No resources found");
    }
}
