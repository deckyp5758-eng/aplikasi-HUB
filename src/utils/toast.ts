export type ToastType = 'error' | 'success' | 'info';

export interface ToastEvent {
  message: string;
  type: ToastType;
}

type ToastListener = (event: ToastEvent) => void;

const listeners: Set<ToastListener> = new Set();

export const showToast = (message: string, type: ToastType = 'error') => {
  listeners.forEach(listener => listener({ message, type }));
};

export const subscribeToast = (listener: ToastListener) => {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
};
