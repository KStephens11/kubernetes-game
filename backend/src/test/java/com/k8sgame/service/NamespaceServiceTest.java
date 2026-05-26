package com.k8sgame.service;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NamespaceServiceTest {

    @Mock
    private KubernetesClient kubernetesClient;

    @InjectMocks
    private NamespaceService namespaceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(namespaceService, "namespacePrefix", "game-session-");
        ReflectionTestUtils.setField(namespaceService, "quotaPods", "10");
        ReflectionTestUtils.setField(namespaceService, "quotaServices", "5");
        ReflectionTestUtils.setField(namespaceService, "quotaCpu", "2");
        ReflectionTestUtils.setField(namespaceService, "quotaMemory", "4Gi");
    }

    @Test
    void getNamespaceName_prependsPrefix() {
        assertThat(namespaceService.getNamespaceName("abc123"))
                .isEqualTo("game-session-abc123");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void setupNamespace_createsNamespaceAndQuota() {
        NonNamespaceOperation nsOp = mock(NonNamespaceOperation.class);
        Resource nsResource = mock(Resource.class);
        doReturn(nsOp).when(kubernetesClient).namespaces();
        doReturn(nsResource).when(nsOp).resource(any(Namespace.class));
        doReturn(new Namespace()).when(nsResource).createOrReplace();

        MixedOperation rqOp = mock(MixedOperation.class);
        MixedOperation rqNsOp = mock(MixedOperation.class);
        Resource rqResource = mock(Resource.class);
        doReturn(rqOp).when(kubernetesClient).resourceQuotas();
        doReturn(rqNsOp).when(rqOp).inNamespace(any());
        doReturn(rqResource).when(rqNsOp).resource(any(ResourceQuota.class));
        doReturn(new ResourceQuota()).when(rqResource).createOrReplace();

        boolean result = namespaceService.setupNamespace("abc123");

        assertThat(result).isTrue();
        verify(nsResource).createOrReplace();
        verify(rqResource).createOrReplace();
    }
}
