import { useState, useEffect, useCallback } from 'react';
import { useAutoRefresh } from './useAutoRefresh';
import { usePullToRefresh } from './usePullToRefresh';
import { showToast } from '../utils/toast';

interface UseFetchDataOptions<T> {
  fetcher: () => Promise<T>;
  autoRefreshInterval?: number;
  enableAutoRefresh?: boolean;
}

export function useFetchData<T>({
  fetcher,
  autoRefreshInterval = 60000,
  enableAutoRefresh = true,
}: UseFetchDataOptions<T>) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async (isSilent = false) => {
    if (!isSilent) {
      setLoading(true);
    }
    setError(null);

    try {
      const result = await fetcher();
      setData(result);
    } catch (err: any) {
      const msg = err?.message || 'Terjadi kesalahan saat mengambil data.';
      setError(msg);
      if (isSilent) {
        showToast(msg, 'error');
      }
    } finally {
      if (!isSilent) {
        setLoading(false);
      }
    }
  }, [fetcher]);

  // Initial load
  useEffect(() => {
    loadData(false);
  }, [loadData]);

  // Auto Refresh every 60 seconds (silent update)
  useAutoRefresh(() => {
    loadData(true);
  }, autoRefreshInterval, enableAutoRefresh);

  // Pull to Refresh
  const { refreshing, handleRefresh } = usePullToRefresh(async () => {
    await loadData(false);
  });

  // Retry function
  const retry = useCallback(() => {
    loadData(false);
  }, [loadData]);

  return {
    data,
    loading,
    error,
    refreshing,
    retry,
    handleRefresh,
  };
}
