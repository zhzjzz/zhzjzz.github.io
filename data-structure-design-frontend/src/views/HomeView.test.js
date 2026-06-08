import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const homeViewPath = fileURLToPath(new URL('./HomeView.vue', import.meta.url))
const source = readFileSync(homeViewPath, 'utf8')

test('home food recommendations prefer AMap live data with local fallback', () => {
  assert.match(source, /import \{[^}]*searchAmapFoods[^}]*getTopFoods|import \{[^}]*getTopFoods[^}]*searchAmapFoods/)

  const liveCallIndex = source.indexOf('searchAmapFoods(')
  const fallbackCallIndex = source.indexOf('getTopFoods(')

  assert.notEqual(liveCallIndex, -1)
  assert.notEqual(fallbackCallIndex, -1)
  assert.ok(liveCallIndex < fallbackCallIndex)
})
