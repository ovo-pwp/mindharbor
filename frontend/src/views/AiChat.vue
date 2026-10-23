<template>
  <div class="chat-page">
    <aside class="sessions">
      <el-button class="new-btn" @click="newSession">
        <el-icon><Plus /></el-icon> 新的对话
      </el-button>
      <div class="list">
        <div
          v-for="s in sessions"
          :key="s.id"
          class="session"
          :class="{ active: s.id === currentId }"
          @click="openSession(s.id)"
        >
          <el-icon :size="14"><ChatLineRound /></el-icon>
          <span class="s-title">{{ s.title }}</span>
          <el-icon class="del" :size="13" @click.stop="removeSession(s.id)"><Delete /></el-icon>
        </div>
        <div v-if="sessions.length === 0" class="empty muted">还没有对话记录</div>
      </div>
      <div class="tip">
        AI 助手提供情绪支持与心理教育，不做诊断。危机情况请拨打 400-161-9995。
      </div>
    </aside>

    <section class="main">
      <div class="m-head">
        <span class="m-title">AI 心理助手</span>
        <button class="m-new" @click="newSession">+ 新对话</button>
      </div>
      <div ref="scroller" class="messages">
        <div v-if="messages.length === 0 && !streaming" class="welcome">
          <div class="wlogo">心</div>
          <h2>我在这里，你想从哪儿说起？</h2>
          <p class="muted">不需要组织语言，想到什么说什么就好。</p>
          <div class="chips">
            <span v-for="c in chips" :key="c" @click="quickSend(c)">{{ c }}</span>
          </div>
        </div>

        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <div class="avatar" :class="m.role">{{ m.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble">
            <div class="markdown-body" v-html="render(m.content)"></div>
          </div>
        </div>

        <!-- 等待反馈：思考中 / 正在调用技能。用 trim 判断，避免模型开头的空行撑出一个空气泡 -->
        <div v-if="streaming && (toolHint || !streamText.trim())" class="tool-hint">
          <span class="spinner" />
          {{ toolHint ? `正在调用「${toolHint}」…` : '正在思考…' }}
        </div>

        <div v-if="streaming && streamText.trim()" class="msg assistant">
          <div class="avatar assistant">AI</div>
          <div class="bubble">
            <div class="markdown-body" v-html="render(streamText)"></div>
            <span class="cursor" />
          </div>
        </div>
      </div>

      <div class="buddy">
        <div
          class="ball-wrap"
          :class="{ poked: poking }"
          @mouseenter="onBallEnter"
          @mouseleave="onBallLeave"
          @click="onBallTap"
        >
          <iframe ref="ballFrame" src="/emotion-ball/embed.html" class="ball-frame" title="伙伴" />
          <span class="ball-mask" />
        </div>
        <span class="buddy-text">{{ buddyText }}</span>
      </div>

      <div class="composer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          :placeholder="placeholderText"
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" :loading="streaming" :disabled="!input.trim()" @click="send">
          发送
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import { api } from '../api'

const sessions = ref([])
const messages = ref([])
const currentId = ref(null)
const input = ref('')
const streaming = ref(false)
const streamText = ref('')
const toolHint = ref('')
const toolResults = ref([])
const ballFrame = ref(null)
const ballEmotion = ref('02')
const buddyState = ref('idle')

const buddyText = computed(() => ({
  idle: '我在',
  thinking: '在想',
  replying: '在说',
  feeling: '我懂'
}[buddyState.value] || '我在'))

// ---- 球的小互动：悬停会「好奇」，点它会「惊讶 / 害羞 / 无奈」----
const poking = ref(false)
let tapCount = 0
let tapTimer = null

const canInteract = () => buddyState.value === 'idle'

const onBallEnter = () => {
  if (canInteract()) setBall('03')
}

const onBallLeave = () => {
  clearTimeout(tapTimer)
  tapCount = 0
  if (canInteract()) setBall('02')
}

const onBallTap = () => {
  if (!canInteract()) return
  tapCount++
  clearTimeout(tapTimer)

  // 点一下惊讶，两下害羞，三下以上无奈
  if (tapCount === 1) setBall('13')
  else if (tapCount === 2) setBall('14')
  else setBall('18')

  poking.value = true
  setTimeout(() => { poking.value = false }, 180)

  tapTimer = setTimeout(() => {
    tapCount = 0
    if (canInteract()) setBall('02')
  }, 1400)
}

const setBall = (id) => {
  ballEmotion.value = id
  try {
    ballFrame.value?.contentWindow?.postMessage({ type: 'emotion', id }, '*')
  } catch (e) { }
}

watch(ballEmotion, (id) => {
  try {
    ballFrame.value?.contentWindow?.postMessage({ type: 'emotion', id }, '*')
  } catch (e) { }
})

// 手机上 Enter 是换行不是发送，别给错误提示
const placeholderText = (typeof window !== 'undefined' && window.innerWidth <= 768)
  ? '说说你现在的心情…'
  : '说说你现在的心情…（Enter 发送，Shift + Enter 换行）'
const scroller = ref(null)

const chips = [
  '最近总是睡不着',
  '工作压力大到喘不过气',
  '和家里人吵架了',
  '我好像对什么都提不起兴趣',
  '总是很焦虑，停不下来'
]

marked.setOptions({ breaks: true, gfm: true })
const render = (text) => marked.parse(text || '')

const scrollBottom = async () => {
  await nextTick()
  if (scroller.value) scroller.value.scrollTop = scroller.value.scrollHeight
}

const loadSessions = async () => {
  const res = await api.aiSessions()
  sessions.value = res.data || []
}

const openSession = async (id) => {
  currentId.value = id
  const res = await api.aiMessages(id)
  messages.value = res.data || []
  scrollBottom()
}

const newSession = () => {
  currentId.value = null
  messages.value = []
  input.value = ''
}

const removeSession = async (id) => {
  try {
    await ElMessageBox.confirm('删除这个对话？记录将无法恢复。', '确认删除', { type: 'warning' })
  } catch (e) {
    return
  }
  await api.aiDeleteSession(id)
  if (currentId.value === id) newSession()
  await loadSessions()
}

const quickSend = (text) => {
  input.value = text
  send()
}

const send = async () => {
  const content = input.value.trim()
  if (!content || streaming.value) return
  input.value = ''
  messages.value.push({ role: 'user', content })
  streaming.value = true
  streamText.value = ''
  toolHint.value = ''
  toolResults.value = []
  buddyState.value = 'thinking'
  setBall('30')
  scrollBottom()

  try {
    const token = localStorage.getItem('token')
    const resp = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({ sessionId: currentId.value, content })
    })
    if (resp.status === 401) {
      // 登录态失效（比如服务重启）：不能只报「对话失败」，否则用户以为功能坏了
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      ElMessage.warning('登录已过期，正在跳转登录页…')
      setTimeout(() => { location.hash = '#/login' }, 900)
      return
    }
    if (!resp.ok || !resp.body) {
      throw new Error('连接失败')
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split('\n\n')
      buffer = blocks.pop() || ''
      for (const block of blocks) {
        const line = block.split('\n').find((l) => l.startsWith('data:'))
        if (!line) continue
        const raw = line.slice(5).trim()
        if (!raw) continue
        let evt
        try {
          evt = JSON.parse(raw)
        } catch (e) {
          continue
        }
        if (evt.type === 'delta') {
          streamText.value += evt.content
          if (buddyState.value !== 'replying') {
            buddyState.value = 'replying'
            setBall('39')
          }
          toolHint.value = ''
          scrollBottom()
        } else if (evt.type === 'tool') {
          // 模型正在调用某个技能，先给用户一个可见的反馈
          toolHint.value = evt.label || evt.name || ''
          scrollBottom()
        } else if (evt.type === 'tool_result') {
          // 本地兜底执行了技能，结果单独记下，稍后附在回复末尾
          toolResults.value.push(evt)
        } else if (evt.type === 'meta') {
          if (!currentId.value && evt.sessionId) {
            currentId.value = evt.sessionId
          }
        } else if (evt.type === 'done') {
          if (evt.sessionId) currentId.value = evt.sessionId
        } else if (evt.type === 'error') {
          ElMessage.warning(evt.message || '生成中断')
        }
      }
    }

    if (toolResults.value.length) {
      const lines = toolResults.value.map((r) => '*✅ ' + r.text + '*').join('\n')
      streamText.value += (streamText.value ? '\n\n---\n' : '') + lines
    }

    if (streamText.value) {
      messages.value.push({ role: 'assistant', content: streamText.value })
    }
    streamText.value = ''
    await loadSessions()
    scrollBottom()
  } catch (e) {
    ElMessage.error('对话失败，请稍后重试')
    streamText.value = ''
  } finally {
    streaming.value = false
    toolHint.value = ''
    buddyState.value = 'idle'
    setBall('02')
  }
}

// 小球视线跟随鼠标。
// 注意：坐标必须在这里同步取出来，不能放进 requestAnimationFrame ——
// 事件对象在回调执行时可能已被浏览器回收，读出来是 0。
let lastGazeAt = 0
const onMouseMove = (e) => {
  const now = performance.now()
  if (now - lastGazeAt < 60) return
  lastGazeAt = now

  const x = e.clientX
  const y = e.clientY
  const el = document.querySelector('.ball-wrap')
  if (!el) return
  const r = el.getBoundingClientRect()
  const cx = r.left + r.width / 2
  const cy = r.top + r.height / 2
  const nx = Math.max(-1, Math.min(1, (x - cx) / 200))
  const ny = Math.max(-1, Math.min(1, (y - cy) / 200))

  // 引擎内部视线幅度只有球体的 3.4%，缩到 56px 后不足 2px，看不出来。
  // 不改引擎（放大内部值会触发彩带异常），改成整体位移 + 轻微倾斜，肉眼更明显。
  el.style.transform = 'translate(' + (nx * 3.5).toFixed(1) + 'px, ' + (ny * 2.5).toFixed(1) + 'px) rotate(' + (nx * 4).toFixed(1) + 'deg)'
  el.style.transition = 'transform 0.25s cubic-bezier(0.33, 0, 0.67, 1)'

  try {
    ballFrame.value?.contentWindow?.postMessage({ type: 'gaze', x: nx, y: ny }, '*')
  } catch (err) { }
}

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMouseMove)
})

onMounted(loadSessions)
</script>

<style scoped>
.chat-page {
  display: flex;
  gap: 16px;
  height: calc(100vh - 122px);
}

.sessions {
  width: 232px;
  flex: 0 0 232px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 14px;
  display: flex;
  flex-direction: column;
}

.new-btn {
  width: 100%;
  margin-bottom: 12px;
}

.list {
  flex: 1;
  overflow-y: auto;
}

.session {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--ink-2);
  cursor: pointer;
  transition: background 0.15s;
}

.session:hover {
  background: #f4f3ee;
}

.session.active {
  background: var(--sage-soft);
  color: var(--sage-dark);
  font-weight: 500;
}

.s-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.del {
  opacity: 0;
  color: var(--ink-3);
}

.session:hover .del {
  opacity: 1;
}

.empty {
  font-size: 12.5px;
  padding: 16px 6px;
  text-align: center;
}

.tip {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--ink-3);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 26px 30px;
}

.welcome {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.wlogo {
  width: 56px;
  height: 56px;
  border-radius: 17px;
  background: linear-gradient(135deg, #5d8f7a, #3c6455);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 24px;
  margin-bottom: 18px;
}

.welcome h2 {
  font-size: 20px;
  margin: 0 0 8px;
}

.welcome p {
  margin: 0 0 24px;
  font-size: 13px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  justify-content: center;
  max-width: 520px;
}

.chips span {
  padding: 7px 13px;
  border-radius: 20px;
  border: 1px solid var(--line);
  font-size: 12.5px;
  color: var(--ink-2);
  cursor: pointer;
  transition: all 0.15s;
}

.chips span:hover {
  border-color: var(--sage);
  color: var(--sage-dark);
  background: var(--sage-soft);
}

.msg {
  display: flex;
  gap: 11px;
  margin-bottom: 20px;
}

.msg.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 12px;
  flex: 0 0 30px;
  background: #eef1ee;
  color: var(--ink-2);
}

.avatar.assistant {
  background: linear-gradient(135deg, #5d8f7a, #3c6455);
  color: #fff;
}

.bubble {
  max-width: 76%;
  padding: 12px 16px;
  border-radius: 12px;
  background: #f7f6f2;
  border: 1px solid #eeede7;
}

.msg.user .bubble {
  background: var(--sage);
  border-color: var(--sage);
  color: #fff;
}

.msg.user .bubble :deep(.markdown-body) {
  color: #fff;
}

.msg.user .bubble :deep(.markdown-body strong),
.msg.user .bubble :deep(.markdown-body blockquote) {
  color: #fff;
}

.msg.user .bubble :deep(.markdown-body blockquote) {
  background: rgba(255, 255, 255, 0.12);
  border-left-color: rgba(255, 255, 255, 0.5);
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 15px;
  background: var(--sage);
  vertical-align: text-bottom;
  animation: blink 1s steps(2) infinite;
}

/* 技能调用提示 */
.tool-hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px 41px;
  padding: 7px 14px;
  border-radius: 20px;
  background: var(--sage-soft);
  border: 1px solid #d7e6dd;
  color: var(--sage-dark);
  font-size: 12.5px;
}

.spinner {
  width: 11px;
  height: 11px;
  border: 2px solid rgba(79, 125, 107, 0.25);
  border-top-color: var(--sage);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.composer {
  border-top: 1px solid var(--line);
  padding: 14px 18px;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: #fcfcfa;
}

.composer :deep(.el-textarea__inner) {
  box-shadow: none;
  border: 1px solid var(--line);
  padding: 10px 13px;
}

.composer .el-button {
  height: 40px;
  width: 78px;
}

/* 移动端顶部条，桌面隐藏 */
.m-head {
  display: none;
}

@media (max-width: 768px) {
  .chat-page {
    height: calc(100vh - 100px);
    gap: 0;
  }
  .sessions {
    display: none;
  }
  .m-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 14px;
    border-bottom: 1px solid var(--line);
    flex: 0 0 auto;
  }
  .m-title {
    font-size: 13.5px;
    font-weight: 500;
    color: var(--ink);
  }
  .m-new {
    font-size: 12.5px;
    color: var(--sage-dark);
    background: var(--sage-soft);
    border: none;
    border-radius: 14px;
    padding: 5px 12px;
    cursor: pointer;
    font-family: inherit;
  }
  .messages {
    padding: 16px 13px;
  }
  .bubble {
    max-width: 86%;
    padding: 10px 13px;
    font-size: 13.5px;
  }
  .avatar {
    width: 26px;
    height: 26px;
    flex-basis: 26px;
    font-size: 11px;
  }
  .composer {
    padding: 10px 12px;
    gap: 8px;
  }
  .composer .el-button {
    width: 64px;
    height: 38px;
  }
  .tool-hint {
    margin-left: 35px;
    font-size: 12px;
  }
  .welcome {
    padding: 0 6px;
  }
  .welcome h2 {
    font-size: 17px;
  }
  .chips span {
    font-size: 12px;
    padding: 6px 11px;
  }
}

.buddy {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 16px 8px;
  flex: 0 0 auto;
}

.ball-wrap {
  width: 56px;
  height: 56px;
  flex: 0 0 56px;
  overflow: hidden;
  border-radius: 50%;
}

/* iframe 内部按 700x700 渲染，靠 scale 缩到 34px，球才完整 */
.ball-frame {
  width: 700px;
  height: 700px;
  border: 0;
  display: block;
  transform: scale(0.08);
  transform-origin: top left;
}

.buddy-text {
  font-size: 12px;
  color: var(--ink-3);
  letter-spacing: 0.5px;
}

/* 球可交互：位移在 .ball-wrap 上（跟随鼠标），
   被点时缩小的是内层 iframe，避免和视线位移抢同一个 transform */
.ball-wrap {
  position: relative;
  cursor: pointer;
}

/* iframe 内部是独立文档，点击不会冒泡上来，用一层透明遮罩接管点击 */
.ball-mask {
  position: absolute;
  inset: 0;
  border-radius: 50%;
}

.ball-wrap .ball-frame {
  transition: transform 0.16s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.ball-wrap.poked .ball-frame {
  transform: scale(0.068);
}
</style>
