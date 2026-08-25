import { fetchApi } from './api';
import { ApiResponse, PengirimanItem, CreatePengirimanPayload, UpdatePengirimanPayload } from '../types';
import { DEFAULT_APPS_SCRIPT_URL } from '../utils/constants';

export async function getPengirimanApi(baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse<PengirimanItem[]>> {
  const url = `${baseUrl}?endpoint=pengiriman`;
  return await fetchApi<ApiResponse<PengirimanItem[]>>(url, { method: 'GET' });
}

export async function addPengirimanApi(payload: CreatePengirimanPayload, baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse> {
  const url = `${baseUrl}?endpoint=addpengiriman`;
  return await fetchApi<ApiResponse>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function updatePengirimanApi(payload: UpdatePengirimanPayload, baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse> {
  const url = `${baseUrl}?endpoint=updatepengiriman`;
  return await fetchApi<ApiResponse>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function deletePengirimanApi(id: number, baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse> {
  const url = `${baseUrl}?endpoint=deletepengiriman`;
  return await fetchApi<ApiResponse>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id }),
  });
}
