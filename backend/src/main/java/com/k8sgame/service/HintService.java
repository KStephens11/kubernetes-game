package com.k8sgame.service;

import com.k8sgame.model.Challenge;
import com.k8sgame.model.GameState;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Delivers progressive hints for the current challenge.
 *
 * <p>Hints are ordered by level (1=conceptual, 2=tactical, 3=explicit).
 * Each call to {@link #getNextHint} advances the hint index for the session.
 */
@Service
@RequiredArgsConstructor
public class HintService {

    private final ChallengeLoader challengeLoader;

    @Value("${game.max-hints-per-challenge:3}")
    private int maxHints;

    /**
     * Returns the next available hint for the current challenge.
     *
     * @param state     the player's game state (tracks hints used)
     * @param challenge the current challenge
     * @return a hint response map with {@code hint}, {@code hintsRemaining}, and {@code hintLevel}
     */
    public Optional<Map<String, Object>> getNextHint(GameState state, Challenge challenge) {
        List<Map<String, Object>> hints = challenge.getHints();
        if (hints == null || hints.isEmpty()) {
            return Optional.of(Map.of(
                    "hint", "No hints available for this challenge.",
                    "hintsRemaining", 0,
                    "hintLevel", 0
            ));
        }

        int used = state.getHintsUsed();
        if (used >= maxHints || used >= hints.size()) {
            return Optional.empty(); // max hints reached
        }

        Map<String, Object> hint = hints.get(used);
        state.incrementHintsUsed();

        int remaining = Math.max(0, Math.min(maxHints, hints.size()) - state.getHintsUsed());
        return Optional.of(Map.of(
                "hint", hint.getOrDefault("text", ""),
                "hintsRemaining", remaining,
                "hintLevel", hint.getOrDefault("level", used + 1)
        ));
    }
}
