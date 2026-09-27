import axios from 'axios'
import { M } from '../i18n/messages.js'

export const TOKEN_KEY = 'auth_token'
export const SESSION_KEY = 'auth_session'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

export const http = axios.create({
  baseURL,
  timeout: 25000,
  headers: { 'Content-Type': 'application/json' },
})

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) {
    sessionStorage.setItem(TOKEN_KEY, token)
  } else {
    sessionStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(TOKEN_KEY)
  }
}

export function setSession(session) {
  if (session) sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
  else sessionStorage.removeItem(SESSION_KEY)
}

export function getStoredSession() {
  const raw = sessionStorage.getItem(SESSION_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// A single normalised error shape: { code, message, fields, status }
export class ApiError extends Error {
  constructor({ code, message, fields, status }) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.fields = fields || null
    this.status = status
  }
}

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    const normalized = new ApiError({
      code: body?.code || (status ? `HTTP_${status}` : 'NETWORK_ERROR'),
      message: body?.message || M.GENERIC,
      fields: body?.data || null,
      status,
    })
    // Session invalidation / expiry → force a clean re-login (UC-06).
    if (status === 401) {
      setToken(null)
      setSession(null)
      window.dispatchEvent(new CustomEvent('auth:unauthorized', { detail: normalized }))
    }
    return Promise.reject(normalized)
  },
)

// --- helpers: return the ApiResponse envelope, throwing ApiError on failure ---
export async function get(url, params) {
  const { data } = await http.get(url, { params })
  return data
}
export async function post(url, body) {
  const { data } = await http.post(url, body)
  return data
}
export async function put(url, body) {
  const { data } = await http.put(url, body)
  return data
}
export async function del(url) {
  const { data } = await http.delete(url)
  return data
}
export async function upload(url, formData) {
  const { data } = await http.post(url, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  return data
}
