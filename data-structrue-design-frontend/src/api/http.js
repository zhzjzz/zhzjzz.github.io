import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 8000,
})

/**
 * 请求拦截器：统一附带登录令牌，便于后端后续扩展鉴权。
 */
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('travel-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default http
