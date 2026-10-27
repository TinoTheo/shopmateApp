'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

export type Load<T> = {
  state: 'loading' | 'ready' | 'failed';
  data: T | null;
  /** When the data on screen arrived. Changes only when new data does. */
  updatedAt: Date | null;
  /** Offline with data still on screen: show it "as of" [updatedAt]. */
  offline: boolean;
  failedAt: Date | null;
  retry: () => void;
};

const TIMEOUT_MS = 10_000;
const POLL_MS = 60_000;

function withTimeout<T>(p: Promise<T>): Promise<T> {
  return Promise.race([p, new Promise<T>((_, reject) => setTimeout(() => reject(new Error('timeout')), TIMEOUT_MS))]);
}

/**
 * One page's data: loads on [key], refreshes every minute while the tab is visible, and again
 * when the connection comes back. After 10 seconds without an answer it gives up and says so.
 * Offline keeps what's on screen — offline is a state, not an error.
 */
export function useLoad<T>(key: string, fetcher: () => Promise<T>): Load<T> {
  const [state, setState] = useState<Load<T>['state']>('loading');
  const [data, setData] = useState<T | null>(null);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);
  const [offline, setOffline] = useState(false);
  const [failedAt, setFailedAt] = useState<Date | null>(null);
  const fn = useRef(fetcher);
  fn.current = fetcher;
  const run = useRef(0);
  const hasData = useRef(false);

  const load = useCallback(async (quiet: boolean) => {
    const id = ++run.current;
    if (!quiet) setState('loading');
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      setOffline(true);
      if (!hasData.current) setState('ready');
      return;
    }
    try {
      const result = await withTimeout(fn.current());
      if (id !== run.current) return;
      hasData.current = true;
      setData(result);
      setUpdatedAt(new Date());
      setOffline(false);
      setState('ready');
    } catch {
      if (id !== run.current) return;
      if (typeof navigator !== 'undefined' && !navigator.onLine) {
        setOffline(true);
        setState('ready');
      } else if (!quiet || !hasData.current) {
        setFailedAt(new Date());
        setState('failed');
      }
    }
  }, []);

  useEffect(() => {
    hasData.current = false;
    setData(null);
    setUpdatedAt(null);
    load(false);
  }, [key, load]);

  useEffect(() => {
    const tick = setInterval(() => {
      if (document.visibilityState === 'visible') load(true);
    }, POLL_MS);
    const back = () => load(true);
    const gone = () => setOffline(true);
    const seen = () => {
      if (document.visibilityState === 'visible') load(true);
    };
    window.addEventListener('online', back);
    window.addEventListener('offline', gone);
    document.addEventListener('visibilitychange', seen);
    return () => {
      clearInterval(tick);
      window.removeEventListener('online', back);
      window.removeEventListener('offline', gone);
      document.removeEventListener('visibilitychange', seen);
    };
  }, [load]);

  return { state, data, updatedAt, offline, failedAt, retry: () => load(false) };
}
