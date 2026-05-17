import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildRealSpotNodes,
  buildSpotNodes,
  computeConsensus,
  summarizeConsensus,
  voteTypeLabel,
} from './itineraryVotes.js'

test('computeConsensus returns must when must votes are the majority', () => {
  const result = computeConsensus([
    { username: 'Zhou', voteType: 'must' },
    { username: 'Alex', voteType: 'must' },
    { username: 'Lin', voteType: 'want' },
  ])

  assert.equal(result, 'must')
})

test('computeConsensus returns conflict when avoid votes are significant', () => {
  const result = computeConsensus([
    { username: 'Zhou', voteType: 'avoid' },
    { username: 'Alex', voteType: 'avoid' },
    { username: 'Lin', voteType: 'must' },
  ])

  assert.equal(result, 'conflict')
})

test('buildSpotNodes groups votes by spot and keeps stable coordinates', () => {
  const nodes = buildSpotNodes([
    { spotId: 101, spotName: 'West Lake', username: 'Zhou', voteType: 'must' },
    { spotId: 101, spotName: 'West Lake', username: 'Alex', voteType: 'want' },
    { spotId: 102, spotName: 'Nanjing Road', username: 'Zhou', voteType: 'backup' },
  ])

  assert.equal(nodes.length, 2)
  assert.equal(nodes[0].spotName, 'West Lake')
  assert.equal(nodes[0].votes.length, 2)
  assert.equal(nodes[0].consensus, 'want')
  assert.equal(nodes[0].x, 18)
  assert.equal(nodes[0].y, 28)
})

test('buildRealSpotNodes keeps real destination coordinates', () => {
  const nodes = buildRealSpotNodes([
    {
      destinationId: 11,
      spotId: 11,
      spotName: 'West Lake',
      latitude: 30.259244,
      longitude: 120.13026,
      votes: [{ username: 'Zhou', voteType: 'must' }],
    },
  ])

  assert.equal(nodes.length, 1)
  assert.equal(nodes[0].spotName, 'West Lake')
  assert.equal(nodes[0].latitude, 30.259244)
  assert.equal(nodes[0].longitude, 120.13026)
  assert.equal(nodes[0].consensus, 'must')
})

test('buildRealSpotNodes filters records without real coordinates', () => {
  const nodes = buildRealSpotNodes([
    { destinationId: 12, spotId: 12, spotName: 'Missing Coordinates', latitude: null, longitude: null, votes: [] },
  ])

  assert.deepEqual(nodes, [])
})

test('summarizeConsensus counts agreed conflict and must nodes', () => {
  const summary = summarizeConsensus([
    { consensus: 'must' },
    { consensus: 'want' },
    { consensus: 'conflict' },
    { consensus: 'backup' },
  ])

  assert.deepEqual(summary, {
    total: 4,
    agreed: 2,
    conflicts: 1,
    must: 1,
  })
})

test('voteTypeLabel returns UI labels', () => {
  assert.equal(voteTypeLabel('must'), '必去')
  assert.equal(voteTypeLabel('want'), '想去')
  assert.equal(voteTypeLabel('avoid'), '不想去')
  assert.equal(voteTypeLabel('backup'), '备选')
  assert.equal(voteTypeLabel('unknown'), '未标记')
})
