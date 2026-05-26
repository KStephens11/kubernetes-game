import React from 'react';
import { render, screen } from '@testing-library/react';
import { Terminal } from '../components/terminal/Terminal';

// Mock the hooks so we don't need a real WebSocket
jest.mock('../hooks/useWebSocket', () => ({
  useWebSocket: () => ({ connected: false, sendCommand: jest.fn(), disconnect: jest.fn() }),
}));

jest.mock('../hooks/useCommandHistory', () => ({
  useCommandHistory: () => ({
    addCommand: jest.fn(),
    getPrevious: jest.fn(() => null),
    getNext: jest.fn(() => ''),
  }),
}));

describe('Terminal', () => {
  it('renders the terminal wrapper', () => {
    const { container } = render(<Terminal sessionId="abc" namespace="game-session-abc" />);
    expect(container.querySelector('.terminal-wrapper')).toBeInTheDocument();
  });

  it('shows namespace in header', () => {
    render(<Terminal sessionId="abc" namespace="game-session-abc" />);
    expect(screen.getByText('[game-session-abc] k8s-game')).toBeInTheDocument();
  });

  it('shows disconnected status when not connected', () => {
    render(<Terminal sessionId="abc" namespace="game-session-abc" />);
    expect(screen.getByText('● Disconnected')).toBeInTheDocument();
  });
});
