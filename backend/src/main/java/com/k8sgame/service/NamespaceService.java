package com.k8sgame.service;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.ResourceQuotaBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Manages per-session Kubernetes namespaces for player isolation.
 *
 * <p>Each game session gets its own namespace prefixed with
 * {@code game-session-}. Resource quotas are applied to prevent
 * cluster exhaustion.
 */
@Service
@RequiredArgsConstructor
public class NamespaceService {

    private static final Logger logger = LoggerFactory.getLogger(NamespaceService.class);

    private final KubernetesClient kubernetesClient;

    @Value("${kubernetes.namespace-prefix:game-session-}")
    private String namespacePrefix;

    @Value("${kubernetes.resource-quotas.pods:10}")
    private String quotaPods;

    @Value("${kubernetes.resource-quotas.services:5}")
    private String quotaServices;

    @Value("${kubernetes.resource-quotas.cpu:2}")
    private String quotaCpu;

    @Value("${kubernetes.resource-quotas.memory:4Gi}")
    private String quotaMemory;

    /**
     * Returns the namespace name for a given session ID.
     *
     * @param sessionId the player's session ID
     * @return the fully-qualified namespace name
     */
    public String getNamespaceName(String sessionId) {
        return namespacePrefix + sessionId;
    }

    /**
     * Creates the namespace and applies resource quotas for a session.
     *
     * @param sessionId the player's session ID
     * @return {@code true} if setup succeeded, {@code false} otherwise
     */
    public boolean setupNamespace(String sessionId) {
        String namespace = getNamespaceName(sessionId);
        try {
            Namespace ns = new NamespaceBuilder()
                    .withNewMetadata()
                        .withName(namespace)
                        .addToLabels("app", "k8s-game")
                        .addToLabels("session-id", sessionId)
                    .endMetadata()
                    .build();
            kubernetesClient.namespaces().resource(ns).createOrReplace();

            ResourceQuota quota = new ResourceQuotaBuilder()
                    .withNewMetadata()
                        .withName("player-quota")
                        .withNamespace(namespace)
                    .endMetadata()
                    .withNewSpec()
                        .addToHard("pods", new Quantity(quotaPods))
                        .addToHard("services", new Quantity(quotaServices))
                        .addToHard("requests.cpu", new Quantity(quotaCpu))
                        .addToHard("requests.memory", new Quantity(quotaMemory))
                    .endSpec()
                    .build();
            kubernetesClient.resourceQuotas().inNamespace(namespace).resource(quota).createOrReplace();

            logger.info("Namespace {} created with resource quotas", namespace);
            return true;
        } catch (KubernetesClientException e) {
            logger.error("Failed to setup namespace {} for session {}", namespace, sessionId, e);
            return false;
        }
    }

    /**
     * Deletes the namespace and all resources within it.
     *
     * @param sessionId the player's session ID
     */
    public void cleanupNamespace(String sessionId) {
        String namespace = getNamespaceName(sessionId);
        try {
            kubernetesClient.namespaces().withName(namespace).delete();
            logger.info("Namespace {} deleted", namespace);
        } catch (KubernetesClientException e) {
            logger.warn("Failed to delete namespace {}: {}", namespace, e.getMessage());
        }
    }

    /**
     * Deletes all user-created resources in the namespace without deleting the namespace itself.
     *
     * @param sessionId the player's session ID
     */
    public void resetNamespace(String sessionId) {
        String namespace = getNamespaceName(sessionId);
        try {
            kubernetesClient.pods().inNamespace(namespace).delete();
            kubernetesClient.apps().deployments().inNamespace(namespace).delete();
            kubernetesClient.services().inNamespace(namespace).delete();
            kubernetesClient.configMaps().inNamespace(namespace).delete();
            kubernetesClient.secrets().inNamespace(namespace).delete();
            logger.info("Namespace {} reset (resources cleared)", namespace);
        } catch (KubernetesClientException e) {
            logger.warn("Error resetting namespace {}: {}", namespace, e.getMessage());
        }
    }

    /**
     * Resets the namespace and recreates the initial resources defined by the challenge.
     *
     * <p>Each resource map must have a {@code type} field. Supported types:
     * {@code pod}, {@code deployment}, {@code service}, {@code configmap}, {@code secret}.
     *
     * @param sessionId        the player's session ID
     * @param initialResources list of resource descriptor maps from the challenge YAML
     */
    public void resetNamespaceWithResources(String sessionId, List<Map<String, Object>> initialResources) {
        resetNamespace(sessionId);
        if (initialResources == null || initialResources.isEmpty()) return;

        String namespace = getNamespaceName(sessionId);
        for (Map<String, Object> res : initialResources) {
            try {
                createInitialResource(namespace, res);
            } catch (Exception e) {
                logger.warn("Failed to recreate initial resource {} in {}: {}", res.get("name"), namespace, e.getMessage());
            }
        }
        logger.info("Namespace {} initial resources recreated ({} resources)", namespace, initialResources.size());
    }

    @SuppressWarnings("unchecked")
    private void createInitialResource(String namespace, Map<String, Object> res) {
        String type = (String) res.get("type");
        String name = (String) res.get("name");
        String image = (String) res.getOrDefault("image", "nginx");
        int replicas = res.containsKey("replicas") ? Integer.parseInt(res.get("replicas").toString()) : 1;

        switch (type) {
            case "pod" -> kubernetesClient.pods().inNamespace(namespace).resource(
                    new io.fabric8.kubernetes.api.model.PodBuilder()
                        .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                        .withNewSpec()
                            .addNewContainer().withName(name).withImage(image).endContainer()
                        .endSpec()
                        .build()).create();

            case "deployment" -> kubernetesClient.apps().deployments().inNamespace(namespace).resource(
                    new DeploymentBuilder()
                        .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                        .withNewSpec()
                            .withReplicas(replicas)
                            .withNewSelector().addToMatchLabels("app", name).endSelector()
                            .withNewTemplate()
                                .withNewMetadata().addToLabels("app", name).endMetadata()
                                .withNewSpec()
                                    .addNewContainer().withName(name).withImage(image).endContainer()
                                .endSpec()
                            .endTemplate()
                        .endSpec()
                        .build()).create();

            case "service" -> {
                String selector = (String) res.getOrDefault("selector", name);
                int port = res.containsKey("port") ? Integer.parseInt(res.get("port").toString()) : 80;
                kubernetesClient.services().inNamespace(namespace).resource(
                        new io.fabric8.kubernetes.api.model.ServiceBuilder()
                            .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                            .withNewSpec()
                                .addToSelector("app", selector)
                                .addNewPort().withPort(port).withNewTargetPort(port).endPort()
                            .endSpec()
                            .build()).create();
            }

            case "configmap" -> {
                Map<String, String> data = (Map<String, String>) res.getOrDefault("data", Map.of());
                kubernetesClient.configMaps().inNamespace(namespace).resource(
                        new io.fabric8.kubernetes.api.model.ConfigMapBuilder()
                            .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                            .withData(data)
                            .build()).create();
            }

            case "secret" -> {
                Map<String, String> data = (Map<String, String>) res.getOrDefault("data", Map.of());
                kubernetesClient.secrets().inNamespace(namespace).resource(
                        new io.fabric8.kubernetes.api.model.SecretBuilder()
                            .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                            .withStringData(data)
                            .build()).create();
            }

            default -> logger.warn("Unknown initial resource type '{}' for resource '{}'", type, name);
        }
    }
}