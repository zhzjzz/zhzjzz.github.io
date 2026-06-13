import test from 'node:test'
import assert from 'node:assert/strict'
import { buildRouteInnovationPayload, formatInnovationSummary } from './routeInnovation.js'

test('buildRouteInnovationPayload includes profile, obstacle and congestion fields', () => {
  const payload = buildRouteInnovationPayload({
    travelerProfile: 'ELDERLY',
    avoidNodeInput: '12, 34',
    congestionFromNodeId: 1,
    congestionToNodeId: 2,
    congestionMultiplier: 3,
  })

  assert.equal(payload.travelerProfile, 'ELDERLY')
  assert.deepEqual(payload.avoidNodeIds, [12, 34])
  assert.deepEqual(payload.congestionOverrides, [
    { fromNodeId: 1, toNodeId: 2, congestionMultiplier: 3 },
  ])
})

test('formatInnovationSummary renders readable savings and explanations', () => {
  const lines = formatInnovationSummary({
    travelerProfile: 'FAMILY',
    optimizedVisitOrder: true,
    originalCost: 300,
    optimizedCost: 210,
    savedCost: 90,
    explanations: ['少走回头路', '亲子友好'],
  })

  assert.deepEqual(lines, [
    '人群模式：亲子',
    '少走回头路：已启用',
    '预计节省：90 m',
    '少走回头路',
    '亲子友好',
  ])
})
