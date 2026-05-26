package com.k8sgame.controller;

import com.k8sgame.model.Challenge;
import com.k8sgame.model.GameState;
import com.k8sgame.service.GameService;
import com.k8sgame.service.HintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for hint delivery.
 */
@RestController
@RequestMapping("/api/hints")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HintController {

    private final GameService gameService;
    private final HintService hintService;

    /** GET /api/hints/{sessionId} */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getNextHint(@PathVariable String sessionId) {
        Optional<GameState> stateOpt = gameService.getGameState(sessionId);
        if (stateOpt.isEmpty()) return ResponseEntity.notFound().build();

        Optional<Challenge> challengeOpt = gameService.getCurrentChallenge(sessionId);
        if (challengeOpt.isEmpty()) return ResponseEntity.notFound().build();

        Optional<Map<String, Object>> hint = hintService.getNextHint(stateOpt.get(), challengeOpt.get());
        if (hint.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No more hints available for this challenge."));
        }
        return ResponseEntity.ok(hint.get());
    }
}
