/**
 * api.js - Axios HTTP client for the Kubernetes Learning Game REST API.
 *
 * Provides a pre-configured Axios instance and typed helper functions for
 * every backend REST endpoint. All functions return the unwrapped response
 * data so callers don't need to access `.data` themselves.
 *
 * @module services/api
 */

import axios from 'axios';

/**
 * Base Axios instance.
 * The base URL is resolved from the Vite environment variable so that the
 * dev proxy (vite.config.js) handles routing in development, and the
 * production build can point at the real backend.
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ---------------------------------------------------------------------------
// Response / error interceptors
// ---------------------------------------------------------------------------

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.message ||
      'An unexpected error occurred';
    return Promise.reject(new Error(message));
  }
);

// ---------------------------------------------------------------------------
// Game endpoints
// ---------------------------------------------------------------------------

/**
 * Starts a new game session.
 *
 * @returns {Promise<{ sessionId: string, namespace: string, firstChallenge: Object }>}
 */
export const startGame = async () => {
  const { data } = await apiClient.post('/api/game/start');
  return data;
};

/**
 * Retrieves the current game state for a session.
 *
 * @param {string} sessionId - The player's session ID
 * @returns {Promise<{ currentLevel: number, currentChallenge: number, progress: Object }>}
 */
export const getGameState = async (sessionId) => {
  const { data } = await apiClient.get(`/api/game/state/${sessionId}`);
  return data;
};

/**
 * Resets the current challenge namespace to its initial state.
 *
 * @param {string} sessionId - The player's session ID
 * @returns {Promise<{ success: boolean, message: string }>}
 */
export const resetGame = async (sessionId) => {
  const { data } = await apiClient.post(`/api/game/reset/${sessionId}`);
  return data;
};

// ---------------------------------------------------------------------------
// Challenge endpoints
// ---------------------------------------------------------------------------

/**
 * Retrieves the current challenge for a session.
 *
 * @param {string} sessionId - The player's session ID
 * @returns {Promise<Object>} The current Challenge object
 */
export const getCurrentChallenge = async (sessionId) => {
  const { data } = await apiClient.get(`/api/challenges/current/${sessionId}`);
  return data;
};

// ---------------------------------------------------------------------------
// Hint endpoints
// ---------------------------------------------------------------------------

/**
 * Retrieves the next available hint for the current challenge.
 *
 * @param {string} sessionId - The player's session ID
 * @returns {Promise<{ hint: string, hintsRemaining: number }>}
 */
export const getHint = async (sessionId) => {
  const { data } = await apiClient.get(`/api/hints/${sessionId}`);
  return data;
};

export default apiClient;
