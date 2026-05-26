import React, { createContext, useState, useCallback } from 'react';
import { initGame, resetCurrentChallenge } from '../services/gameService';

export const GameContext = createContext(null);

/**
 * Provides game session state to the component tree.
 */
export const GameProvider = ({ children }) => {
  const [sessionId, setSessionId] = useState(null);
  const [namespace, setNamespace] = useState(null);
  const [currentChallenge, setCurrentChallenge] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const startGame = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { sessionId: sid, namespace: ns, currentChallenge: challenge } = await initGame();
      setSessionId(sid);
      setNamespace(ns);
      setCurrentChallenge(challenge);
    } catch (err) {
      setError(err.message || 'Failed to start game');
    } finally {
      setLoading(false);
    }
  }, []);

  const advanceChallenge = useCallback((challenge) => {
    setCurrentChallenge(challenge);
  }, []);

  const resetGame = useCallback(async () => {
    if (!sessionId) return;
    await resetCurrentChallenge(sessionId);
  }, [sessionId]);

  return (
    <GameContext.Provider value={{
      sessionId, namespace, currentChallenge,
      loading, error,
      startGame, advanceChallenge, resetGame,
    }}>
      {children}
    </GameContext.Provider>
  );
};

export default GameContext;
