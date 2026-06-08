const internalFacilityTypes = new Set(['教学楼', '宿舍', '宿舍楼', '办公楼', '图书馆', '核心景点'])
const internalFacilityNameKeywords = ['教学楼', '宿舍', '办公楼', '图书馆']

const normalize = (value = '') => String(value).trim().toLowerCase()

export const isPublicFacilityPlace = (name = '', type = '') => {
  const normalizedName = normalize(name)
  const normalizedType = normalize(type)
  if (normalizedType && internalFacilityTypes.has(normalizedType)) return false
  return !internalFacilityNameKeywords.some((keyword) => normalizedName.includes(keyword))
}
