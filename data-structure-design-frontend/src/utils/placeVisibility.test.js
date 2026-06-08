import assert from 'node:assert/strict'
import test from 'node:test'
import { isPublicFacilityPlace } from './placeVisibility.js'

test('internal school buildings are hidden from public facility UI', () => {
  assert.equal(isPublicFacilityPlace('雍和宫_教学楼1', '建筑'), false)
  assert.equal(isPublicFacilityPlace('雍和宫_宿舍楼1', '宿舍楼'), false)
  assert.equal(isPublicFacilityPlace('北京邮电大学_办公楼', '建筑'), false)
  assert.equal(isPublicFacilityPlace('雍和宫_商店1', '商店'), true)
  assert.equal(isPublicFacilityPlace('雍和宫_洗手间1', '洗手间'), true)
})
