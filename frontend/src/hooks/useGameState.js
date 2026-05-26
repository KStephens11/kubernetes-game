/**
 * useGameState - Custom hook for accessing and updating game state.
 *
 * Provides a convenient interface to the GameContext, ensuring the hook
 * is always used within a GameProvider tree.
 *
 * @module hooks/useGameState
 */

import { useContext } from 'react';
import { GameContext } from '../context/GameContext';

/**
 * Custom hook that exposes the current game state and update helpers.
 *
 * Must be used inside a <GameProvider> component.
 *
 * @returns {{
 *   sessionId: string|null,
 *   currentChallenge: Object|null,
 *   completedChallenges: string[],
 *   hintsUsed: number,
 *   loading: boolean,
 *   error: string|null,
 *   startGame: Function,
 *   advanceChallenge: Function,
 *   resetGame: Function
 * }}
 * @throws {Error} When used outside of a GameProvider
 */
export const useGameState = () => {
  const context = useContext(GameContext);

  if (!context) {
    throw new Error('useGameState must be used within a <GameProvider>');
  }

  return context;
};

export default useGameState;
