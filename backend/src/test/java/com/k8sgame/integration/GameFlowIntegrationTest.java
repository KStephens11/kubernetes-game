package com.k8sgame.integration;

import com.k8sgame.model.Challenge;
import com.k8sgame.model.GameState;
import com.k8sgame.service.ChallengeLoader;
import com.k8sgame.service.GameService;
import com.k8sgame.service.HintService;
import com.k8sgame.service.NamespaceService;
import com.k8sgame.service.NarrativeService;
import com.k8sgame.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration tests for the core gameplay flow.
 *
 * <p>Tests the complete loop: start game → challenge progression → hints → narrative.
 * Uses mocked NamespaceService and ValidationService to avoid cluster dependency.
 */
@SpringBootTest
@ActiveProfiles("test")
class GameFlowIntegrationTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private ChallengeLoader challengeLoader;

    @Autowired
    private NarrativeService narrativeService;

    @Autowired
    private HintService hintService;

    @MockBean
    private NamespaceService namespaceService;

    @MockBean
    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        when(namespaceService.getNamespaceName(anyString()))
                .thenAnswer(inv -> "game-session-" + inv.getArgument(0));
        when(namespaceService.setupNamespace(anyString())).thenReturn(true);
        doNothing().when(namespaceService).cleanupNamespace(anyString());
        doNothing().when(namespaceService).resetNamespace(anyString());
        doNothing().when(namespaceService).resetNamespaceWithResources(anyString(), any());
    }

    // ─── Game session lifecycle ───────────────────────────────────────────────

    @Test
    void startGame_createsSessionWithFirstChallenge() {
        GameState state = gameService.startGame();

        assertThat(state.getSessionId()).isNotBlank();
        assertThat(state.getNamespace()).startsWith("game-session-");
        assertThat(state.getCurrentLevel()).isEqualTo(1);
        assertThat(state.getCurrentChallengeIndex()).isEqualTo(0);

        Optional<Challenge> first = gameService.getCurrentChallenge(state.getSessionId());
        assertThat(first).isPresent();
        assertThat(first.get().getLevel()).isEqualTo(1);
    }

    @Test
    void getGameState_returnsStateForExistingSession() {
        GameState created = gameService.startGame();

        Optional<GameState> retrieved = gameService.getGameState(created.getSessionId());

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getSessionId()).isEqualTo(created.getSessionId());
    }

    @Test
    void getGameState_returnsEmptyForUnknownSession() {
        assertThat(gameService.getGameState("nonexistent")).isEmpty();
    }

    @Test
    void endGame_removesSession() {
        GameState state = gameService.startGame();
        String sid = state.getSessionId();

        gameService.endGame(sid);

        assertThat(gameService.getGameState(sid)).isEmpty();
    }

    // ─── Challenge progression ────────────────────────────────────────────────

    @Test
    void advanceChallenge_movesToNextChallenge() {
        GameState state = gameService.startGame();
        String sid = state.getSessionId();
        String firstId = gameService.getCurrentChallenge(sid).get().getId();

        Optional<Challenge> next = gameService.advanceChallenge(sid);

        assertThat(next).isPresent();
        assertThat(next.get().getId()).isNotEqualTo(firstId);
        assertThat(gameService.getGameState(sid).get().getCurrentChallengeIndex()).isEqualTo(1);
    }

    @Test
    void advanceChallenge_marksCurrentChallengeComplete() {
        GameState state = gameService.startGame();
        String sid = state.getSessionId();
        String firstId = gameService.getCurrentChallenge(sid).get().getId();

        gameService.advanceChallenge(sid);

        assertThat(gameService.getGameState(sid).get().getCompletedChallengeIds())
                .contains(firstId);
    }

    @Test
    void advanceChallenge_returnsEmptyForUnknownSession() {
        assertThat(gameService.advanceChallenge("unknown")).isEmpty();
    }

    // ─── Reset functionality ──────────────────────────────────────────────────

    @Test
    void resetCurrentChallenge_doesNotChangeProgress() {
        GameState state = gameService.startGame();
        String sid = state.getSessionId();
        int indexBefore = state.getCurrentChallengeIndex();

        gameService.resetCurrentChallenge(sid);

        assertThat(gameService.getGameState(sid).get().getCurrentChallengeIndex())
                .isEqualTo(indexBefore);
    }

    @Test
    void resetCurrentChallenge_doesNothingForUnknownSession() {
        // Should not throw
        gameService.resetCurrentChallenge("unknown");
    }

    // ─── Hint system ──────────────────────────────────────────────────────────

    @Test
    void getHint_returnsFirstHintOnFirstRequest() {
        GameState state = gameService.startGame();
        String sid = state.getSessionId();
        Challenge challenge = gameService.getCurrentChallenge(sid).get();

        if (challenge.getHints() != null && !challenge.getHints().isEmpty()) {
            Optional<Map<String, Object>> hint = hintService.getNextHint(state, challenge);
            assertThat(hint).isPresent();
            assertThat(hint.get().get("hint").toString()).isNotBlank();
            assertThat(state.getHintsUsed()).isEqualTo(1);
        }
    }

    @Test
    void getHint_returnsEmptyWhenMaxHintsReached() {
        GameState state = gameService.startGame();
        String sid = state.getSessionId();
        Challenge challenge = gameService.getCurrentChallenge(sid).get();

        // Exhaust all hints
        for (int i = 0; i < 3; i++) {
            hintService.getNextHint(state, challenge);
        }

        Optional<Map<String, Object>> result = hintService.getNextHint(state, challenge);
        assertThat(result).isEmpty();
    }

    // ─── Narrative service ────────────────────────────────────────────────────

    @Test
    void narrativeService_returnsLevelIntroForAllLevels() {
        for (int level = 1; level <= 5; level++) {
            String intro = narrativeService.getLevelIntro(level);
            assertThat(intro).isNotBlank();
            assertThat(intro).contains("LEVEL " + level);
        }
    }

    @Test
    void narrativeService_returnsLevelCompleteForAllLevels() {
        for (int level = 1; level <= 5; level++) {
            String complete = narrativeService.getLevelComplete(level);
            assertThat(complete).isNotBlank();
            assertThat(complete).contains("LEVEL " + level);
        }
    }

    @Test
    void narrativeService_formatsBoxWithBorders() {
        String intro = narrativeService.getLevelIntro(1);
        assertThat(intro).contains("╔");
        assertThat(intro).contains("╚");
        assertThat(intro).contains("║");
    }

    // ─── Challenge content ────────────────────────────────────────────────────

    @Test
    void challengeLoader_loadsAtLeast15Challenges() {
        int count = 0;
        Optional<Challenge> c = challengeLoader.getChallengeAtIndex(count);
        while (c.isPresent()) {
            count++;
            c = challengeLoader.getChallengeAtIndex(count);
        }
        assertThat(count).isGreaterThanOrEqualTo(15);
    }

    @Test
    void challengeLoader_allChallengesHaveRequiredFields() {
        int index = 0;
        Optional<Challenge> c = challengeLoader.getChallengeAtIndex(index);
        while (c.isPresent()) {
            Challenge challenge = c.get();
            assertThat(challenge.getId())
                    .as("Challenge at index %d has no id", index).isNotBlank();
            assertThat(challenge.getTitle())
                    .as("Challenge %s has no title", challenge.getId()).isNotBlank();
            assertThat(challenge.getLevel())
                    .as("Challenge %s has no level", challenge.getId()).isGreaterThan(0);
            assertThat(challenge.getSuccessCriteria())
                    .as("Challenge %s has no criteria", challenge.getId()).isNotEmpty();
            index++;
            c = challengeLoader.getChallengeAtIndex(index);
        }
    }

    @Test
    void challengeLoader_challengesSpanFiveLevels() {
        int maxLevel = 0;
        int index = 0;
        Optional<Challenge> c = challengeLoader.getChallengeAtIndex(index);
        while (c.isPresent()) {
            maxLevel = Math.max(maxLevel, c.get().getLevel());
            index++;
            c = challengeLoader.getChallengeAtIndex(index);
        }
        assertThat(maxLevel).isGreaterThanOrEqualTo(5);
    }

    // ─── Error scenarios ──────────────────────────────────────────────────────

    @Test
    void getCurrentChallenge_returnsEmptyForUnknownSession() {
        assertThat(gameService.getCurrentChallenge("unknown")).isEmpty();
    }
}
