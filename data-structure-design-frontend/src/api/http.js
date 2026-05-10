import axios from 'axios'

const resolvedBase = import.meta.env.VITE_API_BASE_URL || '/api'
const baseURL = resolvedBase.startsWith('http') && !resolvedBase.endsWith('/api')
  ? resolvedBase + '/api'
  : resolvedBase

const http = axios.create({
  baseURL,
  timeout: 20000,
})

/**
 * 请求拦截器：统一附带登录令牌，便于后端后续扩展鉴权。
 */
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('travel-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const userName = localStorage.getItem('travel-user-name')
  if (userName) {
    config.headers['X-Travel-User'] = encodeURIComponent(userName)
  }
  config.headers['ngrok-skip-browser-warning'] = '1'
  return config
})

export default http
