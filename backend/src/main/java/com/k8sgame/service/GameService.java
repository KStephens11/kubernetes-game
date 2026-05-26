package com.k8sgame.service;

import com.k8sgame.model.Challenge;
import com.k8sgame.model.GameState;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages in-memory game sessions.
 *
 * <p>Each session has a unique ID, an isolated namespace, and tracks
 * the player's progress through the ordered challenge list.
 */
@Service
@RequiredArgsConstructor
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private final NamespaceService namespaceService;
    private final ChallengeLoader challengeLoader;

    private final ConcurrentHashMap<String, GameState> sessions = new ConcurrentHashMap<>();

    /**
     * Creates a new game session, sets up the player namespace, and returns the initial state.
     *
     * @return the newly created {@link GameState}
     */
    public GameState startGame() {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        String namespace = namespaceService.getNamespaceName(sessionId);

        namespaceService.setupNamespace(sessionId);

        GameState state = new GameState(sessionId, namespace);
        sessions.put(sessionId, state);
        logger.info("Game started: session={}, namespace={}", sessionId, namespace);
        return state;
    }

    /**
     * Returns the game state for a session.
     *
     * @param sessionId the session ID
     * @return an Optional containing the state, or empty if not found
     */
    public Optional<GameState> getGameState(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * Returns the current challenge for a session.
     *
     * @param sessionId the session ID
     * @return an Optional containing the current challenge
     */
    public Optional<Challenge> getCurrentChallenge(String sessionId) {
        return getGameState(sessionId)
                .flatMap(state -> challengeLoader.getChallengeAtIndex(state.getCurrentChallengeIndex()));
    }

    /**
     * Advances the session to the next challenge.
     *
     * @param sessionId the session ID
     * @return the next challenge, or empty if all challenges are complete
     */
    public Optional<Challenge> advanceChallenge(String sessionId) {
        return getGameState(sessionId).flatMap(state -> {
            getCurrentChallenge(sessionId).ifPresent(c -> state.markChallengeComplete(c.getId()));

            int nextIndex = state.getCurrentChallengeIndex() + 1;
            Optional<Challenge> next = challengeLoader.getChallengeAtIndex(nextIndex);

            if (next.isPresent()) {
                state.setCurrentChallengeIndex(nextIndex);
                state.setCurrentLevel(next.get().getLevel());
                logger.info("Session {} advanced to challenge index {}", sessionId, nextIndex);
            } else {
                logger.info("Session {} completed all challenges", sessionId);
            }

            return next;
        });
    }

    /**
     * Resets the current challenge namespace and recreates its initial resources.
     *
     * @param sessionId the session ID
     */
    public void resetCurrentChallenge(String sessionId) {
        getGameState(sessionId).ifPresent(state -> {
            getCurrentChallenge(sessionId).ifPresentOrElse(
                    challenge -> namespaceService.resetNamespaceWithResources(
                            sessionId, challenge.getInitialResources()),
                    () -> namespaceService.resetNamespace(sessionId)
            );
            logger.info("Session {} namespace reset with initial resources", sessionId);
        });
    }

    /**
     * Ends a session and cleans up the namespace.
     *
     * @param sessionId the session ID
     */
    public void endGame(String sessionId) {
        sessions.remove(sessionId);
        namespaceService.cleanupNamespace(sessionId);
        logger.info("Session {} ended", sessionId);
    }
}
