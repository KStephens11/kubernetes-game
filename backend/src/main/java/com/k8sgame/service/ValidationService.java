package com.k8sgame.service;

import com.k8sgame.model.ValidationResult;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates whether the current cluster state satisfies a challenge's success criteria.
 *
 * <p>Each criterion is a {@code Map<String, String>} loaded from the challenge YAML.
 * Supported criterion types:
 * <ul>
 *   <li>{@code pod_exists} – pod with given name exists in namespace</li>
 *   <li>{@code pod_not_exists} – pod with given name does NOT exist in namespace</li>
 *   <li>{@code pod_running} – pod exists and is in Running phase</li>
 *   <li>{@code pod_status} – pod exists and is in the specified {@code status} phase</li>
 *   <li>{@code service_exists} – service with given name exists</li>
 *   <li>{@code service_selector} – service exists and has a selector matching the given {@code selector} app label</li>
 *   <li>{@code service_has_endpoints} – service has at least one ready endpoint</li>
 *   <li>{@code deployment_ready} – deployment has desired replicas ready</li>
 *   <li>{@code configmap_exists} – configmap with given name exists</li>
 *   <li>{@code secret_exists} – secret with given name exists</li>
 *   <li>{@code command_executed} – always passes (command-tracking is handled client-side)</li>
 *   <li>{@code pod_has_env_from_configmap} – pod exists and has an env var sourced from the given configmap</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    private final KubernetesClient kubernetesClient;

    /**
     * Validates all criteria for a challenge against the current cluster state.
     *
     * @param namespace  the player's namespace
     * @param criteria   list of criterion maps from the challenge YAML
     * @return a {@link ValidationResult} indicating pass/fail with feedback
     */
    public ValidationResult validate(String namespace, List<Map<String, Object>> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return ValidationResult.success("No criteria to validate.");
        }

        List<String> unmet = new ArrayList<>();

        for (Map<String, Object> criterion : criteria) {
            String type = (String) criterion.get("type");
            String name = (String) criterion.get("name");

            try {
                boolean met = switch (type) {
                    case "pod_exists"              -> checkPodExists(namespace, name);
                    case "pod_not_exists"          -> !checkPodExists(namespace, name);
                    case "pod_running"             -> checkPodRunning(namespace, name);
                    case "pod_status"              -> checkPodStatus(namespace, name, criterion);
                    case "service_exists"          -> checkServiceExists(namespace, name);
                    case "service_selector"        -> checkServiceSelector(namespace, name, criterion);
                    case "service_has_endpoints"   -> checkServiceHasEndpoints(namespace, name);
                    case "deployment_ready"        -> checkDeploymentReady(namespace, name, criterion);
                    case "configmap_exists"        -> checkConfigMapExists(namespace, name);
                    case "secret_exists"           -> checkSecretExists(namespace, name);
                    case "pod_has_env_from_configmap" -> checkPodHasEnvFromConfigMap(namespace, name, criterion);
                    case "command_executed"        -> true; // tracked client-side; always passes server validation
                    default -> {
                        logger.warn("Unknown criterion type: {}", type);
                        yield true; // unknown criteria are skipped
                    }
                };

                if (!met) {
                    unmet.add(type + " '" + name + "'");
                }
            } catch (KubernetesClientException e) {
                logger.warn("Error checking criterion {} for {}: {}", type, name, e.getMessage());
                unmet.add(type + " '" + name + "' (cluster error)");
            }
        }

        if (unmet.isEmpty()) {
            return ValidationResult.success("All criteria met! Challenge complete.");
        }
        return ValidationResult.failure(
                "Not yet complete. Remaining: " + String.join(", ", unmet),
                unmet);
    }

    // -----------------------------------------------------------------------
    // Individual criterion checkers
    // -----------------------------------------------------------------------

    /**
     * Checks that a pod with the given name exists in the namespace.
     */
    public boolean checkPodExists(String namespace, String podName) {
        return kubernetesClient.pods().inNamespace(namespace).withName(podName).get() != null;
    }

    /**
     * Checks that a pod exists and is in the Running phase.
     */
    public boolean checkPodRunning(String namespace, String podName) {
        var pod = kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
        if (pod == null || pod.getStatus() == null) return false;
        return "Running".equals(pod.getStatus().getPhase());
    }

    /**
     * Checks that a service with the given name exists in the namespace.
     */
    public boolean checkServiceExists(String namespace, String serviceName) {
        return kubernetesClient.services().inNamespace(namespace).withName(serviceName).get() != null;
    }

    /**
     * Checks that a deployment exists and has the desired number of ready replicas.
     *
     * @param criterion may contain a {@code replicas} key for the expected count
     */
    public boolean checkDeploymentReady(String namespace, String deploymentName, Map<String, Object> criterion) {
        var dep = kubernetesClient.apps().deployments().inNamespace(namespace).withName(deploymentName).get();
        if (dep == null || dep.getStatus() == null) return false;

        int desired = dep.getSpec().getReplicas() != null ? dep.getSpec().getReplicas() : 1;
        int ready = dep.getStatus().getReadyReplicas() != null ? dep.getStatus().getReadyReplicas() : 0;

        // If criterion specifies expected replicas, check that too
        Object expectedReplicas = criterion.get("replicas");
        if (expectedReplicas != null) {
            int expected = Integer.parseInt(expectedReplicas.toString());
            return ready >= expected;
        }

        return ready >= desired;
    }

    /**
     * Checks that a ConfigMap with the given name exists in the namespace.
     */
    public boolean checkConfigMapExists(String namespace, String configMapName) {
        return kubernetesClient.configMaps().inNamespace(namespace).withName(configMapName).get() != null;
    }

    /**
     * Checks that a Secret with the given name exists in the namespace.
     */
    public boolean checkSecretExists(String namespace, String secretName) {
        return kubernetesClient.secrets().inNamespace(namespace).withName(secretName).get() != null;
    }

    /**
     * Checks that a pod exists and is in the phase specified by the {@code status} criterion field.
     */
    public boolean checkPodStatus(String namespace, String podName, Map<String, Object> criterion) {
        var pod = kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
        if (pod == null || pod.getStatus() == null) return false;
        String expectedStatus = (String) criterion.getOrDefault("status", "Running");
        return expectedStatus.equals(pod.getStatus().getPhase());
    }

    /**
     * Checks that a service exists and its {@code app} selector matches the expected value.
     */
    public boolean checkServiceSelector(String namespace, String serviceName, Map<String, Object> criterion) {
        var svc = kubernetesClient.services().inNamespace(namespace).withName(serviceName).get();
        if (svc == null || svc.getSpec() == null) return false;
        String expectedSelector = (String) criterion.get("selector");
        if (expectedSelector == null) return true;
        var selectors = svc.getSpec().getSelector();
        return selectors != null && expectedSelector.equals(selectors.get("app"));
    }

    /**
     * Checks that a service has at least one ready endpoint address.
     */
    public boolean checkServiceHasEndpoints(String namespace, String serviceName) {
        var endpoints = kubernetesClient.endpoints().inNamespace(namespace).withName(serviceName).get();
        if (endpoints == null || endpoints.getSubsets() == null) return false;
        return endpoints.getSubsets().stream()
                .anyMatch(s -> s.getAddresses() != null && !s.getAddresses().isEmpty());
    }

    /**
     * Checks that a pod exists and has at least one env var sourced from the specified ConfigMap.
     * The criterion should include a {@code configmap} field with the ConfigMap name.
     */
    public boolean checkPodHasEnvFromConfigMap(String namespace, String podName, Map<String, Object> criterion) {
        var pod = kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
        if (pod == null || pod.getSpec() == null) return false;
        String expectedConfigMap = (String) criterion.get("configmap");
        return pod.getSpec().getContainers().stream()
                .anyMatch(container -> {
                    // Check envFrom
                    if (container.getEnvFrom() != null) {
                        boolean fromEnvFrom = container.getEnvFrom().stream()
                                .anyMatch(ef -> ef.getConfigMapRef() != null
                                        && (expectedConfigMap == null
                                            || expectedConfigMap.equals(ef.getConfigMapRef().getName())));
                        if (fromEnvFrom) return true;
                    }
                    // Check individual env vars with valueFrom.configMapKeyRef
                    if (container.getEnv() != null) {
                        return container.getEnv().stream()
                                .anyMatch(ev -> ev.getValueFrom() != null
                                        && ev.getValueFrom().getConfigMapKeyRef() != null
                                        && (expectedConfigMap == null
                                            || expectedConfigMap.equals(ev.getValueFrom().getConfigMapKeyRef().getName())));
                    }
                    return false;
                });
    }
}
