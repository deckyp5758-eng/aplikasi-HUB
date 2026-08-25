import { getArmadaApi, getDriversApi, getBanApi, getLogsApi, submitLogApi, submitServiceLogApi, loginDriverApi, updateBanApi } from '../api/armada';
import { ArmadaItem, DriverItem, BanItem, LogItem } from '../types';

export class ArmadaService {
  static async fetchArmada(baseUrl?: string): Promise<ArmadaItem[]> {
    const response = await getArmadaApi(baseUrl);
    return response.armada || response.data || [];
  }

  static async fetchDrivers(baseUrl?: string): Promise<DriverItem[]> {
    const response = await getDriversApi(baseUrl);
    return response.drivers || response.data || [];
  }

  static async fetchBan(baseUrl?: string): Promise<BanItem[]> {
    const response = await getBanApi(baseUrl);
    return response.banArmada || response.data || [];
  }

  static async fetchLogs(baseUrl?: string): Promise<LogItem[]> {
    const response = await getLogsApi(baseUrl);
    return response.logs || response.data || [];
  }

  static async postDailyLog(logData: any, baseUrl?: string) {
    return await submitLogApi(logData, baseUrl);
  }

  static async postServiceLog(armadaId: string, kmServis: number, catatan?: string, baseUrl?: string) {
    return await submitServiceLogApi({ armadaId, kmServis, catatan }, baseUrl);
  }

  static async authenticateDriver(driverName: string, pin: string, baseUrl?: string) {
    return await loginDriverApi(driverName, pin, baseUrl);
  }

  static async updateBan(banData: { armadaId: string; posisi: string; barcode: string; tahun?: string; kondisi?: string; tekanan?: string; keterangan?: string }, baseUrl?: string) {
    return await updateBanApi(banData, baseUrl);
  }
}
