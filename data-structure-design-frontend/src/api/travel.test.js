import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const travelApiPath = fileURLToPath(new URL('./travel.js', import.meta.url))
const source = readFileSync(travelApiPath, 'utf8')

test('diary video task calls use longer timeouts than the global axios default', () => {
  assert.match(source, /createDiaryVideo = \(id\) => http\.post\(`\/diaries\/\$\{id\}\/video`, null, \{ timeout: 120000 \}\)/)
  assert.match(source, /getDiaryVideoStatus = \(id\) => http\.get\(`\/diaries\/\$\{id\}\/video`, \{ timeout: 60000 \}\)/)
})
