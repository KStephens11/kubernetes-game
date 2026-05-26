package com.k8sgame.controller;

import com.k8sgame.model.Challenge;
import com.k8sgame.model.GameState;
import com.k8sgame.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for game session management.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GameController {

    private final GameService gameService;

    /** POST /api/game/start */
    @PostMapping("/game/start")
    public ResponseEntity<Map<String, Object>> startGame() {
        GameState state = gameService.startGame();
        Challenge first = gameService.getCurrentChallenge(state.getSessionId()).orElse(null);
        return ResponseEntity.ok(Map.of(
                "sessionId", state.getSessionId(),
                "namespace", state.getNamespace(),
                "currentLevel", state.getCurrentLevel(),
                "firstChallenge", first != null ? challengeToMap(first) : Map.of()
        ));
    }

    /** GET /api/game/state/{sessionId} */
    @GetMapping("/game/state/{sessionId}")
    public ResponseEntity<Map<String, Object>> getGameState(@PathVariable String sessionId) {
        return gameService.getGameState(sessionId)
                .map(s -> ResponseEntity.ok(stateToMap(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/game/reset/{sessionId} */
    @PostMapping("/game/reset/{sessionId}")
    public ResponseEntity<Map<String, String>> resetGame(@PathVariable String sessionId) {
        if (gameService.getGameState(sessionId).isEmpty()) return ResponseEntity.notFound().build();
        gameService.resetCurrentChallenge(sessionId);
        return ResponseEntity.ok(Map.of("message", "Namespace reset. Good luck!"));
    }

    /** GET /api/challenges/current/{sessionId} */
    @GetMapping("/challenges/current/{sessionId}")
    public ResponseEntity<Map<String, Object>> getCurrentChallenge(@PathVariable String sessionId) {
        return gameService.getCurrentChallenge(sessionId)
                .map(c -> ResponseEntity.ok(challengeToMap(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> stateToMap(GameState s) {
        return Map.of(
                "sessionId", s.getSessionId(),
                "namespace", s.getNamespace(),
                "currentLevel", s.getCurrentLevel(),
                "currentChallengeIndex", s.getCurrentChallengeIndex(),
                "completedChallenges", s.getCompletedChallengeIds(),
                "hintsUsed", s.getHintsUsed(),
                "commandsExecuted", s.getCommandsExecuted()
        );
    }

    private Map<String, Object> challengeToMap(Challenge c) {
        return Map.of(
                "id", c.getId() != null ? c.getId() : "",
                "level", c.getLevel(),
                "title", c.getTitle() != null ? c.getTitle() : "",
                "description", c.getDescription() != null ? c.getDescription() : "",
                "storyContext", c.getStoryContext() != null ? c.getStoryContext() : "",
                "successCriteria", c.getSuccessCriteria() != null ? c.getSuccessCriteria() : List.of(),
                "hints", c.getHints() != null ? c.getHints() : List.of()
        );
    }
}
