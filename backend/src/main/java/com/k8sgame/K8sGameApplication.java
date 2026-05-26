package com.k8sgame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Kubernetes Learning Game backend.
 *
 * <p>This application provides a Spring Boot backend that:
 * <ul>
 *   <li>Exposes REST endpoints for game state management</li>
 *   <li>Provides WebSocket support for real-time kubectl command execution</li>
 *   <li>Integrates with a real Kubernetes cluster via the Fabric8 client</li>
 *   <li>Manages player namespaces with isolation and resource quotas</li>
 * </ul>
 */
@SpringBootApplication
public class K8sGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(K8sGameApplication.class, args);
    }
}
