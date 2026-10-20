<template>
  <div class="consult">
    <aside class="list card">
      <div class="card-title">会话</div>
      <div
        v-for="c in conversations"
        :key="c.userId"
        class="conv"
        :class="{ active: c.userId === currentId }"
        @click="open(c.userId)"
      >
        <div class="avatar">{{ c.initial }}</div>
        <div class="info">
          <div class="row between">
            <span class="name">{{ c.name }}</span>
            <span class="time">{{ short(c.lastTime) }}</span>
          </div>
          <div class="last">{{ c.lastMessage }}</div>
        </div>
        <el-badge v-if="c.unread" :value="c.unread" class="badge" />
      </div>
      <el-empty v-if="conversations.length === 0" description="暂无会话" :image-size="70" />
    </aside>

    <section class="chat card">
      <template v-if="currentId">
        <div class="chat-head">
          <span class="name">{{ currentName }}</span>
          <span class="muted" style="font-size: 12px">已建立咨询关系</span>
        </div>
        <div ref="scroller" class="messages">
          <div v-for="m in messages" :key="m.id" class="msg" :class="{ mine: m.fromId === myId }">
            <div class="bubble">{{ m.content }}</div>
            <div class="t">{{ short(m.createdAt) }}</div>
          </div>
        </div>
        <div class="composer">
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            resize="none"
            placeholder="输入消息…（Enter 发送）"
            @keydown.enter.exact.prevent="send"
          />
          <el-button type="primary" :disabled="!input.trim()" @click="send">发送</el-button>
        </div>
      </template>
      <div v-else class="placeholder">
        <el-icon :size="30" color="#b8c4bd"><ChatDotRound /></el-icon>
        <p class="muted">选择左侧会话开始沟通</p>
        <el-button v-if="role === 'USER'" @click="$router.push('/doctors')">去找咨询师</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store/user'

const store = useUserStore()
const role = computed(() => store.role)
const myId = computed(() => store.user?.id)
const conversations = ref([])
const currentId = ref(null)
const currentName = ref('')
const messages = ref([])
const input = ref('')
const scroller = ref(null)
let timer = null

const short = (s) => {
  if (!s) return ''
  const t = String(s).replace('T', ' ')
  return t.slice(5, 16)
}

const loadConversations = async () => {
  const res = await api.conversations()
  conversations.value = res.data || []
}

const open = async (id) => {
  currentId.value = id
  const c = conversations.value.find((x) => x.userId === id)
  currentName.value = c?.name || ''
  await loadHistory()
  const conv = conversations.value.find((x) => x.userId === id)
  if (conv) conv.unread = 0
}

const loadHistory = async () => {
  if (!currentId.value) return
  const res = await api.history(currentId.value)
  const changed = JSON.stringify(res.data) !== JSON.stringify(messages.value)
  messages.value = res.data || []
  if (changed) {
    await nextTick()
    if (scroller.value) scroller.value.scrollTop = scroller.value.scrollHeight
  }
}

const send = async () => {
  const content = input.value.trim()
  if (!content) return
  input.value = ''
  try {
    await api.sendMessage({ toId: currentId.value, content })
    await loadHistory()
    await loadConversations()
  } catch (e) {
    input.value = content
  }
}

onMounted(async () => {
  await loadConversations()
  if (conversations.value.length) {
    await open(conversations.value[0].userId)
  }
  timer = setInterval(async () => {
    try {
      await loadConversations()
      if (currentId.value) await loadHistory()
    } catch (e) {
      // ignore
    }
  }, 8000)
})

onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.consult {
  display: flex;
  gap: 16px;
  height: calc(100vh - 122px);
}

.list {
  width: 268px;
  flex: 0 0 268px;
  overflow-y: auto;
  padding: 16px 12px;
}

.conv {
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  transition: background 0.15s;
}

.conv:hover {
  background: #f5f4ef;
}

.conv.active {
  background: var(--sage-soft);
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #eef1ee;
  display: grid;
  place-items: center;
  font-size: 13px;
  color: var(--ink-2);
  flex: 0 0 34px;
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 13.5px;
  font-weight: 500;
}

.time {
  font-size: 11px;
  color: var(--ink-3);
}

.last {
  font-size: 12px;
  color: var(--ink-3);
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge {
  position: absolute;
  right: 10px;
  bottom: 10px;
}

.chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.chat-head {
  height: 52px;
  flex: 0 0 52px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid var(--line);
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 18px 20px;
  background: #fcfcfa;
}

.msg {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 14px;
}

.msg.mine {
  align-items: flex-end;
}

.bubble {
  max-width: 68%;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 11px;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ink-2);
  white-space: pre-wrap;
}

.mine .bubble {
  background: var(--sage);
  border-color: var(--sage);
  color: #fff;
}

.t {
  font-size: 10.5px;
  color: var(--ink-3);
  margin-top: 4px;
}

.composer {
  border-top: 1px solid var(--line);
  padding: 12px 16px;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: #fff;
}

.composer :deep(.el-textarea__inner) {
  box-shadow: none;
  border: 1px solid var(--line);
}

.composer .el-button {
  height: 38px;
  width: 74px;
}

.placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.placeholder p {
  margin: 0;
  font-size: 13px;
}
</style>
