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
  assert.match(source, /const diaryPreviewCover = \(diary\) => \{/)
  assert.match(source, /const uploadedCover = diaryCover\(diary\)/)
  assert.match(source, /return aigcImageUrl\(diary\) \|\| uploadedCover/)
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

test('diary feed exposes ranking and destination sorting controls', () => {
  assert.match(source, /const rankingMode = ref\('recommend'\)/)
  assert.match(source, /const destinationSort = ref\('recommend'\)/)
  assert.match(source, /const interestKeyword = ref\(''\)/)
  assert.match(source, /\{ label: '兴趣推荐', value: 'interest' \}/)
  assert.match(source, /placeholder="输入兴趣，如自然\/历史\/美食；留空默认美食"/)
  assert.match(source, /searchDiariesByDestination\(keyword, destinationSort\.value, 20\)/)
  assert.match(source, /searchDiaryFullText\(keyword, rankingMode\.value, searchInterest, 20\)/)
})

test('diary feed header and toolbar are separated into two rows', () => {
  assert.match(source, /\.feed-toolbar \{\s*display: grid;/)
  assert.match(source, /\.toolbar-copy \{\s*display: grid;/)
})

test('selected story comments are previewed with a show-more toggle', () => {
  assert.match(source, /const COMMENT_PREVIEW_LIMIT = 15/)
  assert.match(source, /const selectedDiaryComments = computed\(/)
  assert.match(source, /class="comment-toggle-button"/)
  assert.match(source, /'查看更多'/)
})

test('selected story shows the composite diary score next to rating input', () => {
  assert.match(source, /综合评分 \{\{ Number\(selectedDiary\.score \|\| 0\)\.toFixed\(1\) \}\}/)
})

test('search results switch to a ranking-board layout while default feed stays masonry', () => {
  assert.match(source, /const isSearchResultsMode = computed\(\(\) => Boolean\(searchKeyword\.value\.trim\(\)\)\)/)
  assert.match(source, /<section v-if="isSearchResultsMode" class="search-ranking-panel reveal-in"/)
  assert.match(source, /class="hot-note-card search-ranking-card"/)
  assert.match(source, /<section v-else class="diary-flow reveal-in"/)
})

test('aigc metadata uses compact link text instead of full raw url', () => {
  assert.match(source, /const aigcLinkText = \(diary\) =>/)
  assert.match(source, /已生成旅行图链接/)
  assert.match(source, /已生成旅行视频链接/)
  assert.match(source, /<small>\{\{ aigcLinkText\(selectedDiary\) \}\}<\/small>/)
})

test('diary detail supports video generation and polling', () => {
  assert.match(source, /createDiaryVideo/)
  assert.match(source, /getDiaryVideoStatus/)
  assert.match(source, /const generateSelectedDiaryVideo = async \(\) =>/)
  assert.match(source, /const pollDiaryVideoStatus = async \(diaryId\) =>/)
  assert.match(source, /const stopDiaryVideoPolling = \(\) =>/)
  assert.match(source, /const VIDEO_POLL_INTERVAL_MS = 2500/)
  assert.match(source, /window\.setTimeout\(\(\) => pollDiaryVideoStatus\(diaryId\), VIDEO_POLL_INTERVAL_MS\)/)
  assert.match(source, /onBeforeUnmount\(\(\) => \{/)
})

test('existing in-progress diary video tasks resume polling on selection', () => {
  assert.match(source, /const VIDEO_ACTIVE_STATUSES = new Set\(\['queued', 'processing'\]\)/)
  assert.match(source, /const scheduleDiaryVideoPolling = \(diaryId\) =>/)
  assert.match(source, /\(\) => \[selectedDiary\.value\?\.id, selectedDiary\.value\?\.aigcVideoId, selectedDiary\.value\?\.aigcStatus\]/)
  assert.match(source, /VIDEO_ACTIVE_STATUSES\.has\(normalizedStatus\)/)
  assert.match(source, /\{ immediate: true \}/)
})

test('completed diary video renders in the detail area', () => {
  assert.match(source, /const selectedDiaryVideoUrl = computed\(\(\) =>/)
  assert.match(source, /<video v-if="selectedDiaryVideoUrl" class="detail-video-player" :src="selectedDiaryVideoUrl" controls playsinline preload="metadata"><\/video>/)
  assert.match(source, /@click="generateSelectedDiaryVideo"/)
})
