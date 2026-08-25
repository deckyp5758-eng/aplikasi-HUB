export interface AiKnowledgeItem {
  id?: number;
  kategori: string;
  pertanyaan: string;
  jawaban: string;
}

export interface AiChatRequest {
  chatMessage: string;
  apiKey?: string;
  base64Data?: string;
  mimeType?: string;
}

export interface AiChatResponse {
  success: boolean;
  message: string;
}
