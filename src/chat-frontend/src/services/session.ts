import axios from 'axios';
import type { AvatarId, User } from '../types/chat';

export const backendUrl = (import.meta.env.VITE_BACKEND_URL || '').replace(/\/$/, '');
export const http = axios.create({ baseURL: backendUrl, timeout: 8000 });
type Session = { user: User; token?: string; generation: string; expiresAt: string };
const key = 'chatty-session';
let credential: Session | undefined;
try { credential = JSON.parse(sessionStorage.getItem(key) || 'null') || undefined; } catch { sessionStorage.removeItem(key); }
export const sessionCredential = () => credential;
export const clearSession = () => { credential = undefined; sessionStorage.removeItem(key); };
http.interceptors.request.use(config => {
  if (credential?.token) config.headers.Authorization = `Bearer ${credential.token}`;
  return config;
});
export async function acquireSession(username: string, avatarId: AvatarId) {
  if (credential?.token && credential.user.username === username) {
    const response = await http.get<Session>('/api/v1/session');
    if (response.data.generation !== credential.generation) { clearSession(); throw new Error('La sesión del servidor se reinició. Vuelve a entrar.'); }
    credential = { ...response.data, token: credential.token };
  } else {
    const response = await http.post<Session>('/api/v1/auth', null, { params: { username, avatarId } });
    credential = response.data;
  }
  sessionStorage.setItem(key, JSON.stringify(credential));
  return credential;
}
export async function validateSession() {
  const response = await http.get<Session>('/api/v1/session');
  if (!credential || response.data.generation !== credential.generation) {
    clearSession(); throw new Error('La sesión del servidor se reinició. Vuelve a entrar.');
  }
  return response.data;
}
export async function revokeSession() {
  try { if (credential?.token) await http.post('/api/v1/logout'); } finally { clearSession(); }
}
export const sessionError = (error: unknown) => axios.isAxiosError(error)
  ? error.response?.data?.message || 'No se pudo abrir la sesión. Intenta nuevamente.'
  : error instanceof Error ? error.message : 'No se pudo abrir la sesión.';
