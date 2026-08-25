import { getAiKnowledgeApi, askAiAssistantApi } from '../api/ai';
import { AiKnowledgeItem, AiChatRequest, AiChatResponse } from '../types';

export class AiService {
  static async fetchKnowledge(baseUrl?: string): Promise<AiKnowledgeItem[]> {
    const response = await getAiKnowledgeApi(baseUrl);
    return response.data || [];
  }

  static async sendChatMessage(payload: AiChatRequest, baseUrl?: string): Promise<AiChatResponse> {
    return await askAiAssistantApi(payload, baseUrl);
  }
}
