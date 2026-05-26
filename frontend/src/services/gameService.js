/**
 * gameService.js - High-level game orchestration service.
 *
 * Combines REST API calls (api.js) with local state helpers to provide a
 * single entry point for game lifecycle operations. Components and hooks
 * should prefer this service over calling api.js directly.
 *
 * @module services/gameService
 */

import { startGame, getGameState, getCurrentChallenge, resetGame } from './api';

/**
 * Initialises a new game session.
 *
 * Calls the backend to create a session, then returns the session ID,
 * namespace, and first challenge in a single object.
 *
 * @returns {Promise<{
 *   sessionId: string,
 *   namespace: string,
 *   currentChallenge: Object
 * }>}
 */
export const initGame = async () => {
  const { sessionId, namespace, firstChallenge } = await startGame();
  return { sessionId, namespace, currentChallenge: firstChallenge };
};

/**
 * Loads the full game state for an existing session.
 *
 * @param {string} sessionId - The player's session ID
 * @returns {Promise<{
 *   sessionId: string,
 *   currentLevel: number,
 *   currentChallenge: Object,
 *   progress: Object
 * }>}
 */
export const loadGame = async (sessionId) => {
  const [state, challenge] = await Promise.all([
    getGameState(sessionId),
    getCurrentChallenge(sessionId),
  ]);

  return {
    sessionId,
    currentLevel: state.currentLevel,
    currentChallenge: challenge,
    progress: state,
  };
};

/**
 * Resets the current challenge namespace to its initial state.
 *
 * @param {string} sessionId - The player's session ID
 * @returns {Promise<{ success: boolean, message: string }>}
 */
export const resetCurrentChallenge = async (sessionId) => {
  return resetGame(sessionId);
};
