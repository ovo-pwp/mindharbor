<template>
  <div class="practice">
    <div class="page-head">
      <h1>心理练习</h1>
      <p>不是任务，是工具。做一半、做不下去、中途退出，都算数。</p>
    </div>

    <!-- ============ 列表 ============ -->
    <template v-if="view === 'list'">
      <div class="grid g3" style="margin-bottom: 18px">
        <div v-for="t in types" :key="t.type" class="card pick" @click="start(t)">
          <div class="pick-icon" :class="t.type">
            <span class="glyph">{{ glyph(t.type) }}</span>
          </div>
          <div class="pick-name">{{ t.name }}</div>
          <div class="pick-desc">{{ t.desc }}</div>
          <div class="pick-foot">
            <span class="muted">约 {{ t.minutes }} 分钟</span>
            <span class="go">开始 →</span>
          </div>
        </div>
      </div>

      <div v-if="summary.totalCount" class="card summary">
        最近 {{ summary.days }} 天做过 {{ summary.totalCount }} 次，
        累计 {{ summary.totalMinutes }} 分钟。做多做少都没关系。
      </div>

      <div class="card">
        <div class="card-title">最近做过</div>
        <div v-if="recent.length" class="records">
          <div v-for="r in recent" :key="r.id" class="rec">
            <span class="dot" :class="r.type" />
            <span class="rec-name">{{ nameOf(r.type) }}</span>
            <span class="rec-extra muted">{{ extraOf(r) }}</span>
            <span class="rec-time muted">{{ fmt(r.createdAt) }}</span>
          </div>
        </div>
        <div v-else class="muted" style="font-size: 13px; padding: 10px 0">
          还没有练习记录。挑一个试试，哪怕只做一轮呼吸。
        </div>
      </div>
    </template>

    <!-- ============ 呼吸 ============ -->
    <template v-else-if="view === 'breathing'">
      <div class="card stage">
        <div class="stage-head">
          <span class="muted">4-7-8 呼吸</span>
          <span class="muted">第 {{ round }} / {{ totalRounds }} 轮</span>
        </div>

        <div class="breath-area">
          <div class="ring-outer">
            <div
              class="ring"
              :style="{ transform: 'scale(' + scale + ')', transitionDuration: transMs + 'ms' }"
            >
              <div class="phase-name">{{ phaseName }}</div>
              <div class="phase-count">{{ remain }}</div>
            </div>
          </div>
        </div>

        <div class="stage-foot">
          <button class="ghost" @click="stopBreathing">停下来</button>
          <span class="muted">跟着圆圈，不用刻意深呼吸</span>
        </div>
      </div>
    </template>

    <!-- ============ 思维记录 ============ -->
    <template v-else-if="view === 'thought'">
      <div class="card stage">
        <div class="stage-head">
          <span class="muted">思维记录</span>
          <span class="muted">{{ thoughtStep + 1 }} / 5</span>
        </div>

        <div class="q-title">{{ thoughtSteps[thoughtStep].q }}</div>
        <div class="q-hint">{{ thoughtSteps[thoughtStep].hint }}</div>

        <el-input
          v-model="thought[thoughtSteps[thoughtStep].key]"
          type="textarea"
          :rows="5"
          resize="none"
          :placeholder="thoughtSteps[thoughtStep].ph"
        />

        <div class="stage-foot">
          <button class="ghost" @click="finishThought(true)">先到这儿</button>
          <div class="row">
            <button v-if="thoughtStep > 0" class="ghost" @click="thoughtStep--">上一题</button>
            <button class="primary" @click="nextThought">
              {{ thoughtStep === thoughtSteps.length - 1 ? '看看结果' : '下一题' }}
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 思维记录结果 ============ -->
    <template v-else-if="view === 'thoughtResult'">
      <div class="card stage">
        <div class="stage-head">
          <span class="muted">对照一下</span>
        </div>

        <div class="compare">
          <div class="cmp">
            <div class="cmp-label">当时的想法</div>
            <div class="cmp-text old">{{ thought.thought || '（没写）' }}</div>
          </div>
          <div class="cmp-arrow">↓</div>
          <div class="cmp">
            <div class="cmp-label">换个说法</div>
            <div class="cmp-text new">{{ thought.reframe || '（还没想到新的说法，也没关系）' }}</div>
          </div>
        </div>

        <div v-if="thought.for || thought.against" class="evidence">
          <div class="ev-row">
            <span class="ev-tag ok">支持</span>
            <span>{{ thought.for || '—' }}</span>
          </div>
          <div class="ev-row">
            <span class="ev-tag no">反对</span>
            <span>{{ thought.against || '—' }}</span>
          </div>
        </div>

        <div class="stage-foot">
          <button class="ghost" @click="discard">不保存</button>
          <button class="primary" @click="saveThought">保存这次记录</button>
        </div>
      </div>
    </template>

    <!-- ============ 身体扫描 ============ -->
    <template v-else-if="view === 'body'">
      <div class="card stage">
        <div class="stage-head">
          <span class="muted">身体扫描</span>
          <span class="muted">{{ bodyStep + 1 }} / {{ bodyParts.length }}</span>
        </div>

        <div class="bar"><span :style="{ width: ((bodyStep + 1) / bodyParts.length) * 100 + '%' }" /></div>

        <div class="body-area">
          <div class="body-part">{{ bodyParts[bodyStep].name }}</div>
          <div class="body-text">{{ bodyParts[bodyStep].text }}</div>
        </div>

        <div class="stage-foot">
          <button class="ghost" @click="stopBody">停下来</button>
          <button class="primary" @click="nextBody">
            {{ bodyStep === bodyParts.length - 1 ? '完成了' : '下一段' }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const view = ref('list')
const types = ref([])
const recent = ref([])
const summary = ref({ totalCount: 0, totalMinutes: 0, days: 30 })
const startedAt = ref(null)

const glyph = (t) => ({ BREATHING: '○', THOUGHT_RECORD: '✎', BODY_SCAN: '⌇' }[t] || '·')
const nameOf = (t) => ({ BREATHING: '4-7-8 呼吸', THOUGHT_RECORD: '思维记录', BODY_SCAN: '身体扫描' }[t] || t)

const fmt = (s) => (s ? String(s).replace('T', ' ').slice(5, 16) : '')

const extraOf = (r) => {
  if (r.type === 'THOUGHT_RECORD') {
    try {
      const o = JSON.parse(r.content || '{}')
      return o.reframe ? '「' + o.reframe.slice(0, 24) + '」' : '已记录'
    } catch { return '已记录' }
  }
  return r.duration ? r.duration + ' 秒' : ''
}

// ---------------- 呼吸 ----------------
const phases = [
  { name: '吸气', seconds: 4, scale: 1.5 },
  { name: '屏息', seconds: 7, scale: 1.5 },
  { name: '呼气', seconds: 8, scale: 1 }
]
const totalRounds = 4
const round = ref(1)
const phaseIdx = ref(0)
const remain = ref(phases[0].seconds)
const scale = ref(1)
const transMs = ref(0)
let timer = null

const phaseName = computed(() => phases[phaseIdx.value].name)

const start = (t) => {
  startedAt.value = Date.now()
  if (t.type === 'BREATHING') { enterBreathing() }
  else if (t.type === 'THOUGHT_RECORD') { enterThought() }
  else { enterBody() }
}

const enterBreathing = () => {
  view.value = 'breathing'
  round.value = 1
  phaseIdx.value = 0
  remain.value = phases[0].seconds
  scale.value = 1
  transMs.value = 0
  // 让第一帧的 scale(1) 生效，再放大，才有动画
  setTimeout(() => {
    scale.value = phases[0].scale
    transMs.value = phases[0].seconds * 1000
    runTimer()
  }, 60)
}

const runTimer = () => {
  clearInterval(timer)
  timer = setInterval(() => {
    remain.value--
    if (remain.value > 0) return
    // 进入下一阶段
    phaseIdx.value++
    if (phaseIdx.value >= phases.length) {
      phaseIdx.value = 0
      round.value++
      if (round.value > totalRounds) {
        finishBreathing()
        return
      }
    }
    const p = phases[phaseIdx.value]
    remain.value = p.seconds
    scale.value = p.scale
    transMs.value = p.seconds * 1000
  }, 1000)
}

const finishBreathing = async () => {
  clearInterval(timer)
  const secs = Math.round((Date.now() - startedAt.value) / 1000)
  await save('BREATHING', secs, null)
  ElMessage({ message: '做完了，慢慢来', type: 'success', duration: 1800 })
  backToList()
}

const stopBreathing = async () => {
  clearInterval(timer)
  const secs = Math.round((Date.now() - startedAt.value) / 1000)
  if (secs >= 5) await save('BREATHING', secs, null)
  backToList()
}

// ---------------- 思维记录 ----------------
const thoughtSteps = [
  { key: 'situation', q: '发生了什么？', hint: '只写事实，像摄像头拍到的那样。', ph: '例：开会时领导说这个方案不行' },
  { key: 'thought', q: '当时脑子里冒出的话是什么？', hint: '原话写下来，哪怕很难听。', ph: '例：我真是一点用都没有' },
  { key: 'for', q: '有哪些事实支持这句话？', hint: '只写能被别人看到的，不写感觉。', ph: '' },
  { key: 'against', q: '有哪些事实不支持这句话？', hint: '想想过去、想想别的方面。', ph: '' },
  { key: 'reframe', q: '换个说法，会是什么？', hint: '不是强行乐观，是更贴近事实。', ph: '例：这个方案没过，但我上周的东西是被采纳的' }
]
const thoughtStep = ref(0)
const thought = reactive({ situation: '', thought: '', for: '', against: '', reframe: '' })

const enterThought = () => {
  view.value = 'thought'
  thoughtStep.value = 0
  Object.keys(thought).forEach((k) => { thought[k] = '' })
}

const nextThought = () => {
  if (thoughtStep.value < thoughtSteps.length - 1) {
    thoughtStep.value++
  } else {
    view.value = 'thoughtResult'
  }
}

const saveThought = async () => {
  const filled = Object.values(thought).some((v) => v && v.trim())
  if (!filled) {
    ElMessage.warning('还没写内容')
    return
  }
  await save('THOUGHT_RECORD', 0, JSON.stringify(thought))
  ElMessage({ message: '记下了', type: 'success', duration: 1600 })
  backToList()
}

const finishThought = async () => {
  const filled = Object.values(thought).some((v) => v && v.trim())
  if (filled) {
    await save('THOUGHT_RECORD', 0, JSON.stringify(thought))
    ElMessage({ message: '写多少算多少，已记下', type: 'success', duration: 1600 })
  }
  backToList()
}

const discard = () => backToList()

// ---------------- 身体扫描 ----------------
const bodyParts = [
  { name: '脚趾与脚掌', text: '把注意力放到脚上。不用动，只是注意到——是热是凉，有没有压着什么。' },
  { name: '小腿与膝盖', text: '慢慢往上，到小腿和膝盖。有没有紧绷的地方？不用改变它，知道它在就好。' },
  { name: '大腿与臀部', text: '注意力来到大腿和坐着的地方。感受身体被托住的重量。' },
  { name: '腹部', text: '到肚子了。留意呼吸时这里的起伏，不用刻意深呼吸。' },
  { name: '胸口与心跳', text: '胸口。如果感到闷或紧，就让它在那儿，不赶它走。' },
  { name: '肩膀与脖子', text: '肩膀是不是提着？可以的话，让它们往下沉一点点。' },
  { name: '下巴与脸', text: '下巴、眼睛、额头。这些地方常常一直用力，试着松开一点。' },
  { name: '整个身体', text: '最后，把整个身体一起感受到。它一直在这儿陪着你。' }
]
const bodyStep = ref(0)

const enterBody = () => {
  view.value = 'body'
  bodyStep.value = 0
}

const nextBody = async () => {
  if (bodyStep.value < bodyParts.length - 1) {
    bodyStep.value++
  } else {
    const secs = Math.round((Date.now() - startedAt.value) / 1000)
    await save('BODY_SCAN', secs, null)
    ElMessage({ message: '扫描完成', type: 'success', duration: 1800 })
    backToList()
  }
}

const stopBody = async () => {
  const secs = Math.round((Date.now() - startedAt.value) / 1000)
  if (secs >= 10) await save('BODY_SCAN', secs, null)
  backToList()
}

// ---------------- 通用 ----------------
const save = async (type, duration, content) => {
  try {
    await api.practiceLog({ type, duration, content })
    await loadData()
  } catch (e) {
    ElMessage.warning('保存失败，不影响你刚做的事')
  }
}

const backToList = () => {
  clearInterval(timer)
  view.value = 'list'
}

const loadData = async () => {
  const [a, b] = await Promise.all([api.practiceRecent(30), api.practiceSummary()])
  recent.value = a.data || []
  summary.value = b.data || { totalCount: 0, totalMinutes: 0, days: 30 }
}

onMounted(async () => {
  const t = await api.practiceTypes()
  types.value = t.data || []
  await loadData()
})

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.practice {
  max-width: 980px;
  margin: 0 auto;
}

.pick {
  cursor: pointer;
  transition: all 0.18s;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.pick:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(40, 60, 50, 0.1);
}

.pick-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: var(--sage-soft);
  color: var(--sage-dark);
}

.pick-icon.BREATHING {
  background: #e8f1f6;
  color: #48748c;
}

.pick-icon.THOUGHT_RECORD {
  background: #f6efe4;
  color: #8a6a3c;
}

.pick-icon.BODY_SCAN {
  background: #eeeaf6;
  color: #6b5f8a;
}

.glyph {
  font-size: 20px;
  line-height: 1;
}

.pick-name {
  font-size: 15px;
  font-weight: 500;
}

.pick-desc {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-3);
  flex: 1;
}

.pick-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  padding-top: 4px;
  border-top: 1px dashed var(--line);
}

.go {
  color: var(--sage-dark);
}

.summary {
  font-size: 13px;
  color: var(--ink-2);
  background: var(--sage-soft);
  border-color: #d7e6dd;
  margin-bottom: 16px;
}

.records {
  display: flex;
  flex-direction: column;
}

.rec {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
}

.rec:last-child {
  border-bottom: none;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #9dc2b0;
  flex: 0 0 7px;
}

.dot.BREATHING { background: #7fa8c0; }
.dot.THOUGHT_RECORD { background: #c8a56f; }
.dot.BODY_SCAN { background: #9b8cbc; }

.rec-name {
  flex: 0 0 auto;
}

.rec-extra {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.rec-time {
  font-size: 12px;
  flex: 0 0 auto;
}

/* ---------- 练习舞台 ---------- */
.stage {
  min-height: 460px;
  display: flex;
  flex-direction: column;
}

.stage-head {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  margin-bottom: 10px;
}

.stage-foot {
  margin-top: auto;
  padding-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.ghost {
  border: 1px solid var(--line);
  background: #fff;
  color: var(--ink-2);
  border-radius: 10px;
  padding: 9px 16px;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}

.ghost:hover {
  border-color: #bcd4c7;
}

.primary {
  border: none;
  background: var(--sage);
  color: #fff;
  border-radius: 10px;
  padding: 9px 18px;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}

.primary:hover {
  background: var(--sage-dark);
}

/* 呼吸 */
.breath-area {
  flex: 1;
  display: grid;
  place-items: center;
  min-height: 300px;
}

.ring-outer {
  width: 220px;
  height: 220px;
  display: grid;
  place-items: center;
}

.ring {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #eaf5ef, #cfe4d9);
  border: 1px solid #bcd7c8;
  display: grid;
  place-items: center;
  transition-property: transform;
  transition-timing-function: cubic-bezier(0.37, 0, 0.63, 1);
  will-change: transform;
}

.phase-name {
  font-size: 15px;
  color: var(--sage-dark);
  letter-spacing: 2px;
}

.phase-count {
  font-size: 30px;
  color: var(--sage-dark);
  font-weight: 300;
  line-height: 1;
}

/* 思维记录 */
.q-title {
  font-size: 17px;
  margin: 14px 0 6px;
}

.q-hint {
  font-size: 12.5px;
  color: var(--ink-3);
  margin-bottom: 14px;
}

.compare {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 16px 0;
}

.cmp-label {
  font-size: 12px;
  color: var(--ink-3);
  margin-bottom: 5px;
}

.cmp-text {
  font-size: 14px;
  line-height: 1.8;
  padding: 13px 15px;
  border-radius: 10px;
}

.cmp-text.old {
  background: #f7f4f0;
  color: #7a6a5c;
}

.cmp-text.new {
  background: var(--sage-soft);
  color: var(--sage-dark);
}

.cmp-arrow {
  text-align: center;
  color: var(--ink-3);
  font-size: 14px;
}

.evidence {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: var(--ink-2);
}

.ev-row {
  display: flex;
  gap: 9px;
  align-items: flex-start;
}

.ev-tag {
  flex: 0 0 auto;
  font-size: 11.5px;
  padding: 2px 7px;
  border-radius: 6px;
}

.ev-tag.ok {
  background: #eef4e9;
  color: #5d7c45;
}

.ev-tag.no {
  background: #f7efec;
  color: #996a58;
}

/* 身体扫描 */
.bar {
  height: 3px;
  background: var(--line);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 26px;
}

.bar span {
  display: block;
  height: 100%;
  background: var(--sage);
  transition: width 0.3s;
}

.body-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
  padding: 10px 6px;
}

.body-part {
  font-size: 20px;
  color: var(--sage-dark);
}

.body-text {
  font-size: 14.5px;
  line-height: 2;
  color: var(--ink-2);
}

@media (max-width: 768px) {
  .stage {
    min-height: 400px;
  }
  .ring {
    width: 150px;
    height: 150px;
  }
  .ring-outer {
    width: 180px;
    height: 180px;
  }
  .q-title {
    font-size: 15.5px;
  }
  .body-part {
    font-size: 17px;
  }
  .body-text {
    font-size: 13.5px;
    line-height: 1.9;
  }
}
</style>
