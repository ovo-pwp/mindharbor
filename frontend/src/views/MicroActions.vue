<template>
  <div class="micro">
    <div class="page-head">
      <h1>今天的小事</h1>
      <p>有多少力气，就做多少事。做不了也没关系。</p>
    </div>

    <div class="card level-card">
      <div class="q">今天有多少力气？</div>
      <div class="levels">
        <button
          v-for="l in levels"
          :key="l.level"
          class="lv"
          :class="{ on: l.level === level }"
          @click="pick(l.level)"
        >
          <span class="num">{{ l.level }}</span>
          <span class="lb">{{ l.label }}</span>
        </button>
      </div>
    </div>

    <div v-if="level !== null" class="card items-card">
      <div class="hint-line">{{ current.hint }}</div>
      <div class="items">
        <button
          v-for="it in current.items"
          :key="it"
          class="item"
          :class="{ done: done.includes(it) }"
          @click="record(it)"
        >
          <span class="txt">{{ it }}</span>
          <span v-if="done.includes(it)" class="tick">已记下</span>
        </button>
      </div>
      <div class="foot-note">选低一档不算失败。做不了也没关系，明天还有。</div>
    </div>

    <div class="card">
      <div class="card-title">最近做过的事</div>
      <div v-if="recent.length" class="timeline">
        <div v-for="r in recent" :key="r.id" class="tl-item">
          <span class="dot" />
          <span class="date">{{ String(r.actionDate).slice(5) }}</span>
          <span class="txt">{{ r.content }}</span>
        </div>
      </div>
      <div v-else class="empty muted">还没有记录，随便挑一件试试。</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const levels = ref([])
const level = ref(null)
const current = ref({ items: [], hint: '' })
const done = ref([])
const recent = ref([])

const pick = async (l) => {
  level.value = l
  const res = await api.microOptions(l)
  current.value = res.data
}

const record = async (content) => {
  if (done.value.includes(content)) return
  await api.microLog({ level: level.value, content })
  done.value = [...done.value, content]
  ElMessage({ message: '记下了', type: 'success', duration: 1400, offset: 60 })
  await loadRecent()
}

const loadRecent = async () => {
  const res = await api.microRecent(14)
  recent.value = res.data || []
}

onMounted(async () => {
  const [a, b, c] = await Promise.all([api.microLevels(), api.microToday(), api.microRecent(14)])
  levels.value = a.data || []
  done.value = b.data || []
  recent.value = c.data || []
})
</script>

<style scoped>
.micro {
  max-width: 860px;
  margin: 0 auto;
}

.level-card {
  margin-bottom: 16px;
}

.q {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 14px;
}

.levels {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}

.lv {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 13px 6px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.16s;
  font-family: inherit;
}

.lv:hover {
  border-color: #bcd4c7;
  background: #fbfdfb;
}

.lv.on {
  border-color: var(--sage);
  background: var(--sage-soft);
  box-shadow: inset 0 0 0 1px var(--sage);
}

.num {
  font-size: 20px;
  font-weight: 500;
  color: var(--sage-dark);
  line-height: 1;
}

.lb {
  font-size: 11.5px;
  color: var(--ink-3);
  text-align: center;
  line-height: 1.3;
}

.lv.on .lb {
  color: var(--sage-dark);
}

.items-card {
  margin-bottom: 16px;
}

.hint-line {
  font-size: 13px;
  color: var(--sage-dark);
  background: var(--sage-soft);
  border-radius: 8px;
  padding: 9px 13px;
  margin-bottom: 15px;
}

.items {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 15px 17px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.16s;
  font-family: inherit;
}

.item:hover {
  border-color: #bcd4c7;
  background: #fbfdfb;
}

.item .txt {
  font-size: 14px;
  color: var(--ink);
}

.item.done {
  border-color: #cfe3d8;
  background: #f4faf6;
  cursor: default;
}

.item.done .txt {
  color: var(--ink-3);
  text-decoration: line-through;
}

.tick {
  font-size: 11.5px;
  color: var(--sage);
  flex: 0 0 auto;
}

.foot-note {
  margin-top: 15px;
  font-size: 12.5px;
  color: var(--ink-3);
  text-align: center;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tl-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
}

.tl-item:last-child {
  border-bottom: none;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #b9d2c5;
  flex: 0 0 6px;
}

.date {
  color: var(--ink-3);
  font-size: 12px;
  flex: 0 0 42px;
}

.tl-item .txt {
  color: var(--ink-2);
}

.empty {
  font-size: 13px;
  padding: 12px 0;
}

@media (max-width: 760px) {
  .levels {
    grid-template-columns: repeat(3, 1fr);
  }
  .items {
    grid-template-columns: 1fr;
  }
}
</style>
