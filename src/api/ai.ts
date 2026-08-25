import { fetchApi } from './api';
import { ApiResponse, AiKnowledgeItem, AiChatRequest, AiChatResponse } from '../types';
import { DEFAULT_APPS_SCRIPT_URL } from '../utils/constants';

export async function getAiKnowledgeApi(baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<ApiResponse<AiKnowledgeItem[]>> {
  const url = `${baseUrl}?endpoint=aidata`;
  return await fetchApi<ApiResponse<AiKnowledgeItem[]>>(url, { method: 'GET' });
}

export async function askAiAssistantApi(payload: AiChatRequest, baseUrl: string = DEFAULT_APPS_SCRIPT_URL): Promise<AiChatResponse> {
  const url = `${baseUrl}?endpoint=asisten_ai`;
  return await fetchApi<AiChatResponse>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}
