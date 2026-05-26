package com.k8sgame;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test to verify the Spring application context loads successfully.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "kubernetes.config-path=~/.kube/config"
})
class K8sGameApplicationTest {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts without errors
    }
}
