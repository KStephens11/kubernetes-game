import React, { useEffect, useContext, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

import { GameProvider, GameContext } from './context/GameContext';
import { GameLayout } from './components/layout/GameLayout';
import { Terminal } from './components/terminal/Terminal';
import { ChallengePanel } from './components/game/ChallengePanel';

import './styles/game.css';

/**
 * Inner game component — has access to GameContext.
 */
function Game() {
  const { sessionId, namespace, currentChallenge, loading, error, startGame, advanceChallenge } =
    useContext(GameContext);

  const [validationResult, setValidationResult] = useState(null);
  const [challengeIndex, setChallengeIndex] = useState(0);
  const [pendingChallenge, setPendingChallenge] = useState(null);

  // Subscribe to /queue/challenge to receive next challenge on progression
  const stompRef = useRef(null);
  useEffect(() => {
    if (!sessionId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws/terminal'),
      connectHeaders: { sessionId },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/user/queue/challenge', (msg) => {
          const next = JSON.parse(msg.body);
          setPendingChallenge(next);
        });
      },
    });
    client.activate();
    stompRef.current = client;

    return () => { client.deactivate(); };
  }, [sessionId, advanceChallenge]);

  useEffect(() => {
    startGame();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) {
    return (
      <div className="app-loading">
        <div className="app-loading__spinner" />
        <p>Initialising game session…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="app-error">
        <p>Failed to connect: {error}</p>
        <button onClick={startGame}>Retry</button>
      </div>
    );
  }

  const header = (
    <div className="game-header">
      <span className="game-header__title">⎈ Kubernetes Learning Game</span>
      {sessionId && <span className="game-header__session">session: {sessionId}</span>}
    </div>
  );

  return (
    <GameLayout
      header={header}
      terminal={
        <Terminal
          sessionId={sessionId}
          namespace={namespace}
          onValidation={setValidationResult}
        />
      }
      challengePanel={
        <ChallengePanel
          challenge={currentChallenge}
          challengeIndex={challengeIndex}
          totalChallenges={15}
          validationResult={validationResult}
          pendingChallenge={pendingChallenge}
          onNext={() => {
            advanceChallenge(pendingChallenge);
            setChallengeIndex((idx) => idx + 1);
            setValidationResult(null);
            setPendingChallenge(null);
          }}
        />
      }
    />
  );
}

function App() {
  return (
    <GameProvider>
      <Game />
    </GameProvider>
  );
}

export default App;
