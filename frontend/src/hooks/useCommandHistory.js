import { useState, useRef, useCallback } from 'react';

export const useCommandHistory = (maxSize = 100) => {
  const [history, setHistory] = useState([]);
  const historyRef = useRef([]);
  const indexRef = useRef(-1);

  const addCommand = useCallback((command) => {
    if (!command || !command.trim()) return;
    setHistory((prev) => {
      const updated = [...prev, command];
      const trimmed = updated.length > maxSize ? updated.slice(updated.length - maxSize) : updated;
      historyRef.current = trimmed;
      return trimmed;
    });
    indexRef.current = -1;
  }, [maxSize]);

  const getPrevious = useCallback(() => {
    const h = historyRef.current;
    if (h.length === 0) return null;
    const next = indexRef.current + 1;
    if (next >= h.length) return null;
    indexRef.current = next;
    return h[h.length - 1 - next];
  }, []);

  const getNext = useCallback(() => {
    if (indexRef.current <= 0) {
      indexRef.current = -1;
      return '';
    }
    indexRef.current -= 1;
    const h = historyRef.current;
    return h[h.length - 1 - indexRef.current] ?? '';
  }, []);

  return { addCommand, getPrevious, getNext, history };
};

export default useCommandHistory;
