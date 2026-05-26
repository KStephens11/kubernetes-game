package com.k8sgame.config;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures and exposes the Fabric8 {@link KubernetesClient} bean.
 *
 * <p>The client is built from the kubeconfig path specified in
 * {@code application.yml} (or the {@code KUBECONFIG} environment variable).
 * On startup the bean verifies connectivity by listing namespaces; a warning
 * is logged if the cluster is unreachable so the application can still start
 * in offline / test mode.
 */
@Configuration
public class KubernetesConfig {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesConfig.class);

    @Value("${kubernetes.config-path:}")
    private String kubeconfigPath;

    /**
     * Creates a {@link KubernetesClient} from the resolved kubeconfig.
     *
     * @return a configured Fabric8 Kubernetes client
     */
    @Bean
    public KubernetesClient kubernetesClient() {
        Config config;
        if (kubeconfigPath != null && !kubeconfigPath.isBlank()) {
            logger.info("Loading kubeconfig from: {}", kubeconfigPath);
            config = new ConfigBuilder()
                    .withFile(new java.io.File(kubeconfigPath))
                    .build();
        } else {
            logger.info("No explicit kubeconfig path — using default (~/.kube/config or in-cluster)");
            config = Config.autoConfigure(null);
        }

        KubernetesClient client = new KubernetesClientBuilder()
                .withConfig(config)
                .build();

        verifyConnectivity(client);
        return client;
    }

    private void verifyConnectivity(KubernetesClient client) {
        try {
            client.namespaces().list();
            logger.info("Kubernetes cluster connectivity verified");
        } catch (KubernetesClientException e) {
            logger.warn("Could not connect to Kubernetes cluster at startup: {}. "
                    + "The application will start but cluster operations will fail.", e.getMessage());
        }
    }
}
