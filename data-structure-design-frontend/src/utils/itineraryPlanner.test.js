import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildPlannerPayload,
  defaultPlannerSelection,
  formatDuration,
  formatKm,
} from './itineraryPlanner.js'

test('defaultPlannerSelection selects must and want nodes', () => {
  const selected = defaultPlannerSelection([
    { spotId: 1, spotName: 'A', consensus: 'must' },
    { spotId: 2, spotName: 'B', consensus: 'want' },
    { spotId: 3, spotName: 'C', consensus: 'backup' },
  ])

  assert.deepEqual(selected.map((spot) => [spot.spotId, spot.selected]), [
    [1, true],
    [2, true],
    [3, false],
  ])
})

test('defaultPlannerSelection selects all nodes when no must or want exists', () => {
  const selected = defaultPlannerSelection([
    { spotId: 1, spotName: 'A', consensus: 'backup' },
    { spotId: 2, spotName: 'B', consensus: 'conflict' },
  ])

  assert.deepEqual(selected.map((spot) => spot.selected), [true, true])
})

test('buildPlannerPayload keeps selected spots in order', () => {
  const payload = buildPlannerPayload({
    departureTime: '2026-05-15T09:00',
    strategy: 'SHORTEST_TIME',
    optimizeVisitOrder: false,
    spots: [
      { spotId: 1, destinationId: 1, spotName: 'A', latitude: 1, longitude: 2, selected: false },
      { spotId: 2, destinationId: 2, spotName: 'B', latitude: 3, longitude: 4, selected: true },
    ],
  })

  assert.equal(payload.departureTime, '2026-05-15T09:00')
  assert.equal(payload.optimizeVisitOrder, false)
  assert.equal(payload.spots.length, 2)
  assert.equal(payload.spots[0].selected, false)
  assert.equal(payload.spots[1].spotName, 'B')
  assert.equal(payload.spots[1].transportMode, 'walk')
})

test('format helpers render compact distance and duration', () => {
  assert.equal(formatKm(1530), '1.53 km')
  assert.equal(formatDuration(3660), '1 小时 1 分钟')
})
