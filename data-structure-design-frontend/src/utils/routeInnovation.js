const PROFILE_LABELS = {
  STANDARD: '普通',
  ELDERLY: '老人',
  FAMILY: '亲子',
  ACCESSIBLE: '无障碍',
}

export const parseNodeIds = (value = '') => String(value)
  .split(/[,\s，、]+/)
  .map((item) => Number(item))
  .filter((item) => Number.isFinite(item))

export const buildRouteInnovationPayload = ({
  travelerProfile = 'STANDARD',
  avoidNodeInput = '',
  congestionFromNodeId = null,
  congestionToNodeId = null,
  congestionMultiplier = null,
} = {}) => {
  const payload = {
    travelerProfile,
    avoidNodeIds: parseNodeIds(avoidNodeInput),
  }
  const from = Number(congestionFromNodeId)
  const to = Number(congestionToNodeId)
  const multiplier = Number(congestionMultiplier)
  if (Number.isFinite(from) && Number.isFinite(to) && Number.isFinite(multiplier) && multiplier > 0) {
    payload.congestionOverrides = [{ fromNodeId: from, toNodeId: to, congestionMultiplier: multiplier }]
  }
  return payload
}

export const formatInnovationSummary = (summary = {}) => {
  if (!summary || !Object.keys(summary).length) return []
  const profile = PROFILE_LABELS[summary.travelerProfile] || PROFILE_LABELS.STANDARD
  const saved = Number(summary.savedCost) || 0
  return [
    `人群模式：${profile}`,
    `少走回头路：${summary.optimizedVisitOrder ? '已启用' : '未启用'}`,
    `预计节省：${Math.round(saved)} m`,
    ...(Array.isArray(summary.explanations) ? summary.explanations : []),
  ]
}
