import assert from 'node:assert/strict'
import test from 'node:test'

import { foodIconName, foodIconLabel } from '../src/utils/foodIcon.js'

test('maps common restaurant types to real food icons', () => {
  assert.equal(foodIconName({ cuisine: '咖啡' }), 'CoffeeMachine')
  assert.equal(foodIconName({ cuisine: '烘焙' }), 'Bread')
  assert.equal(foodIconName({ cuisine: '甜品' }), 'Cake')
  assert.equal(foodIconName({ cuisine: '西式简餐' }), 'Hamburger')
  assert.equal(foodIconName({ cuisine: '面食' }), 'Noodles')
  assert.equal(foodIconName({ cuisine: '鸡肉', name: '炸鸡' }), 'ChickenLeg')
})

test('uses readable labels for card icon badges', () => {
  assert.equal(foodIconLabel({ cuisine: 'coffee' }), '咖啡')
  assert.equal(foodIconLabel({ cuisine: 'fast_food' }), '快餐')
  assert.equal(foodIconLabel({ cuisine: 'unknown', name: 'The Reds' }), '餐饮')
})
