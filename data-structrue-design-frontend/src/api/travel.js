import http from './http'

// 用户登录接口：用于登录页提交账号密码。
export const login = (payload) => http.post('/auth/login', payload)
export const register = (payload) => http.post('/auth/register', payload)

// 目的地推荐与检索相关接口。
export const getTopDestinations = (k = 10) => http.get('/destinations/top', { params: { k } })
export const searchDestinations = (keyword) => http.get('/destinations', { params: { keyword } })

// 美食推荐接口。
export const getTopFoods = (k = 10) => http.get('/foods/top', { params: { k } })

// 日记管理与全文检索接口。
export const listDiaries = () => http.get('/diaries')
export const createDiary = (payload) => http.post('/diaries', payload)
export const searchDiaryFullText = (keyword) => http.get('/diaries/search', { params: { keyword } })

// 行程协作接口。
export const listItineraries = () => http.get('/itineraries')
export const createItinerary = (payload) => http.post('/itineraries', payload)

// 路线规划接口。
export const getOsmRoute = (params) => http.get('/route', { params })
export const listDestinations = () => http.get('/destinations')
export const searchRouteDestinations = (keyword, limit = 10) =>
  http.get('/destinations/route-search', { params: { keyword, limit } })

// 场所查询接口。
export const listFacilities = (type) => http.get('/facilities', { params: { type } })
export const searchNearbyFacilities = (params) => http.get('/facilities/nearby', { params })
