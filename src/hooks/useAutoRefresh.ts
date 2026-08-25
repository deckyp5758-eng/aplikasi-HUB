import { useEffect, useRef } from 'react';
import { AUTO_REFRESH_INTERVAL_MS } from '../utils/constants';

export function useAutoRefresh(callback: () => void | Promise<void>, intervalMs: number = AUTO_REFRESH_INTERVAL_MS, enabled: boolean = true) {
  const savedCallback = useRef(callback);

  useEffect(() => {
    savedCallback.current = callback;
  }, [callback]);

  useEffect(() => {
    if (!enabled) return;

    const tick = () => {
      savedCallback.current();
    };

    const id = setInterval(tick, intervalMs);
    return () => clearInterval(id);
  }, [intervalMs, enabled]);
}
