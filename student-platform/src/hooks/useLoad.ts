"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, messageOf } from "@/lib/api";

interface LoadState<T> {
  data: T | null;
  error: string | null;
  code: string | null;
  loading: boolean;
}

/**
 * Loads data when the component appears and lets the caller reload it after changes.
 * Components that load per-id data should be rendered with a `key` so they reset when the id changes.
 */
export function useLoad<T>(loader: () => Promise<T>) {
  const loaderRef = useRef(loader);
  loaderRef.current = loader;
  const [state, setState] = useState<LoadState<T>>({ data: null, error: null, code: null, loading: true });

  const reload = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, error: null, code: null }));
    try {
      const data = await loaderRef.current();
      setState({ data, error: null, code: null, loading: false });
    } catch (error) {
      setState({
        data: null,
        error: messageOf(error),
        code: error instanceof ApiError ? error.code : null,
        loading: false,
      });
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { ...state, reload };
}
