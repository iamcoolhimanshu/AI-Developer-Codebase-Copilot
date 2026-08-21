import axios, { type AxiosError, type AxiosInstance } from 'axios'
import type { ApiResponse } from './types'

const TOKEN_KEY = 'cc_access_token'
const REFRESH_KEY = 'cc_refresh_token'

export function setTokens(access: string, refresh: string) {
  localStorage.setItem(TOKEN_KEY, access)
  localStorage.setItem(REFRESH_KEY, refresh)
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY)
}

const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 120000,
})

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original: any = error.config
    if (error.response?.status === 401 && original && !original._retried) {
      original._retried = true
      const refresh = localStorage.getItem(REFRESH_KEY)
      if (refresh) {
        try {
          const res = await axios.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
            '/api/auth/refresh',
            { refreshToken: refresh },
          )
          const data = res.data.data
          setTokens(data.accessToken, data.refreshToken)
          original.headers.Authorization = `Bearer ${data.accessToken}`
          return api(original)
        } catch {
          clearTokens()
        }
      }
    }
    if (error.response?.status === 401 && !original?._retried) {
      clearTokens()
    }
    return Promise.reject(error)
  },
)

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as { message?: string } | undefined
    if (body?.message && body.message !== 'OK') return body.message
    return error.message
  }
  return String(error)
}

export async function unwrap<T = any>(p: Promise<{ data: any }>): Promise<T> {
  const res = await p
  return (res.data?.data ?? res.data) as T
}

export default api