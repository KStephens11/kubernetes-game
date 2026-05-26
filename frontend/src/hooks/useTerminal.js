/**
 * useTerminal - Custom hook for managing an xterm.js terminal instance.
 *
 * Encapsulates terminal initialisation, theming, the FitAddon lifecycle,
 * and helper methods for writing coloured output. The consuming component
 * is responsible for providing a DOM ref to mount the terminal into.
 *
 * @module hooks/useTerminal
 */

import { useEffect, useRef, useCallback } from 'react';

/**
 * ANSI escape sequences for terminal colour output.
 * Matches the colour scheme defined in utils/colorScheme.js.
 */
const ANSI = {
  RESET: '\x1b[0m',
  GREEN: '\x1b[1;32m',
  RED: '\x1b[1;31m',
  YELLOW: '\x1b[1;33m',
  CYAN: '\x1b[1;36m',
  MAGENTA: '\x1b[1;35m',
  BLUE: '\x1b[1;34m',
};

/**
 * Default xterm.js theme aligned with the game's dark terminal aesthetic.
 */
const DEFAULT_THEME = {
  background: '#1e1e1e',
  foreground: '#d4d4d4',
  cursor: '#00ff00',
  selection: '#264f78',
  black: '#000000',
  red: '#cd3131',
  green: '#0dbc79',
  yellow: '#e5e510',
  blue: '#2472c8',
  magenta: '#bc3fbc',
  cyan: '#11a8cd',
  white: '#e5e5e5',
};

/**
 * Custom hook for xterm.js terminal management.
 *
 * @param {React.RefObject<HTMLElement>} containerRef - Ref to the DOM element
 *   that the terminal should be mounted into
 * @param {Object} [options] - Optional xterm.js Terminal constructor options
 * @returns {{
 *   write: Function,
 *   writeln: Function,
 *   writeSuccess: Function,
 *   writeError: Function,
 *   writeInfo: Function,
 *   writeStory: Function,
 *   clear: Function,
 *   focus: Function,
 *   fit: Function,
 *   terminalRef: React.MutableRefObject
 * }}
 */
export const useTerminal = (containerRef, options = {}) => {
  /** @type {React.MutableRefObject<import('xterm').Terminal|null>} */
  const terminalRef = useRef(null);
  /** @type {React.MutableRefObject<import('xterm-addon-fit').FitAddon|null>} */
  const fitAddonRef = useRef(null);

  useEffect(() => {
    if (!containerRef.current) return;

    let term;
    let fitAddon;

    const init = async () => {
      const { Terminal } = await import('xterm');
      const { FitAddon } = await import('xterm-addon-fit');

      term = new Terminal({
        cursorBlink: true,
        fontSize: 14,
        fontFamily: 'Menlo, Monaco, "Courier New", monospace',
        theme: DEFAULT_THEME,
        scrollback: 1000,
        ...options,
      });

      fitAddon = new FitAddon();
      term.loadAddon(fitAddon);
      term.open(containerRef.current);
      fitAddon.fit();

      terminalRef.current = term;
      fitAddonRef.current = fitAddon;
    };

    init();

    const handleResize = () => {
      fitAddonRef.current?.fit();
    };
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      terminalRef.current?.dispose();
      terminalRef.current = null;
      fitAddonRef.current = null;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  /** Writes raw text to the terminal (no newline). */
  const write = useCallback((text) => {
    terminalRef.current?.write(text);
  }, []);

  /** Writes a line of text to the terminal (appends \r\n). */
  const writeln = useCallback((text) => {
    terminalRef.current?.writeln(text);
  }, []);

  /** Writes a green success message. */
  const writeSuccess = useCallback((text) => {
    terminalRef.current?.writeln(`${ANSI.GREEN}✓ ${text}${ANSI.RESET}`);
  }, []);

  /** Writes a red error message. */
  const writeError = useCallback((text) => {
    terminalRef.current?.writeln(`${ANSI.RED}✗ ${text}${ANSI.RESET}`);
  }, []);

  /** Writes a cyan informational message. */
  const writeInfo = useCallback((text) => {
    terminalRef.current?.writeln(`${ANSI.CYAN}${text}${ANSI.RESET}`);
  }, []);

  /** Writes a magenta story/narrative message with decorative borders. */
  const writeStory = useCallback((text) => {
    const border = '═'.repeat(60);
    terminalRef.current?.writeln(`\n${ANSI.MAGENTA}╔${border}╗`);
    text.split('\n').forEach((line) => {
      terminalRef.current?.writeln(`║ ${line}`);
    });
    terminalRef.current?.writeln(`╚${border}╝${ANSI.RESET}\n`);
  }, []);

  /** Clears the terminal screen. */
  const clear = useCallback(() => {
    terminalRef.current?.clear();
  }, []);

  /** Focuses the terminal for keyboard input. */
  const focus = useCallback(() => {
    terminalRef.current?.focus();
  }, []);

  /** Re-fits the terminal to its container dimensions. */
  const fit = useCallback(() => {
    fitAddonRef.current?.fit();
  }, []);

  return { write, writeln, writeSuccess, writeError, writeInfo, writeStory, clear, focus, fit, terminalRef };
};

export default useTerminal;
