import React from 'react';
import PropTypes from 'prop-types';

import '../../styles/game.css';

/**
 * GameLayout component providing the main two-column game interface.
 * Terminal on the left, challenge panel on the right.
 *
 * @param {Object} props - Component props
 * @param {React.ReactNode} [props.terminal] - Terminal component slot
 * @param {React.ReactNode} [props.challengePanel] - Challenge panel slot
 * @param {React.ReactNode} [props.header] - Header slot
 * @returns {JSX.Element} GameLayout component
 */
export const GameLayout = ({ terminal, challengePanel, header }) => {
  // Placeholder: full layout implementation in task 10.2
  return (
    <div className="game-layout">
      {header && <div className="game-layout__header">{header}</div>}
      <div className="game-layout__body">
        <div className="game-layout__terminal">{terminal}</div>
        <div className="game-layout__panel">{challengePanel}</div>
      </div>
    </div>
  );
};

GameLayout.propTypes = {
  terminal: PropTypes.node,
  challengePanel: PropTypes.node,
  header: PropTypes.node,
};

export default GameLayout;
