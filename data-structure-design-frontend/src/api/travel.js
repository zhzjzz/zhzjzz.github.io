import http from './http'

// 用户登录接口：用于登录页提交账号密码。
export const login = (payload) => http.post('/auth/login', payload)
export const register = (payload) => http.post('/auth/register', payload)

// 目的地推荐与检索相关接口。
export const getTopDestinations = (k = 10, mode = 'composite') => http.get('/destinations/top', { params: { k, mode } })
export const searchDestinations = (keyword) => http.get('/destinations', { params: { keyword } })

// 美食推荐接口。
export const getTopFoods = (k = 10) => http.get('/foods/top', { params: { k } })
export const searchFoods = (params = {}) => http.get('/foods', { params })
export const searchAmapFoods = (params = {}) => http.get('/foods/amap', { params, timeout: 30000 })
export const listFoodCuisines = () => http.get('/foods/cuisines')

// 日记管理与全文检索接口。
export const listDiaries = (limit = 20) => http.get('/diaries', { params: { limit } })
export const getDiary = (id) => http.get(`/diaries/${id}`)
export const createDiary = (payload) => http.post('/diaries', payload)
export const deleteDiary = (id) => http.delete(`/diaries/${id}`)
export const searchDiaryFullText = (keyword, limit = 20) => http.get('/diaries/search', { params: { keyword, limit } })
export const listHotDiaries = (limit = 6) => http.get('/diaries/hot', { params: { limit } })
export const getSharedDiary = (token) => http.get(`/diaries/share/${token}`)
export const interactDiary = (id, type) => http.post(`/diaries/${id}/interactions/${type}`)
export const listDiaryComments = (id) => http.get(`/diaries/${id}/comments`)
export const createDiaryComment = (id, payload) => http.post(`/diaries/${id}/comments`, payload)

// 行程协作接口。
export const listItineraries = () => http.get('/itineraries')
export const createItinerary = (payload) => http.post('/itineraries', payload)
export const getItinerary = (id) => http.get(`/itineraries/${id}`)
export const updateItinerary = (id, payload) => http.put(`/itineraries/${id}`, payload)

// 路线规划接口。
export const getOsmRoute = (params) => http.get('/route', { params })
export const listDestinations = () => http.get('/destinations')
export const searchRouteDestinations = (keyword, limit = 10) =>
  http.get('/destinations/route-search', { params: { keyword, limit } })

// 导航接口（SQLite 景区内部 + 跨景区三段式）。
export const searchNavSpots = (params) => http.get('/nav/spots', { params })
export const getNavSpotById = (spotId) => http.get(`/nav/spots/${spotId}`)
export const listNavBuildingsBySpot = (spotName) => http.get('/nav/buildings/by-spot', { params: { spotName } })
export const listNavPoisBySpot = (spotName) => http.get('/nav/pois/by-spot', { params: { spotName } })
export const planSingleRoute = (payload) => http.post('/nav/route/plan', payload)
export const planCrossSpotRoute = (payload) => http.post('/nav/route/cross-spot', payload)
export const planMultiSpotRoute = (payload) => http.post('/nav/route/multi-spot', payload)
export const getNavNodes = (spotName) => http.get('/nav/route/nodes', { params: { spotName } })
export const getNavEdges = (spotName) => http.get('/nav/route/edges', { params: { spotName } })

// 设施查询接口。
// 查询场所列表；不传 type 时返回全部，用于前端动态提取可选类别。
export const listFacilities = (type) => http.get('/facilities', { params: type ? { type } : {} })
export const listFacilityTypes = (keyword = '', limit = 50, sceneType = '') =>
  http.get('/facilities/types', { params: { keyword, limit, sceneType } })
export const searchNearbyFacilities = (params) => http.get('/facilities/nearby', { params })

// 旅游服务 Agent：后端代理调用 SiliconFlow，前端不直接保存或暴露 API Key。
// R1 类模型有时响应较慢，单独给 Agent 对话更长超时时间。
export const chatWithTravelAgent = (payload) => http.post('/agent/chat', payload, { timeout: 90000 })
