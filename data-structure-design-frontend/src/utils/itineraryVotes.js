export const VOTE_TYPES = ['must', 'want', 'avoid', 'backup']

const DEFAULT_COORDS = [
  { x: 18, y: 28 },
  { x: 39, y: 58 },
  { x: 63, y: 34 },
  { x: 78, y: 68 },
  { x: 50, y: 18 },
  { x: 25, y: 74 },
]

export const voteTypeLabel = (type) => ({
  must: '必去',
  want: '想去',
  avoid: '不想去',
  backup: '备选',
}[type] || '未标记')

export const voteTypeTone = (type) => ({
  must: 'danger',
  want: 'success',
  avoid: 'warning',
  backup: 'info',
}[type] || 'info')

export const computeConsensus = (votes = []) => {
  if (!votes.length) return 'backup'
  const counts = votes.reduce((acc, vote) => {
    const type = vote.voteType || vote.type
    acc[type] = (acc[type] || 0) + 1
    return acc
  }, {})
  const total = votes.length
  if ((counts.avoid || 0) >= 2 || (counts.avoid || 0) / total >= 0.5) return 'conflict'
  if ((counts.must || 0) / total > 0.5) return 'must'
  if (((counts.must || 0) + (counts.want || 0)) / total > 0.5) return 'want'
  return 'backup'
}

export const buildSpotNodes = (votes = []) => {
  const grouped = new Map()
  votes.forEach((vote) => {
    const spotId = vote.spotId
    if (!grouped.has(spotId)) {
      grouped.set(spotId, {
        spotId,
        spotName: vote.spotName || `景点 ${spotId}`,
        votes: [],
      })
    }
    grouped.get(spotId).votes.push(vote)
  })

  return Array.from(grouped.values()).map((node, index) => {
    const coord = DEFAULT_COORDS[index % DEFAULT_COORDS.length]
    return {
      ...node,
      x: coord.x,
      y: coord.y,
      consensus: computeConsensus(node.votes),
    }
  })
}

export const buildRealSpotNodes = (mapSpots = []) => {
  return mapSpots
    .filter((spot) => {
      if (spot.latitude == null || spot.longitude == null || spot.latitude === '' || spot.longitude === '') {
        return false
      }
      return Number.isFinite(Number(spot.latitude)) && Number.isFinite(Number(spot.longitude))
    })
    .map((spot) => {
      const votes = Array.isArray(spot.votes) ? spot.votes : []
      return {
        candidateId: spot.candidateId,
        destinationId: spot.destinationId,
        spotId: spot.spotId || spot.destinationId,
        spotName: spot.spotName || `景点 ${spot.spotId || spot.destinationId}`,
        latitude: Number(spot.latitude),
        longitude: Number(spot.longitude),
        votes,
        consensus: computeConsensus(votes),
      }
    })
}

export const summarizeConsensus = (nodes = []) => ({
  total: nodes.length,
  agreed: nodes.filter((node) => ['must', 'want'].includes(node.consensus)).length,
  conflicts: nodes.filter((node) => node.consensus === 'conflict').length,
  must: nodes.filter((node) => node.consensus === 'must').length,
})

export const makePingText = (vote) => {
  if (!vote) return ''
  return `${vote.username || '队友'}标记“${vote.spotName || '景点'}”为${voteTypeLabel(vote.voteType || vote.type)}`
}
