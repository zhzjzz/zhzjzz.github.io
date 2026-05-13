import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildSpotNodes,
  computeConsensus,
  summarizeConsensus,
  voteTypeLabel,
} from './itineraryVotes.js'

test('computeConsensus returns must when must votes are the majority', () => {
  const result = computeConsensus([
    { username: '小周', voteType: 'must' },
    { username: '阿杰', voteType: 'must' },
    { username: '林同学', voteType: 'want' },
  ])

  assert.equal(result, 'must')
})

test('computeConsensus returns conflict when avoid votes are significant', () => {
  const result = computeConsensus([
    { username: '小周', voteType: 'avoid' },
    { username: '阿杰', voteType: 'avoid' },
    { username: '林同学', voteType: 'must' },
  ])

  assert.equal(result, 'conflict')
})

test('buildSpotNodes groups votes by spot and keeps stable coordinates', () => {
  const nodes = buildSpotNodes([
    { spotId: 101, spotName: '外滩', username: '小周', voteType: 'must' },
    { spotId: 101, spotName: '外滩', username: '阿杰', voteType: 'want' },
    { spotId: 102, spotName: '南京路', username: '小周', voteType: 'backup' },
  ])

  assert.equal(nodes.length, 2)
  assert.equal(nodes[0].spotName, '外滩')
  assert.equal(nodes[0].votes.length, 2)
  assert.equal(nodes[0].consensus, 'want')
  assert.equal(nodes[0].x, 18)
  assert.equal(nodes[0].y, 28)
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

test('voteTypeLabel returns Chinese UI labels', () => {
  assert.equal(voteTypeLabel('must'), '必去')
  assert.equal(voteTypeLabel('want'), '想去')
  assert.equal(voteTypeLabel('avoid'), '不想去')
  assert.equal(voteTypeLabel('backup'), '备选')
  assert.equal(voteTypeLabel('unknown'), '未标记')
})
