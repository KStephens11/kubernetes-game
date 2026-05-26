/**
 * commandService.js - Command routing and classification service.
 *
 * Determines whether a terminal input string is a kubectl command, a game
 * command, or an unknown command, and provides helpers for formatting
 * command output before it is written to the terminal.
 *
 * Actual execution of kubectl commands is handled server-side; this module
 * only classifies and pre-processes commands on the client.
 *
 * @module services/commandService
 */

/**
 * Enum-like constants for command types.
 */
export const CommandType = Object.freeze({
  KUBECTL: 'kubectl',
  GAME: 'game',
  UNKNOWN: 'unknown',
});

/**
 * Supported game sub-commands.
 */
export const GAME_COMMANDS = Object.freeze([
  'hint',
  'status',
  'reset',
  'progress',
  'help',
]);

/**
 * Classifies a raw terminal input string into a CommandType.
 *
 * @param {string} input - The raw command string entered by the player
 * @returns {'kubectl'|'game'|'unknown'} The command type
 */
export const classifyCommand = (input) => {
  const trimmed = (input || '').trim();

  if (trimmed.startsWith('kubectl ') || trimmed === 'kubectl') {
    return CommandType.KUBECTL;
  }

  if (trimmed.startsWith('game ') || GAME_COMMANDS.includes(trimmed)) {
    return CommandType.GAME;
  }

  return CommandType.UNKNOWN;
};

/**
 * Extracts the game sub-command from a game command string.
 *
 * @param {string} input - A game command string (e.g. "game hint", "hint")
 * @returns {string} The sub-command (e.g. "hint")
 */
export const extractGameSubCommand = (input) => {
  const trimmed = (input || '').trim();
  if (trimmed.startsWith('game ')) {
    return trimmed.slice(5).trim();
  }
  return trimmed;
};

/**
 * Returns a help text string listing available game commands.
 *
 * @returns {string}
 */
export const getHelpText = () => {
  return [
    'Available commands:',
    '  kubectl <command>  — Execute a kubectl command against your namespace',
    '  hint / game hint   — Request a hint for the current challenge',
    '  status             — Show current challenge status',
    '  reset              — Reset the namespace to the challenge start state',
    '  progress           — Show your overall progress',
    '  help               — Show this help message',
  ].join('\n');
};
