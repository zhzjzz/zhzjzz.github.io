export const DEFAULT_GUIDE_IMPORT_DEMO_TEXT = [
  '观复博物馆',
  '北京理工大学',
  '八达岭野生动物园',
].join('\n')

export const buildTextImportPayload = (text, owner = '') => ({
  sourceType: 'TEXT',
  text: String(text || '').trim(),
  diaryId: null,
  owner: String(owner || '').trim(),
})

export const buildDiaryImportPayload = (diaryId, owner = '') => ({
  sourceType: 'DIARY',
  text: null,
  diaryId,
  owner: String(owner || '').trim(),
})

export const importCanCreate = (preview) =>
  Array.isArray(preview?.spots) && preview.spots.some((spot) => spot?.matchedDestinationId)

export const importSpotLabel = (spot) => {
  const order = Number.isFinite(Number(spot?.orderIndex)) ? Number(spot.orderIndex) + 1 : 1
  const name = spot?.matchedName || spot?.rawName || 'Imported spot'
  const stay = Number.isFinite(Number(spot?.stayMinutes)) ? Number(spot.stayMinutes) : 120
  return `${order}. ${name} · ${stay} min`
}
