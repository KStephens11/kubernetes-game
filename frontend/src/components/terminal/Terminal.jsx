import React, { useEffect, useRef, useCallback } from 'react';
import PropTypes from 'prop-types';
import { Terminal as XTerm } from 'xterm';
import 'xterm/css/xterm.css';
import { useWebSocket } from '../../hooks/useWebSocket';
import { useCommandHistory } from '../../hooks/useCommandHistory';
import '../../styles/terminal.css';

const COLORS = {
  SUCCESS: '\x1b[1;32m',
  ERROR:   '\x1b[1;31m',
  WARNING: '\x1b[1;33m',
  INFO:    '\x1b[1;36m',
  STORY:   '\x1b[1;35m',
  RESET:   '\x1b[0m',
};

/**
 * Terminal component using xterm.js for browser-based terminal emulation.
 *
 * @param {Object}   props
 * @param {string}   props.sessionId        - Player's session ID
 * @param {string}   props.namespace        - Player's Kubernetes namespace
 * @param {Function} [props.onCommandExecute] - Callback when a command is executed
 * @param {Function} [props.onValidation]     - Callback when validation result arrives
 */
export const Terminal = ({ sessionId, namespace, onCommandExecute, onValidation }) => {
  const containerRef = useRef(null);
  const xtermRef    = useRef(null);
  const currentLineRef = useRef('');

  const { addCommand, getPrevious, getNext } = useCommandHistory();

  const handleOutput = useCallback((response) => {
    const term = xtermRef.current;
    if (!term) return;
    const color = response.success ? COLORS.SUCCESS : COLORS.ERROR;
    response.output.split('\n').forEach(line => term.writeln(`${color}${line}${COLORS.RESET}`));
    term.write(getPrompt(namespace));
  }, [namespace]);

  const handleValidation = useCallback((result) => {
    if (onValidation) onValidation(result);
    const term = xtermRef.current;
    if (!term) return;
    if (result.success) {
      term.writeln(`\n${COLORS.SUCCESS}✓ Challenge complete!${COLORS.RESET}`);
      term.write(getPrompt(namespace));
    }
  }, [namespace, onValidation]);

  const { connected, sendCommand } = useWebSocket(sessionId, handleOutput, handleValidation);

  const executeCommand = useCallback((command) => {
    const term = xtermRef.current;
    if (!command.trim()) return;
    addCommand(command);
    if (onCommandExecute) onCommandExecute(command);
    sendCommand(command);
  }, [addCommand, onCommandExecute, sendCommand]);

  useEffect(() => {
    if (!containerRef.current) return;

    const term = new XTerm({
      cursorBlink: true,
      fontSize: 14,
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      theme: {
        background: '#1e1e1e',
        foreground: '#d4d4d4',
        cursor: '#00ff00',
        selection: '#264f78',
      },
    });

    term.open(containerRef.current);

    xtermRef.current = term;

    // Welcome banner
    term.writeln(`${COLORS.INFO}╔════════════════════════════════════════╗${COLORS.RESET}`);
    term.writeln(`${COLORS.INFO}║   Kubernetes Learning Game Terminal    ║${COLORS.RESET}`);
    term.writeln(`${COLORS.INFO}╚════════════════════════════════════════╝${COLORS.RESET}`);
    term.writeln('');
    term.writeln(`${COLORS.INFO}Type kubectl commands or 'game help' to get started.${COLORS.RESET}`);
    term.writeln('');
    term.write(getPrompt(namespace));

    // Input handling
    term.onData((data) => {
      const code = data.charCodeAt(0);

      if (code === 13) { // Enter
        const cmd = currentLineRef.current;
        term.writeln('');
        executeCommand(cmd);
        currentLineRef.current = '';
      } else if (code === 3) { // Ctrl+C
        term.writeln('^C');
        currentLineRef.current = '';
        term.write(getPrompt(namespace));
      } else if (code === 127) { // Backspace
        if (currentLineRef.current.length > 0) {
          currentLineRef.current = currentLineRef.current.slice(0, -1);
          term.write('\b \b');
        }
      } else if (data === '\x1b[A') { // Up arrow
        const prev = getPrevious();
        if (prev !== null) {
          clearCurrentLine(term, currentLineRef.current);
          currentLineRef.current = prev;
          term.write(prev);
        }
      } else if (data === '\x1b[B') { // Down arrow
        const next = getNext();
        clearCurrentLine(term, currentLineRef.current);
        currentLineRef.current = next;
        term.write(next);
      } else if (data === '\x1b[C') { // Right arrow
        term.write(data);
      } else if (data === '\x1b[D') { // Left arrow
        term.write(data);
      } else if (code >= 32) { // Printable chars
        currentLineRef.current += data;
        term.write(data);
      }
    });

    const handleResize = () => term.refresh(0, term.rows - 1);
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      term.dispose();
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="terminal-wrapper">
      <div className="terminal-header">
        <span className="terminal-header-dot terminal-header-dot--red" />
        <span className="terminal-header-dot terminal-header-dot--yellow" />
        <span className="terminal-header-dot terminal-header-dot--green" />
        <span className="terminal-header-title">
          {namespace ? `[${namespace}] k8s-game` : 'k8s-game'}
        </span>
        <span className={`terminal-connection-status ${connected ? 'connected' : 'disconnected'}`}>
          {connected ? '● Connected' : '● Disconnected'}
        </span>
      </div>
      <div ref={containerRef} className="terminal-container" />
    </div>
  );
};

function getPrompt(namespace) {
  return `\x1b[1;36m[${namespace || 'k8s-game'}]\x1b[0m $ `;
}

function clearCurrentLine(term, currentLine) {
  for (let i = 0; i < currentLine.length; i++) term.write('\b \b');
}

Terminal.propTypes = {
  sessionId: PropTypes.string,
  namespace: PropTypes.string,
  onCommandExecute: PropTypes.func,
  onValidation: PropTypes.func,
};

export default Terminal;
