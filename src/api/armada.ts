import { fetchApi } from './api';
import { ApiResponse, ArmadaItem, DriverItem, BanItem, LogItem } from '../types';
import { DEFAULT_APPS_SCRIPT_URL } from '../utils/constants';

export async function getArmadaApi(baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse<ArmadaItem[]>> {
  const url = `${baseUrl}?endpoint=armada`;
  return await fetchApi<ApiResponse<ArmadaItem[]>>(url, { method: 'GET' });
}

export async function getDriversApi(baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse<DriverItem[]>> {
  const url = `${baseUrl}?endpoint=drivers`;
  return await fetchApi<ApiResponse<DriverItem[]>>(url, { method: 'GET' });
}

export async function getBanApi(baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse<BanItem[]>> {
  const url = `${baseUrl}?endpoint=ban`;
  return await fetchApi<ApiResponse<BanItem[]>>(url, { method: 'GET' });
}

export async function getLogsApi(baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse<LogItem[]>> {
  const url = `${baseUrl}?endpoint=logs`;
  return await fetchApi<ApiResponse<LogItem[]>>(url, { method: 'GET' });
}

export async function submitLogApi(logData: any, baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse> {
  const url = `${baseUrl}?endpoint=submitlog`;
  return await fetchApi<ApiResponse>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ logData }),
  });
}

export async function submitServiceLogApi(payload: { armadaId: string; kmServis: number; catatan?: string }, baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse> {
  const url = `${baseUrl}?endpoint=submitservicelog`;
  return await fetchApi<ApiResponse>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function loginDriverApi(driverName: string, pin: string, baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse> {
  const url = `${baseUrl}?endpoint=login`;
  return await fetchApi<ApiResponse>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ driverName, pin }),
  });
}

export async function updateBanApi(banData: { armadaId: string; posisi: string; barcode: string; tahun?: string; kondisi?: string; tekanan?: string; keterangan?: string }, baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse> {
  const url = `${baseUrl}?endpoint=updateban`;
  return await fetchApi<ApiResponse>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ banData }),
  });
}
