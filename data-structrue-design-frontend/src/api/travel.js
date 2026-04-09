import http from './http'

export const getTopDestinations = (k = 10) => http.get('/destinations/top', { params: { k } })
export const searchDestinations = (keyword) => http.get('/destinations', { params: { keyword } })
export const getTopFoods = (k = 10) => http.get('/foods/top', { params: { k } })
export const listDiaries = () => http.get('/diaries')
export const createDiary = (payload) => http.post('/diaries', payload)
export const searchDiaryFullText = (keyword) => http.get('/diaries/search', { params: { keyword } })
export const listItineraries = () => http.get('/itineraries')
export const createItinerary = (payload) => http.post('/itineraries', payload)
export const planRoute = (payload) => http.post('/routes/plan', payload)
