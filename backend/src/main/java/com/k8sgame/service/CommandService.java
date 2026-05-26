package com.k8sgame.service;

import com.k8sgame.model.CommandResponse;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses kubectl command strings and executes them against the Kubernetes cluster
 * via the Fabric8 client, scoped to the player's namespace.
 *
 * <p>Supported operations: get, describe, create, run, delete, apply, expose, scale, logs.
 */
@Service
@RequiredArgsConstructor
public class CommandService {

    private static final Logger logger = LoggerFactory.getLogger(CommandService.class);

    private final KubernetesClient kubernetesClient;
    private final SafetyGuard safetyGuard;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Executes a raw kubectl command string in the given namespace.
     *
     * @param namespace the player's namespace
     * @param rawCommand the full command string (e.g. "kubectl get pods")
     * @return a {@link CommandResponse} with output or error text
     */
    public CommandResponse execute(String namespace, String rawCommand) {
        String trimmed = rawCommand.trim();

        if (!trimmed.startsWith("kubectl ") && !trimmed.equals("kubectl")) {
            return CommandResponse.error("Only kubectl commands are supported here. "
                    + "Use 'game hint', 'game status', etc. for game commands.");
        }

        ParsedCommand cmd = parse(trimmed);
        if (cmd == null) {
            return CommandResponse.error("Could not parse command: " + trimmed);
        }

        SafetyGuard.ValidationOutcome safety = safetyGuard.validateOperation(
                cmd.operation(), namespace, cmd.resourceType());
        if (!safety.allowed()) {
            return CommandResponse.error(safety.message());
        }

        try {
            String output = dispatch(namespace, cmd, trimmed);
            return CommandResponse.success(output);
        } catch (KubernetesClientException e) {
            logger.warn("Kubernetes error executing '{}': {}", trimmed, e.getMessage());
            return CommandResponse.error(formatKubeError(e));
        } catch (Exception e) {
            logger.error("Unexpected error executing '{}'", trimmed, e);
            return CommandResponse.error("Unexpected error: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    /**
     * Parses a kubectl command string into its components.
     *
     * @param command the full kubectl command
     * @return a {@link ParsedCommand} or {@code null} if unparseable
     */
    public ParsedCommand parse(String command) {
        String[] tokens = command.trim().split("\\s+");
        if (tokens.length < 2) return null;

        // tokens[0] == "kubectl"
        String operation = tokens[1];
        String resourceType = tokens.length > 2 ? tokens[2] : "";
        String resourceName = tokens.length > 3 ? tokens[3].replaceAll("^\"|\"$|^'|'$", "") : "";

        // Strip leading dashes from flags that might appear as resourceType
        if (resourceType.startsWith("-")) {
            resourceType = "";
            resourceName = "";
        }

        return new ParsedCommand(operation, resourceType, resourceName, tokens);
    }

    // -----------------------------------------------------------------------
    // Dispatch
    // -----------------------------------------------------------------------

    private String dispatch(String namespace, ParsedCommand cmd, String rawCommand) {
        return switch (cmd.operation()) {
            case "get"      -> handleGet(namespace, cmd);
            case "describe" -> handleDescribe(namespace, cmd);
            case "run"      -> handleRun(namespace, cmd);
            case "create"   -> handleCreate(namespace, cmd, rawCommand);
            case "delete"   -> handleDelete(namespace, cmd);
            case "apply"    -> handleApply(namespace, rawCommand);
            case "expose"   -> handleExpose(namespace, cmd);
            case "scale"    -> handleScale(namespace, cmd);
            case "logs"     -> handleLogs(namespace, cmd);
            case "set"      -> handleSet(namespace, cmd);
            default         -> "kubectl: unknown command '" + cmd.operation() + "'";
        };
    }

    // -----------------------------------------------------------------------
    // Operation handlers
    // -----------------------------------------------------------------------

    private String handleGet(String namespace, ParsedCommand cmd) {
        return switch (cmd.resourceType().toLowerCase()) {
            case "pods", "pod", "po" -> {
                var pods = kubernetesClient.pods().inNamespace(namespace).list().getItems();
                if (pods.isEmpty()) yield "No resources found in " + namespace + " namespace.";
                yield formatTable("NAME", "STATUS", "RESTARTS", "AGE",
                        pods.stream().map(p -> new String[]{
                                p.getMetadata().getName(),
                                p.getStatus() != null && p.getStatus().getPhase() != null
                                        ? p.getStatus().getPhase() : "Unknown",
                                "0",
                                "0s"
                        }).collect(Collectors.toList()));
            }
            case "deployments", "deployment", "deploy" -> {
                var deps = kubernetesClient.apps().deployments().inNamespace(namespace).list().getItems();
                if (deps.isEmpty()) yield "No resources found in " + namespace + " namespace.";
                yield formatTable("NAME", "READY", "UP-TO-DATE", "AVAILABLE",
                        deps.stream().map(d -> {
                            var status = d.getStatus();
                            int ready = status != null && status.getReadyReplicas() != null ? status.getReadyReplicas() : 0;
                            int desired = d.getSpec().getReplicas() != null ? d.getSpec().getReplicas() : 1;
                            return new String[]{
                                    d.getMetadata().getName(),
                                    ready + "/" + desired,
                                    String.valueOf(status != null && status.getUpdatedReplicas() != null ? status.getUpdatedReplicas() : 0),
                                    String.valueOf(ready)
                            };
                        }).collect(Collectors.toList()));
            }
            case "services", "service", "svc" -> {
                var svcs = kubernetesClient.services().inNamespace(namespace).list().getItems();
                if (svcs.isEmpty()) yield "No resources found in " + namespace + " namespace.";
                yield formatTable("NAME", "TYPE", "CLUSTER-IP", "PORT(S)",
                        svcs.stream().map(s -> new String[]{
                                s.getMetadata().getName(),
                                s.getSpec().getType() != null ? s.getSpec().getType() : "ClusterIP",
                                s.getSpec().getClusterIP() != null ? s.getSpec().getClusterIP() : "<none>",
                                s.getSpec().getPorts() != null
                                        ? s.getSpec().getPorts().stream()
                                            .map(p -> p.getPort() + "/" + p.getProtocol())
                                            .collect(Collectors.joining(","))
                                        : "<none>"
                        }).collect(Collectors.toList()));
            }
            case "configmaps", "configmap", "cm" -> {
                var cms = kubernetesClient.configMaps().inNamespace(namespace).list().getItems();
                if (cms.isEmpty()) yield "No resources found in " + namespace + " namespace.";
                yield formatTable("NAME", "DATA", "AGE",
                        cms.stream().map(c -> new String[]{
                                c.getMetadata().getName(),
                                c.getData() != null ? String.valueOf(c.getData().size()) : "0",
                                "0s"
                        }).collect(Collectors.toList()));
            }
            case "secrets", "secret" -> {
                var secrets = kubernetesClient.secrets().inNamespace(namespace).list().getItems();
                if (secrets.isEmpty()) yield "No resources found in " + namespace + " namespace.";
                yield formatTable("NAME", "TYPE", "DATA", "AGE",
                        secrets.stream().map(s -> new String[]{
                                s.getMetadata().getName(),
                                s.getType() != null ? s.getType() : "Opaque",
                                s.getData() != null ? String.valueOf(s.getData().size()) : "0",
                                "0s"
                        }).collect(Collectors.toList()));
            }
            case "namespaces", "namespace", "ns" -> {
                var nsList = kubernetesClient.namespaces().list().getItems();
                yield formatTable("NAME", "STATUS", "AGE",
                        nsList.stream().map(n -> new String[]{
                                n.getMetadata().getName(),
                                n.getStatus() != null && n.getStatus().getPhase() != null
                                        ? n.getStatus().getPhase() : "Active",
                                "0s"
                        }).collect(Collectors.toList()));
            }
            default -> "error: the server doesn't have a resource type '" + cmd.resourceType() + "'";
        };
    }

    private String handleDescribe(String namespace, ParsedCommand cmd) {
        String type = cmd.resourceType().toLowerCase();
        String name = cmd.resourceName();
        if (name.isEmpty()) return "error: must specify resource name";

        return switch (type) {
            case "pod", "pods", "po" -> {
                var pod = kubernetesClient.pods().inNamespace(namespace).withName(name).get();
                if (pod == null) yield "Error from server (NotFound): pods \"" + name + "\" not found";
                yield "Name: " + pod.getMetadata().getName() + "\n"
                        + "Namespace: " + pod.getMetadata().getNamespace() + "\n"
                        + "Status: " + (pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown") + "\n"
                        + "Labels: " + pod.getMetadata().getLabels() + "\n"
                        + "Containers: " + (pod.getSpec() != null
                            ? pod.getSpec().getContainers().stream()
                                .map(c -> c.getName() + " (" + c.getImage() + ")")
                                .collect(Collectors.joining(", "))
                            : "none");
            }
            case "deployment", "deployments", "deploy" -> {
                var dep = kubernetesClient.apps().deployments().inNamespace(namespace).withName(name).get();
                if (dep == null) yield "Error from server (NotFound): deployments \"" + name + "\" not found";
                yield "Name: " + dep.getMetadata().getName() + "\n"
                        + "Namespace: " + dep.getMetadata().getNamespace() + "\n"
                        + "Replicas: " + dep.getSpec().getReplicas() + "\n"
                        + "Labels: " + dep.getMetadata().getLabels();
            }
            case "service", "services", "svc" -> {
                var svc = kubernetesClient.services().inNamespace(namespace).withName(name).get();
                if (svc == null) yield "Error from server (NotFound): services \"" + name + "\" not found";
                yield "Name: " + svc.getMetadata().getName() + "\n"
                        + "Namespace: " + svc.getMetadata().getNamespace() + "\n"
                        + "Type: " + svc.getSpec().getType() + "\n"
                        + "Selector: " + svc.getSpec().getSelector();
            }
            default -> "error: the server doesn't have a resource type '" + type + "'";
        };
    }

    private String handleRun(String namespace, ParsedCommand cmd) {
        // kubectl run <name> --image=<image>
        String podName = cmd.resourceType(); // tokens[2] is the pod name for 'run'
        if (podName.isEmpty() || podName.startsWith("-")) return "error: must specify pod name";

        String image = extractFlag(cmd.tokens(), "--image");
        if (image == null) return "error: --image flag is required";

        var pod = new io.fabric8.kubernetes.api.model.PodBuilder()
                .withNewMetadata()
                    .withName(podName)
                    .withNamespace(namespace)
                    .addToLabels("run", podName)
                .endMetadata()
                .withNewSpec()
                    .addNewContainer()
                        .withName(podName)
                        .withImage(image)
                        .withNewResources()
                            .addToRequests("cpu", new io.fabric8.kubernetes.api.model.Quantity("100m"))
                            .addToRequests("memory", new io.fabric8.kubernetes.api.model.Quantity("128Mi"))
                        .endResources()
                    .endContainer()
                .endSpec()
                .build();
        kubernetesClient.pods().inNamespace(namespace).resource(pod).createOrReplace();
        return "pod/" + podName + " created";
    }

    private String handleCreate(String namespace, ParsedCommand cmd, String rawCommand) {
        String subType = cmd.resourceType().toLowerCase();
        return switch (subType) {
            case "deployment" -> {
                String name = cmd.resourceName();
                String image = extractFlag(cmd.tokens(), "--image");
                String replicasStr = extractFlag(cmd.tokens(), "--replicas");
                int replicas = replicasStr != null ? Integer.parseInt(replicasStr) : 1;
                if (name.isEmpty() || image == null) yield "error: must specify name and --image";

                var dep = new io.fabric8.kubernetes.api.model.apps.DeploymentBuilder()
                        .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                        .withNewSpec()
                            .withReplicas(replicas)
                            .withNewSelector().addToMatchLabels("app", name).endSelector()
                            .withNewTemplate()
                                .withNewMetadata().addToLabels("app", name).endMetadata()
                                .withNewSpec()
                                    .addNewContainer()
                                        .withName(name)
                                        .withImage(image)
                                        .withNewResources()
                                            .addToRequests("cpu", new io.fabric8.kubernetes.api.model.Quantity("100m"))
                                            .addToRequests("memory", new io.fabric8.kubernetes.api.model.Quantity("128Mi"))
                                        .endResources()
                                    .endContainer()
                                .endSpec()
                            .endTemplate()
                        .endSpec()
                        .build();
                kubernetesClient.apps().deployments().inNamespace(namespace).resource(dep).createOrReplace();
                yield "deployment.apps/" + name + " created";
            }
            case "configmap", "cm" -> {
                String name = cmd.resourceName();
                String fromLiteral = extractFlag(cmd.tokens(), "--from-literal");
                if (name.isEmpty()) yield "error: must specify configmap name";

                var cmBuilder = new io.fabric8.kubernetes.api.model.ConfigMapBuilder()
                        .withNewMetadata().withName(name).withNamespace(namespace).endMetadata();
                if (fromLiteral != null) {
                    String[] kv = fromLiteral.split("=", 2);
                    if (kv.length == 2) cmBuilder.addToData(kv[0], kv[1]);
                }
                kubernetesClient.configMaps().inNamespace(namespace).resource(cmBuilder.build()).createOrReplace();
                yield "configmap/" + name + " created";
            }
            case "secret" -> {
                // kubectl create secret generic <name> --from-literal=key=value
                String secretType = cmd.tokens().length > 3 ? cmd.tokens()[3] : "generic";
                String name = cmd.tokens().length > 4 ? cmd.tokens()[4] : "";
                if (name.isEmpty()) yield "error: must specify secret name";

                String fromLiteral = extractFlag(cmd.tokens(), "--from-literal");
                var secretBuilder = new io.fabric8.kubernetes.api.model.SecretBuilder()
                        .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                        .withType("Opaque");
                if (fromLiteral != null) {
                    String[] kv = fromLiteral.split("=", 2);
                    if (kv.length == 2) {
                        secretBuilder.addToStringData(kv[0], kv[1]);
                    }
                }
                kubernetesClient.secrets().inNamespace(namespace).resource(secretBuilder.build()).createOrReplace();
                yield "secret/" + name + " created";
            }
            default -> "error: unknown resource type '" + subType + "' for create";
        };
    }

    private String handleDelete(String namespace, ParsedCommand cmd) {
        String type = cmd.resourceType().toLowerCase();
        String name = cmd.resourceName();
        if (name.isEmpty()) return "error: must specify resource name";

        return switch (type) {
            case "pod", "pods", "po" -> {
                kubernetesClient.pods().inNamespace(namespace).withName(name).delete();
                yield "pod \"" + name + "\" deleted";
            }
            case "deployment", "deployments", "deploy" -> {
                kubernetesClient.apps().deployments().inNamespace(namespace).withName(name).delete();
                yield "deployment.apps \"" + name + "\" deleted";
            }
            case "service", "services", "svc" -> {
                kubernetesClient.services().inNamespace(namespace).withName(name).delete();
                yield "service \"" + name + "\" deleted";
            }
            case "configmap", "configmaps", "cm" -> {
                kubernetesClient.configMaps().inNamespace(namespace).withName(name).delete();
                yield "configmap \"" + name + "\" deleted";
            }
            case "secret", "secrets" -> {
                kubernetesClient.secrets().inNamespace(namespace).withName(name).delete();
                yield "secret \"" + name + "\" deleted";
            }
            default -> "error: the server doesn't have a resource type '" + type + "'";
        };
    }

    private String handleApply(String namespace, String rawCommand) {
        // kubectl apply -f - (inline YAML not supported in game; guide user)
        return "error: 'kubectl apply -f' with inline YAML is not supported in the game terminal. "
                + "Use 'kubectl create' or 'kubectl run' commands instead.";
    }

    private String handleExpose(String namespace, ParsedCommand cmd) {
        // kubectl expose deployment <name> --port=<port> [--target-port=<port>] [--name=<svcname>]
        String type = cmd.resourceType().toLowerCase();
        String name = cmd.resourceName();
        String portStr = extractFlag(cmd.tokens(), "--port");
        String svcName = extractFlag(cmd.tokens(), "--name");
        if (svcName == null) svcName = name;
        if (portStr == null) return "error: --port flag is required";

        int port = Integer.parseInt(portStr);
        String targetPortStr = extractFlag(cmd.tokens(), "--target-port");
        int targetPort = targetPortStr != null ? Integer.parseInt(targetPortStr) : port;

        var svc = new io.fabric8.kubernetes.api.model.ServiceBuilder()
                .withNewMetadata().withName(svcName).withNamespace(namespace).endMetadata()
                .withNewSpec()
                    .withType("ClusterIP")
                    .addToSelector("app", name)
                    .addNewPort()
                        .withPort(port)
                        .withNewTargetPort(targetPort)
                    .endPort()
                .endSpec()
                .build();
        kubernetesClient.services().inNamespace(namespace).resource(svc).createOrReplace();
        return "service/" + svcName + " exposed";
    }

    private String handleScale(String namespace, ParsedCommand cmd) {
        // kubectl scale deployment <name> --replicas=<n>
        String type = cmd.resourceType().toLowerCase();
        String name = cmd.resourceName();
        String replicasStr = extractFlag(cmd.tokens(), "--replicas");
        if (replicasStr == null) return "error: --replicas flag is required";
        int replicas = Integer.parseInt(replicasStr);

        if (type.startsWith("deploy")) {
            kubernetesClient.apps().deployments().inNamespace(namespace).withName(name)
                    .scale(replicas);
            return "deployment.apps/" + name + " scaled";
        }
        return "error: scale not supported for resource type '" + type + "'";
    }

    private String handleLogs(String namespace, ParsedCommand cmd) {
        String podName = cmd.resourceType(); // kubectl logs <podname>
        if (podName.isEmpty()) return "error: must specify pod name";

        var pod = kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
        if (pod == null) return "Error from server (NotFound): pods \"" + podName + "\" not found";

        String logs = kubernetesClient.pods().inNamespace(namespace).withName(podName).getLog();
        return logs != null && !logs.isBlank() ? logs : "(no logs available)";
    }

    private String handleSet(String namespace, ParsedCommand cmd) {
        // kubectl set image deployment/<name> <container>=<image>
        if (!"image".equals(cmd.resourceType())) {
            return "error: 'kubectl set " + cmd.resourceType() + "' is not supported";
        }
        // tokens: kubectl set image deployment/<name> <container>=<image>
        String[] tokens = cmd.tokens();
        if (tokens.length < 5) return "error: usage: kubectl set image deployment/<name> <container>=<image>";

        String depRef = tokens[3]; // e.g. deployment/my-app
        String containerImage = tokens[4]; // e.g. my-app=nginx:1.21

        String depName = depRef.contains("/") ? depRef.split("/")[1] : depRef;
        String[] ci = containerImage.split("=", 2);
        if (ci.length < 2) return "error: invalid format, expected <container>=<image>";

        String containerName = ci[0];
        String newImage = ci[1];

        var dep = kubernetesClient.apps().deployments().inNamespace(namespace).withName(depName).get();
        if (dep == null) return "Error from server (NotFound): deployments \"" + depName + "\" not found";

        dep.getSpec().getTemplate().getSpec().getContainers().stream()
                .filter(c -> c.getName().equals(containerName))
                .findFirst()
                .ifPresent(c -> c.setImage(newImage));

        kubernetesClient.apps().deployments().inNamespace(namespace).resource(dep).replace();
        return "deployment.apps/" + depName + " image updated";
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String extractFlag(String[] tokens, String flag) {
        for (String token : tokens) {
            if (token.startsWith(flag + "=")) {
                return token.substring(flag.length() + 1);
            }
        }
        return null;
    }

    private String formatTable(String col1, String col2, String col3, List<String[]> rows) {
        return formatTable(col1, col2, col3, null, rows);
    }

    private String formatTable(String col1, String col2, String col3, String col4, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        if (col4 != null) {
            sb.append(String.format("%-40s %-15s %-15s %-15s%n", col1, col2, col3, col4));
        } else {
            sb.append(String.format("%-40s %-15s %-15s%n", col1, col2, col3));
        }
        for (String[] row : rows) {
            if (col4 != null && row.length >= 4) {
                sb.append(String.format("%-40s %-15s %-15s %-15s%n", row[0], row[1], row[2], row[3]));
            } else {
                sb.append(String.format("%-40s %-15s %-15s%n", row[0], row[1], row[2]));
            }
        }
        return sb.toString().stripTrailing();
    }

    private String formatKubeError(KubernetesClientException e) {
        if (e.getCode() == 404) return "Error from server (NotFound): " + e.getMessage();
        if (e.getCode() == 403) return "Error from server (Forbidden): " + e.getMessage();
        if (e.getCode() == 409) return "Error from server (AlreadyExists): " + e.getMessage();
        return "Error from server: " + e.getMessage();
    }

    // -----------------------------------------------------------------------
    // ParsedCommand record
    // -----------------------------------------------------------------------

    /**
     * Parsed representation of a kubectl command.
     *
     * @param operation    the kubectl verb (get, create, delete, …)
     * @param resourceType the resource type (pods, deployment, …)
     * @param resourceName the resource name (may be empty)
     * @param tokens       all whitespace-split tokens of the original command
     */
    public record ParsedCommand(String operation, String resourceType, String resourceName, String[] tokens) {}
}
