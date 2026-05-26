package com.k8sgame.model;

/**
 * Incoming command request from the WebSocket terminal.
 *
 * @param sessionId the player's session ID
 * @param command   the raw command string (e.g. "kubectl get pods")
 */
public record CommandRequest(String sessionId, String command) {}
