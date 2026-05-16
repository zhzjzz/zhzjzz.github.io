import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { previewItineraryPlan } from '../api/travel'
import { buildPlannerPayload, selectedPlannerCount } from '../utils/itineraryPlanner'

export const useItineraryPlanner = () => {
  const loading = ref(false)
  const preview = ref(null)
  const error = ref('')

  const generatePreview = async (options) => {
    error.value = ''
    const payload = buildPlannerPayload(options)
    if (selectedPlannerCount(payload.spots) < 1) {
      error.value = '请选择至少一个景点'
      ElMessage.warning(error.value)
      return null
    }

    loading.value = true
    try {
      const { data } = await previewItineraryPlan(payload)
      preview.value = data
      return data
    } catch (caught) {
      error.value = caught?.response?.data?.message || caught?.message || '一键规划失败'
      ElMessage.error(error.value)
      return null
    } finally {
      loading.value = false
    }
  }

  const resetPreview = () => {
    preview.value = null
    error.value = ''
  }

  return {
    loading,
    preview,
    error,
    generatePreview,
    resetPreview,
  }
}
