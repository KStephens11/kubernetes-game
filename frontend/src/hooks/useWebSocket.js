/**
 * useWebSocket - Custom hook for managing WebSocket connections.
 *
 * Manages the SockJS + STOMP WebSocket connection to the backend terminal
 * endpoint. Handles connection lifecycle, subscriptions, and command sending.
 *
 * @module hooks/useWebSocket
 */

import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * Custom hook for WebSocket connection management.
 *
 * Connects to the backend /ws/terminal endpoint using SockJS and STOMP.
 * Subscribes to command output and validation result queues.
 *
 * @param {string} sessionId - The player's session ID used to scope messages
 * @param {Function} [onOutput] - Callback invoked with command output messages
 * @param {Function} [onValidation] - Callback invoked with validation result messages
 * @returns {{ connected: boolean, sendCommand: Function, disconnect: Function }}
 */
export const useWebSocket = (sessionId, onOutput, onValidation) => {
  const [connected, setConnected] = useState(false);
  const connectedRef = useRef(false);
  const stompClientRef = useRef(null);

  useEffect(() => {
    connectedRef.current = connected;
  }, [connected]);

  useEffect(() => {
    if (!sessionId) return;

    // Lazy-import to avoid SSR issues and keep bundle splitting intact
    let client;

    const connect = async () => {
      const SockJS = (await import('sockjs-client')).default;
      const { Client } = await import('@stomp/stompjs');

      client = new Client({
        webSocketFactory: () => new SockJS('http://localhost:8080/ws/terminal'),
        connectHeaders: { sessionId },
        reconnectDelay: 5000,
        onConnect: () => {
          console.log('[WS] Connected, sessionId:', sessionId);
          setConnected(true);

          client.subscribe('/user/queue/output', (message) => {
            console.log('[WS] Output received:', message.body);
            if (onOutput) onOutput(JSON.parse(message.body));
          });

          client.subscribe('/user/queue/validation', (message) => {
            console.log('[WS] Validation received:', message.body);
            if (onValidation) onValidation(JSON.parse(message.body));
          });
        },
        onDisconnect: () => {
          console.log('[WS] Disconnected');
          setConnected(false);
        },
        onWebSocketClose: (e) => {
          console.log('[WS] WebSocket closed:', e);
          setConnected(false);
        },
        onStompError: (frame) => {
          console.error('[WS] STOMP error:', frame);
          setConnected(false);
        },
      });

      client.activate();
      stompClientRef.current = client;
    };

    connect();

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
        stompClientRef.current = null;
      }
      setConnected(false);
    };
  }, [sessionId]); // eslint-disable-line react-hooks/exhaustive-deps

  /**
   * Sends a kubectl or game command to the backend via WebSocket.
   *
   * @param {string} command - The command string to execute
   */
  const sendCommand = useCallback(
    (command) => {
      console.log('[WS] sendCommand, connected:', connectedRef.current, 'client:', !!stompClientRef.current);
      if (stompClientRef.current && connectedRef.current) {
        console.log('[WS] Publishing:', command, 'sessionId:', sessionId);
        stompClientRef.current.publish({
          destination: '/app/command',
          body: JSON.stringify({ sessionId, command }),
        });
      } else {
        console.warn('[WS] Not connected — command dropped:', command);
      }
    },
    [sessionId]
  );

  /**
   * Manually disconnects the WebSocket client.
   */
  const disconnect = useCallback(() => {
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }
    setConnected(false);
  }, []);

  return { connected, sendCommand, disconnect };
};

export default useWebSocket;
