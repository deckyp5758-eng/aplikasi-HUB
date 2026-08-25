export interface ArmadaItem {
  armadaId: string;
  noPolisi: string;
  kmSaatIni: number;
  kmServiceTerakhir: number;
  intervalService: number;
  kmServiceBerikutnya: number;
  sisaKm: number;
  status: string;
  flag?: string;
  fotoKm?: string;
  catattan?: string;
  pajakTahunan?: string;
  kir?: string;
  pajak5Tahunan?: string;
  fotoTruck?: string;
}

export interface DriverItem {
  id: string;
  name: string;
  pin?: string;
}

export interface BanItem {
  id?: number;
  armadaId: string;
  noPolisi: string;
  posisi: string;
  noSeri: string;
  ukuran: string;
  merk: string;
  kondisi: string;
  tekanan: string;
  keterangan: string;
  barcode?: string;
  tahun?: string;
}

export interface LogItem {
  tanggal: string;
  armadaId: string;
  kmTerdeteksi: number;
  linkFoto?: string;
  catatan?: string;
  namaDriver: string;
}
