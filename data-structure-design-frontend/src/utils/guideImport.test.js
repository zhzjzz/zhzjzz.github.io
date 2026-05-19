import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildDiaryImportPayload,
  buildTextImportPayload,
  DEFAULT_GUIDE_IMPORT_DEMO_TEXT,
  importCanCreate,
  importSpotLabel,
} from './guideImport.js'

test('buildTextImportPayload trims text and owner', () => {
  assert.deepEqual(buildTextImportPayload('  go to The Bund  ', '  Zhou  '), {
    sourceType: 'TEXT',
    text: 'go to The Bund',
    diaryId: null,
    owner: 'Zhou',
  })
})

test('buildDiaryImportPayload uses diary id', () => {
  assert.deepEqual(buildDiaryImportPayload(9, 'Lin'), {
    sourceType: 'DIARY',
    text: null,
    diaryId: 9,
    owner: 'Lin',
  })
})

test('importCanCreate requires at least one matched spot', () => {
  assert.equal(importCanCreate({ spots: [] }), false)
  assert.equal(importCanCreate({ spots: [{ matchedDestinationId: 1 }] }), true)
})

test('importSpotLabel includes order and stay time', () => {
  assert.equal(importSpotLabel({ orderIndex: 1, matchedName: 'The Bund', stayMinutes: 90 }), '2. The Bund · 90 min')
})

test('default demo text contains line-separated local destinations', () => {
  assert.ok(DEFAULT_GUIDE_IMPORT_DEMO_TEXT.includes('观复博物馆'))
  assert.ok(DEFAULT_GUIDE_IMPORT_DEMO_TEXT.includes('北京理工大学'))
  assert.ok(DEFAULT_GUIDE_IMPORT_DEMO_TEXT.includes('八达岭野生动物园'))
  assert.equal(DEFAULT_GUIDE_IMPORT_DEMO_TEXT.split('\n').length, 3)
})
