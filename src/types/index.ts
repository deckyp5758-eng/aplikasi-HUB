export * from './armada';
export * from './pengiriman';
export * from './dashboard';
export * from './ai';

export interface ApiResponse<T = any> {
  success: boolean;
  endpoint?: string;
  message?: string;
  data?: T;
  armada?: any;
  drivers?: any;
  logs?: any;
  banArmada?: any;
  newId?: number;
}
