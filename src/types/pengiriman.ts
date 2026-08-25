export interface PengirimanItem {
  id: number;
  noSuratJalan: string;
  tanggal: string;
  driver: string;
  armada: string;
  gudangAsal: string;
  tujuan: string;
  jumlahKoli: number;
  volumeCbm: number;
  status: string;
  catatan?: string;
}

export type CreatePengirimanPayload = Omit<PengirimanItem, 'id'>;
export type UpdatePengirimanPayload = PengirimanItem;
