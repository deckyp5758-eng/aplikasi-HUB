import { fetchApi } from './api';
import { ApiResponse, DashboardSummary } from '../types';
import { DEFAULT_APPS_SCRIPT_URL } from '../utils/constants';

export async function getDashboardApi(baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse<DashboardSummary>> {
  const url = `${baseUrl}?endpoint=dashboard`;
  return await fetchApi<ApiResponse<DashboardSummary>>(url, { method: 'GET' });
}
