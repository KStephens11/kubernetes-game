/**
 * websocket.js - Low-level WebSocket connection factory.
 *
 * Provides a thin wrapper around SockJS + STOMP that creates and manages a
 * single client instance. Higher-level connection logic lives in the
 * useWebSocket hook; this module is responsible only for construction and
 * teardown so it can be tested or replaced independently.
 *
 * @module services/websocket
 */

/**
 * WebSocket endpoint path — proxied by Vite in development.
 */
export const WS_ENDPOINT = '/ws/terminal';

/**
 * STOMP destination for sending commands from client to server.
 */
export const COMMAND_DESTINATION = '/app/command';

/**
 * STOMP subscription path for receiving command output.
 */
export const OUTPUT_QUEUE = '/user/queue/output';

/**
 * STOMP subscription path for receiving validation results.
 */
export const VALIDATION_QUEUE = '/user/queue/validation';

/**
 * Creates and activates a STOMP client connected via SockJS.
 *
 * @param {Object} handlers - Event handler callbacks
 * @param {Function} handlers.onConnect - Called when the connection is established
 * @param {Function} handlers.onDisconnect - Called when the connection is lost
 * @param {Function} [handlers.onError] - Called on STOMP-level errors
 * @returns {Promise<import('@stomp/stompjs').Client>} The activated STOMP client
 */
export const createStompClient = async ({ onConnect, onDisconnect, onError }) => {
  const SockJS = (await import('sockjs-client')).default;
  const { Client } = await import('@stomp/stompjs');

  const client = new Client({
    webSocketFactory: () => new SockJS(WS_ENDPOINT),
    reconnectDelay: 5000,
    onConnect,
    onDisconnect,
    onStompError: onError ?? ((frame) => console.error('STOMP error', frame)),
  });

  client.activate();
  return client;
};

/**
 * Safely deactivates a STOMP client if it exists.
 *
 * @param {import('@stomp/stompjs').Client|null} client - The client to deactivate
 * @returns {Promise<void>}
 */
export const destroyStompClient = async (client) => {
  if (client) {
    await client.deactivate();
  }
};
