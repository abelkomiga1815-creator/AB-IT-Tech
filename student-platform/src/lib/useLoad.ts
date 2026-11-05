"use client";

import { useCallback, useEffect, useState } from "react";
import { messageOf } from "./api";

export interface LoadState<T> {
  data: T | null;
  error: string | null;
  loading: boolean;
  reload: () => void;
}

/** Loads data on mount (and when `reload` is called); ignores results that arrive after unmount. */
export function useLoad<T>(loader: () => Promise<T>): LoadState<T> {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    loader()
      .then((result) => {
        if (!cancelled) {
          setData(result);
          setLoading(false);
        }
      })
      .catch((failure: unknown) => {
        if (!cancelled) {
          setError(messageOf(failure));
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
    // `loader` is created inline by callers; reloading is driven by `version`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [version]);

  const reload = useCallback(() => setVersion((current) => current + 1), []);
  return { data, error, loading, reload };
}
