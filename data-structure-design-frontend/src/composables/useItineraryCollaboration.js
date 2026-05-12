import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const resolveWsUrl = () => {
  const configured = import.meta.env.VITE_WS_URL
  if (configured) return configured
  return '/ws'
}

export const useItineraryCollaboration = () => {
  let client = null
  let subscription = null
  let currentItineraryId = null
  let currentUsername = ''

  const connected = ref(false)
  const connecting = ref(false)
  const onlineUsers = ref([])
  const lastError = ref('')
  const events = ref([])
  const editingUsers = reactive({})

  const activeEditors = computed(() => {
    return Object.entries(editingUsers)
      .filter(([, editor]) => editor?.username)
      .map(([field, editor]) => ({ field, ...editor }))
  })

  const addEvent = (message) => {
    if (!message) return
    events.value.unshift({
      text: message,
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
    })
    events.value = events.value.slice(0, 8)
  }

  const publish = (body) => {
    if (!client?.connected || !currentItineraryId) return false
    client.publish({
      destination: `/app/itinerary/${currentItineraryId}/edit`,
      body: JSON.stringify(body),
    })
    return true
  }

  const disconnect = () => {
    if (client?.connected && currentItineraryId && currentUsername) {
      publish({ type: 'LEAVE', username: currentUsername })
    }
    subscription?.unsubscribe?.()
    subscription = null
    client?.deactivate?.()
    client = null
    connected.value = false
    connecting.value = false
    currentItineraryId = null
  }

  const connect = ({ itineraryId, username, onUpdated, onConflict }) => {
    disconnect()
    currentItineraryId = itineraryId
    currentUsername = username || '协作者'
    connecting.value = true
    lastError.value = ''
    onlineUsers.value = []
    events.value = []
    Object.keys(editingUsers).forEach((key) => delete editingUsers[key])

    client = new Client({
      webSocketFactory: () => new SockJS(resolveWsUrl()),
      reconnectDelay: 3000,
      onConnect: () => {
        connected.value = true
        connecting.value = false
        subscription = client.subscribe(`/topic/itinerary/${itineraryId}`, (frame) => {
          const payload = JSON.parse(frame.body)
          if (Array.isArray(payload.onlineUsers)) {
            onlineUsers.value = payload.onlineUsers
          }
          if (payload.type === 'JOINED') {
            addEvent(`${payload.username || '协作者'}加入协作`)
          }
          if (payload.type === 'LEFT') {
            addEvent(`${payload.username || '协作者'}离开协作`)
          }
          if (payload.type === 'EDITING' && payload.field && payload.username !== currentUsername) {
            editingUsers[payload.field] = {
              username: payload.username,
              timestamp: Date.now(),
            }
          }
          if (payload.type === 'UPDATED') {
            if (payload.field) delete editingUsers[payload.field]
            addEvent(`${payload.username || '协作者'}更新了行程`)
            onUpdated?.(payload)
          }
          if (payload.type === 'CONFLICT') {
            lastError.value = payload.message || '协作保存冲突'
            onConflict?.(payload)
          }
        })
        publish({ type: 'JOIN', username: currentUsername })
      },
      onWebSocketClose: () => {
        connected.value = false
        connecting.value = false
      },
      onStompError: (frame) => {
        lastError.value = frame.headers?.message || '协作连接异常'
      },
    })

    client.activate()
  }

  const sendEditing = (field) => {
    publish({ type: 'EDITING', username: currentUsername, field })
  }

  const sendPatch = ({ field, value, expectedUpdatedAt }) => {
    return publish({
      type: 'PATCH',
      username: currentUsername,
      field,
      value,
      expectedUpdatedAt,
    })
  }

  onBeforeUnmount(disconnect)

  return {
    connected,
    connecting,
    onlineUsers,
    activeEditors,
    lastError,
    events,
    connect,
    disconnect,
    sendEditing,
    sendPatch,
  }
}
