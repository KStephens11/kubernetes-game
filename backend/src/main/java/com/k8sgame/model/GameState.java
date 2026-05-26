package com.k8sgame.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory game state for a single player session.
 */
public class GameState {

    private final String sessionId;
    private final String namespace;
    private int currentLevel;
    private int currentChallengeIndex;
    private final List<String> completedChallengeIds = new ArrayList<>();
    private int hintsUsed;
    private int commandsExecuted;
    private final Instant sessionStart = Instant.now();

    public GameState(String sessionId, String namespace) {
        this.sessionId = sessionId;
        this.namespace = namespace;
        this.currentLevel = 1;
        this.currentChallengeIndex = 0;
    }

    // Getters

    public String getSessionId() { return sessionId; }
    public String getNamespace() { return namespace; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    public int getCurrentChallengeIndex() { return currentChallengeIndex; }
    public void setCurrentChallengeIndex(int currentChallengeIndex) { this.currentChallengeIndex = currentChallengeIndex; }
    public List<String> getCompletedChallengeIds() { return completedChallengeIds; }
    public int getHintsUsed() { return hintsUsed; }
    public void incrementHintsUsed() { this.hintsUsed++; }
    public int getCommandsExecuted() { return commandsExecuted; }
    public void incrementCommandsExecuted() { this.commandsExecuted++; }
    public Instant getSessionStart() { return sessionStart; }

    public void markChallengeComplete(String challengeId) {
        if (!completedChallengeIds.contains(challengeId)) {
            completedChallengeIds.add(challengeId);
        }
    }
}
