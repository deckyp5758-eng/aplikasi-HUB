import { CONFIG } from "../config";

export interface Pengiriman {
  id: number;
  noSuratJalan: String;
  tanggal: String;
  driver: String;
  armada: String;
  gudangAsal: String;
  tujuan: String;
  jumlahKoli: number;
  volumeCbm: number;
  status: "Belum Berangkat" | "Dalam Perjalanan" | "Selesai";
  catatan: String;
}

export interface Armada {
  armadaId: String;
  noPolisi: String;
  kmSaatIni: number;
  kmServiceTerakhir: number;
  intervalService: number;
  kmServiceBerikutnya: number;
  sisaKm: number;
  status: String;
  flag: String;
  fotoKm: String;
  catattan: String;
  pajakTahunan?: String;
  kir?: String;
  pajak5Tahunan?: String;
  fotoTruck?: String;
}

export interface AiKnowledge {
  id: String;
  kategori: String;
  pertanyaan: String;
  jawaban: String;
}

/**
 * Service API helper for Google Apps Script as REST API and Google Sheets as Database
 */
export class FleetApiService {
  private static apiUrl = CONFIG.API_URL;

  /**
   * Generic fetch request helper with timeout and basic error handling
   */
  private static async request<T>(endpoint: String, options: RequestInit = {}): Promise<T> {
    try {
      const response = await fetch(`${this.apiUrl}?${endpoint}`, {
        ...options,
        headers: {
          "Content-Type": "application/json",
          ...options.headers,
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      if (data && data.success === false) {
        throw new Error(data.message || "Request API returned success: false");
      }
      return data as T;
    } catch (error) {
      console.error("API Error encountered:", error);
      throw error;
    }
  }

  // ==========================================
  // 1. PENGIRIMAN DATA OPERATIONS (Spreadsheet PENGIRIMAN)
  // ==========================================

  /**
   * Fetch all shipments from Pengiriman Spreadsheet ID
   */
  public static async getPengirimanList(): Promise<Pengiriman[]> {
    const spreadsheetId = CONFIG.SPREADSHEETS.PENGIRIMAN;
    const urlParams = `action=getPengiriman&spreadsheetId=${spreadsheetId}`;
    const result = await this.request<{ success: boolean; data: Pengiriman[] }>(urlParams);
    return result.data || [];
  }

  /**
   * Create a new shipment row in Pengiriman Spreadsheet
   */
  public static async addPengiriman(pengiriman: Omit<Pengiriman, "id">): Promise<boolean> {
    const spreadsheetId = CONFIG.SPREADSHEETS.PENGIRIMAN;
    const urlParams = `action=addPengiriman&spreadsheetId=${spreadsheetId}`;
    const result = await this.request<{ success: boolean }>(urlParams, {
      method: "POST",
      body: JSON.stringify(pengiriman),
    });
    return result.success;
  }

  /**
   * Update shipment row in Pengiriman Spreadsheet
   */
  public static async updatePengiriman(id: number, pengiriman: Partial<Pengiriman>): Promise<boolean> {
    const spreadsheetId = CONFIG.SPREADSHEETS.PENGIRIMAN;
    const urlParams = `action=updatePengiriman&spreadsheetId=${spreadsheetId}`;
    const result = await this.request<{ success: boolean }>(urlParams, {
      method: "POST",
      body: JSON.stringify({ id, ...pengiriman }),
    });
    return result.success;
  }

  /**
   * Delete shipment row in Pengiriman Spreadsheet
   */
  public static async deletePengiriman(id: number): Promise<boolean> {
    const spreadsheetId = CONFIG.SPREADSHEETS.PENGIRIMAN;
    const urlParams = `action=deletePengiriman&spreadsheetId=${spreadsheetId}`;
    const result = await this.request<{ success: boolean }>(urlParams, {
      method: "POST",
      body: JSON.stringify({ id }),
    });
    return result.success;
  }


  // ==========================================
  // 2. ARMADA DATA OPERATIONS (Spreadsheet ARMADA)
  // ==========================================

  /**
   * Fetch all fleet information from Armada Spreadsheet ID
   */
  public static async getArmadaList(): Promise<Armada[]> {
    const spreadsheetId = CONFIG.SPREADSHEETS.ARMADA;
    const urlParams = `action=getArmada&spreadsheetId=${spreadsheetId}`;
    const result = await this.request<{ success: boolean; armada: Armada[] }>(urlParams);
    return result.armada || [];
  }

  /**
   * Fetch list of active drivers from Armada Spreadsheet
   */
  public static async getDrivers(): Promise<{ id: string; name: string }[]> {
    const spreadsheetId = CONFIG.SPREADSHEETS.ARMADA;
    const urlParams = `action=getDrivers&spreadsheetId=${spreadsheetId}`;
    const result = await this.request<{ success: boolean; drivers: { id: string; name: string }[] }>(urlParams);
    return result.drivers || [];
  }


  // ==========================================
  // 3. AI DATA OPERATIONS (Spreadsheet AI DATA)
  // ==========================================

  /**
   * Fetch Knowledge Base entries for AI Assistant Context from AI DATA Spreadsheet ID
   */
  public static async getAiKnowledgeBase(): Promise<AiKnowledge[]> {
    const spreadsheetId = CONFIG.SPREADSHEETS.AI_DATA;
    const urlParams = `action=getAiKnowledge&spreadsheetId=${spreadsheetId}`;
    try {
      const result = await this.request<{ success: boolean; data: AiKnowledge[] }>(urlParams);
      return result.data || [];
    } catch (e) {
      console.warn("Could not retrieve AI Knowledge database, falling back to local instruction sets.", e);
      return [];
    }
  }

  /**
   * Query prompt directly to Apps Script AI backend or direct Gemini endpoint with loaded context
   */
  public static async askAiAssistant(message: string, contextData?: string): Promise<string> {
    const spreadsheetId = CONFIG.SPREADSHEETS.AI_DATA;
    const urlParams = `action=asisten_ai&spreadsheetId=${spreadsheetId}`;
    
    // Enrich with spreadsheet-loaded AI knowledge base if provided
    let enrichedMessage = message;
    if (contextData) {
      enrichedMessage = `${message}\n\n[KNOWLEDGE BASE CONTEXT:\n${contextData}]`;
    }

    const result = await this.request<{ success: boolean; message: string }>(urlParams, {
      method: "POST",
      body: JSON.stringify({
        chatMessage: enrichedMessage,
      }),
    });

    return result.message || "Maaf, asisten tidak memberikan jawaban.";
  }
}
