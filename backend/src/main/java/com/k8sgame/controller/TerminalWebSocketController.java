package com.k8sgame.controller;

import com.k8sgame.model.Challenge;
import com.k8sgame.model.CommandRequest;
import com.k8sgame.model.CommandResponse;
import com.k8sgame.model.GameState;
import com.k8sgame.model.ValidationResult;
import com.k8sgame.service.CommandService;
import com.k8sgame.service.GameService;
import com.k8sgame.service.HintService;
import com.k8sgame.service.NarrativeService;
import com.k8sgame.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;

/**
 * WebSocket controller that handles terminal command messages from the browser.
 *
 * <p>Clients send to {@code /app/command}; responses are delivered to
 * {@code /user/queue/output} and validation results to {@code /user/queue/validation}.
 */
@Controller
@RequiredArgsConstructor
public class TerminalWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(TerminalWebSocketController.class);

    private final CommandService commandService;
    private final GameService gameService;
    private final ValidationService validationService;
    private final NarrativeService narrativeService;
    private final HintService hintService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles an incoming command from the browser terminal.
     *
     * @param request the command payload containing sessionId and command string
     */
    @MessageMapping("/command")
    public void handleCommand(@Payload CommandRequest request) {
        String sessionId = request.sessionId();
        String command = request.command();

        logger.debug("Command received: session={}, command={}", sessionId, command);

        // Resolve namespace
        String namespace = gameService.getGameState(sessionId)
                .map(GameState::getNamespace)
                .orElse(null);

        if (namespace == null) {
            sendOutput(sessionId, CommandResponse.error("Session not found. Please start a new game."));
            return;
        }

        // Handle game commands
        if (command.startsWith("game ") || command.equals("game")) {
            handleGameCommand(sessionId, command);
            return;
        }

        // Execute kubectl command
        CommandResponse response = commandService.execute(namespace, command);
        gameService.getGameState(sessionId).ifPresent(GameState::incrementCommandsExecuted);
        sendOutput(sessionId, response);

        // Async validation after command execution
        if (response.success()) {
            CompletableFuture.runAsync(() -> triggerValidation(sessionId, namespace));
        }
    }

    private void handleGameCommand(String sessionId, String command) {
        String[] parts = command.split("\\s+", 2);
        String sub = parts.length > 1 ? parts[1] : "";

        switch (sub) {
            case "hint" -> {
                gameService.getGameState(sessionId).ifPresentOrElse(state ->
                    gameService.getCurrentChallenge(sessionId).ifPresentOrElse(challenge -> {
                        var hint = hintService.getNextHint(state, challenge);
                        if (hint.isPresent()) {
                            var h = hint.get();
                            sendOutput(sessionId, CommandResponse.success(
                                "\u001b[1;34m💡 Hint " + h.get("hintLevel") + ": " + h.get("hint")
                                + "\u001b[0m\n   (" + h.get("hintsRemaining") + " hints remaining)"));
                        } else {
                            sendOutput(sessionId, CommandResponse.success(
                                "\u001b[1;33mNo more hints available for this challenge.\u001b[0m"));
                        }
                    }, () -> sendOutput(sessionId, CommandResponse.error("No active challenge."))),
                    () -> sendOutput(sessionId, CommandResponse.error("Session not found.")));
            }
            case "status" -> {
                gameService.getGameState(sessionId).ifPresentOrElse(
                        state -> sendOutput(sessionId, CommandResponse.success(
                                "Level: " + state.getCurrentLevel()
                                + " | Hints used: " + state.getHintsUsed()
                                + " | Commands: " + state.getCommandsExecuted())),
                        () -> sendOutput(sessionId, CommandResponse.error("Session not found.")));
            }
            case "reset" -> {
                gameService.resetCurrentChallenge(sessionId);
                sendOutput(sessionId, CommandResponse.success(
                        "\u001b[1;33m⟳ Namespace reset. Initial resources recreated.\u001b[0m"));
                // Re-send current challenge so UI panel refreshes
                gameService.getCurrentChallenge(sessionId).ifPresent(c ->
                        messagingTemplate.convertAndSendToUser(sessionId, "/queue/challenge", c));
            }
            default -> sendOutput(sessionId, CommandResponse.success(
                    "Game commands: game hint | game status | game reset"));
        }
    }

    private void triggerValidation(String sessionId, String namespace) {
        try {
            gameService.getCurrentChallenge(sessionId).ifPresent(challenge -> {
                ValidationResult result = validationService.validate(namespace, challenge.getSuccessCriteria());
                messagingTemplate.convertAndSendToUser(sessionId, "/queue/validation", result);

                if (result.success()) {
                    int completedLevel = challenge.getLevel();
                    gameService.advanceChallenge(sessionId).ifPresentOrElse(
                            next -> {
                                // Level transition narrative
                                if (next.getLevel() > completedLevel) {
                                    String levelComplete = narrativeService.getLevelComplete(completedLevel);
                                    String levelIntro = narrativeService.getLevelIntro(next.getLevel());
                                    sendOutput(sessionId, CommandResponse.success(levelComplete + levelIntro));
                                } else {
                                    sendOutput(sessionId, CommandResponse.success(
                                            "\n\u001b[1;32m✓ Challenge complete! Next: " + next.getTitle() + "\u001b[0m"));
                                }
                                // Send next challenge to update the UI panel
                                messagingTemplate.convertAndSendToUser(sessionId, "/queue/challenge", next);
                            },
                            () -> {
                                String finale = narrativeService.getLevelComplete(completedLevel);
                                sendOutput(sessionId, CommandResponse.success(
                                        finale + "\n\u001b[1;33m🏆 You completed all challenges! Well done!\u001b[0m\n"));
                            }
                    );
                }
            });
        } catch (Exception e) {
            logger.warn("Validation error for session {}: {}", sessionId, e.getMessage());
        }
    }

    private void sendOutput(String sessionId, CommandResponse response) {
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/output", response);
    }
}
