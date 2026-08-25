import { DEFAULT_APPS_SCRIPT_URL } from '../utils/constants';
import { showToast } from '../utils/toast';

interface FetchOptions extends RequestInit {
  timeoutMs?: number;
  maxRetries?: number;
}

export async function fetchApi<T = any>(
  endpointOrUrl: string,
  options: FetchOptions = {}
): Promise<T> {
  const {
    timeoutMs = 15000,
    maxRetries = 2,
    ...init
  } = options;

  let url = endpointOrUrl;
  if (!url.startsWith('http://') && !url.startsWith('https://')) {
    const cleanEndpoint = endpointOrUrl.replace(/^\/+/, '');
    url = `${DEFAULT_APPS_SCRIPT_URL}?endpoint=${cleanEndpoint}`;
  }

  let attempt = 0;
  let lastError: Error | null = null;

  while (attempt <= maxRetries) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const response = await fetch(url, {
        ...init,
        signal: controller.signal,
      });

      clearTimeout(timer);

      if (!response.ok) {
        throw new Error(`HTTP Error ${response.status}: ${response.statusText}`);
      }

      const json = await response.json();
      return json as T;
    } catch (err: any) {
      clearTimeout(timer);
      lastError = err?.name === 'AbortError' ? new Error('Koneksi timeout. Silakan coba lagi.') : (err as Error);
      
      attempt++;
      if (attempt <= maxRetries) {
        // Wait 1 second before retrying
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
    }
  }

  const errorMessage = lastError?.message || 'Gagal terhubung ke server Google Apps Script.';
  showToast(errorMessage, 'error');
  throw new Error(errorMessage);
}
