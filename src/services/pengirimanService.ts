import { getPengirimanApi, addPengirimanApi, updatePengirimanApi, deletePengirimanApi } from '../api/pengiriman';
import { PengirimanItem, CreatePengirimanPayload, UpdatePengirimanPayload } from '../types';

export class PengirimanService {
  static async fetchPengiriman(baseUrl?: string): Promise<PengirimanItem[]> {
    const response = await getPengirimanApi(baseUrl);
    return response.data || [];
  }

  static async createPengiriman(payload: CreatePengirimanPayload, baseUrl?: string) {
    return await addPengirimanApi(payload, baseUrl);
  }

  static async updatePengiriman(payload: UpdatePengirimanPayload, baseUrl?: string) {
    return await updatePengirimanApi(payload, baseUrl);
  }

  static async deletePengiriman(id: number, baseUrl?: string) {
    return await deletePengirimanApi(id, baseUrl);
  }
}
