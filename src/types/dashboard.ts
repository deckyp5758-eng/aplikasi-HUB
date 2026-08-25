export interface DashboardSummary {
  pengiriman: {
    totalShipments: number;
    selesai: number;
    dalamPerjalanan: number;
    belumBerangkat: number;
    totalKoli: number;
    totalVolumeCbm: number;
  };
  armada: {
    totalTrucks: number;
    segeraServis: number;
    aman: number;
    serviceUnder1000: number;
  };
  drivers: {
    totalDrivers: number;
  };
}
