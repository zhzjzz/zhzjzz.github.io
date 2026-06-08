import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const diaryViewPath = fileURLToPath(new URL('./DiaryView.vue', import.meta.url))
const source = readFileSync(diaryViewPath, 'utf8')

test('diary detail renders generated AIGC image directly', () => {
  assert.match(source, /const aigcImageUrl = \(diary\) =>/)
  assert.match(source, /selectedDiaryAigcImageUrl/)
  assert.match(source, /:src="selectedDiaryAigcImageUrl \|\| selectedDiaryImageUrl \|\| diaryCover\(selectedDiary\)"/)
})

test('diary feed preview prefers generated AIGC image', () => {
  assert.match(source, /const diaryPreviewCover = \(diary\) => aigcImageUrl\(diary\) \|\| diaryCover\(diary\)/)
  assert.match(source, /<img :src="diaryPreviewCover\(item\)" :alt="item\.title \|\| '旅游日记封面'"/)
  assert.match(source, /<img :src="diaryPreviewCover\(item\)" :alt="item\.title \|\| '热门日记封面'"/)
})

test('diary detail has a prominent AIGC generation action', () => {
  assert.match(source, /class="aigc-primary-button"/)
  assert.match(source, /size="large"[\s\S]*generateSelectedDiaryImage/)
})

test('diary hero does not duplicate the publish action', () => {
  const heroSection = source.match(/<section class="diary-hero[\s\S]*?<\/section>/)?.[0] || ''

  assert.doesNotMatch(heroSection, /@click="openComposer"/)
  assert.match(source, /class="composer-trigger"[\s\S]*@click="openComposer"/)
})

test('diary publish trigger has no helper subtitle', () => {
  assert.doesNotMatch(source, /点开后再展开编辑，不占首屏位置/)
  assert.doesNotMatch(source, /<small>[\s\S]*?发布旅游日记[\s\S]*?<\/small>/)
})
