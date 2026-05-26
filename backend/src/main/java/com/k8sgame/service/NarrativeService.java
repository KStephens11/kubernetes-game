package com.k8sgame.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Delivers narrative story content for level transitions.
 */
@Service
public class NarrativeService {

    private static final String BORDER = "═".repeat(56);

    private static final Map<Integer, String> LEVEL_INTROS = Map.of(
        1, "Welcome aboard the Kubernetes Station. Systems are failing\n" +
           "and you're the new cluster operator. Your first task:\n" +
           "get critical pods running.",
        2, "The station is stable, but traffic is increasing. You need\n" +
           "to deploy scalable workloads before systems overload.",
        3, "Pods are running, but nothing can reach them. Configure\n" +
           "Services to restore network connectivity.",
        4, "The cluster is connected. Now manage configuration and\n" +
           "secrets to keep sensitive data out of your pod specs.",
        5, "Something is broken. Use your debugging skills to diagnose\n" +
           "and fix failing workloads before the station goes dark."
    );

    private static final Map<Integer, String> LEVEL_COMPLETIONS = Map.of(
        1, "Pod basics mastered. The station's core systems are online.",
        2, "Deployments under control. Scaling is no longer a problem.",
        3, "Networking restored. Services are routing traffic correctly.",
        4, "Configuration secured. Secrets are safe from prying eyes.",
        5, "All systems nominal. You've completed the Kubernetes Station!"
    );

    /**
     * Returns a formatted level introduction message.
     */
    public String getLevelIntro(int level) {
        String text = LEVEL_INTROS.getOrDefault(level, "A new challenge awaits.");
        return formatBox("LEVEL " + level, text);
    }

    /**
     * Returns a formatted level completion message.
     */
    public String getLevelComplete(int level) {
        String text = LEVEL_COMPLETIONS.getOrDefault(level, "Level complete!");
        return formatBox("LEVEL " + level + " COMPLETE", text);
    }

    private String formatBox(String header, String body) {
        return "\n\u001b[1;35m╔" + BORDER + "╗\n" +
               "║  " + padRight(header, 54) + "║\n" +
               "╠" + BORDER + "╣\n" +
               formatBodyLines(body) +
               "╚" + BORDER + "╝\u001b[0m\n";
    }

    private String formatBodyLines(String body) {
        StringBuilder sb = new StringBuilder();
        for (String line : body.split("\n")) {
            sb.append("║  ").append(padRight(line, 54)).append("║\n");
        }
        return sb.toString();
    }

    private String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }
}
