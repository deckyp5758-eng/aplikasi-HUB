import { getDashboardApi } from '../api/dashboard';
import { DashboardSummary } from '../types';

export class DashboardService {
  static async fetchDashboard(baseUrl?: string): Promise<DashboardSummary> {
    const response = await getDashboardApi(baseUrl);
    if (!response.success || !response.data) {
      throw new Error(response.message || 'Gagal memuat data dashboard.');
    }
    return response.data;
  }
}
