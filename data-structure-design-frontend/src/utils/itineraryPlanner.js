export const defaultPlannerSelection = (spots = []) => {
  const hasPreferred = spots.some((spot) => ['must', 'want'].includes(spot?.consensus))
  return spots.map((spot, orderIndex) => ({
    ...spot,
    orderIndex,
    selected: hasPreferred ? ['must', 'want'].includes(spot?.consensus) : true,
    transportMode: spot?.transportMode || 'walk',
  }))
}

export const buildPlannerPayload = ({
  departureTime = '',
  strategy = 'SHORTEST_TIME',
  optimizeVisitOrder = true,
  spots = [],
} = {}) => ({
  departureTime: departureTime || null,
  strategy,
  optimizeVisitOrder,
  spots: spots.map((spot) => ({
    spotId: spot.spotId,
    destinationId: spot.destinationId,
    spotName: spot.spotName,
    latitude: spot.latitude,
    longitude: spot.longitude,
    transportMode: spot.transportMode || 'walk',
    selected: Boolean(spot.selected),
  })),
})

export const selectedPlannerCount = (spots = []) => (
  spots.filter((spot) => spot.selected).length
)

export const formatKm = (meters = 0) => `${((Number(meters) || 0) / 1000).toFixed(2)} km`

export const formatDuration = (seconds = 0) => {
  const totalMinutes = Math.round((Number(seconds) || 0) / 60)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (hours && minutes) return `${hours} 小时 ${minutes} 分钟`
  if (hours) return `${hours} 小时`
  return `${minutes} 分钟`
}

export const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
