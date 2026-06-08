import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const routeViewPath = fileURLToPath(new URL('./RouteView.vue', import.meta.url))
const source = readFileSync(routeViewPath, 'utf8')

test('route visit places are filtered before demo node defaults are chosen', () => {
  assert.match(source, /import \{[^}]*isPublicFacilityPlace[^}]*\} from '\.\.\/utils\/placeVisibility'/)
  assert.match(source, /\.filter\(\(place\) => isPublicFacilityPlace\(place\.name, place\.type\)\)/)
})

test('return-to-start control is visually grouped with the plan action', () => {
  const returnControlIndex = source.indexOf('active-text="返回起点"')
  const planActionIndex = source.indexOf('<MapRoad theme="outline"')

  assert.notEqual(returnControlIndex, -1)
  assert.notEqual(planActionIndex, -1)
  assert.ok(returnControlIndex < planActionIndex)
})

test('demo scenario fills three scenic visits and enables optimization', () => {
  assert.match(source, /selected\.length >= 3/)
  assert.match(source, /spots\.slice\(0, 3\)/)
  assert.match(source, /optimizeVisitOrder\.value = true/)
})

test('route optimization control describes scenic spot and point ordering', () => {
  assert.match(source, /active-text="优化景区与地点顺序"/)
  assert.match(source, /演示路线已填充 3 个景点，并开启景区与地点顺序优化/)
})
