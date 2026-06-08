import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const destinationViewPath = fileURLToPath(new URL('./DestinationView.vue', import.meta.url))
const source = readFileSync(destinationViewPath, 'utf8')

test('destination interest options exclude food', () => {
  const optionsLine = source.match(/const interestOptions = \[[^\]]+\]/)?.[0] || ''

  assert.doesNotMatch(optionsLine, /美食|缇庨/)
})

test('destination search results can be sorted by heat or rating', () => {
  assert.match(source, /const searchSort = ref\(''\)/)
  assert.match(source, /const hasSortableSearchResults = computed/)
  assert.match(source, /searchDestinations\(keyword\.value, searchSort\.value\)/)
  assert.match(source, /setSearchSort\('heat'\)/)
  assert.match(source, /setSearchSort\('rating'\)/)
  assert.match(source, /v-if="hasSortableSearchResults"/)
  assert.doesNotMatch(source, />\s*Top10\s*</)
})
