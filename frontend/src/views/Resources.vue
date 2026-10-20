<template>
  <div>
    <div class="page-head">
      <h1>心理资源</h1>
      <p>放松音频、练习工具与自助卡片，随时取用。</p>
    </div>

    <div ref="rec" v-if="recommend.length" class="rec-strip">
      <div class="rec-head">
        <span><el-icon><MagicStick /></el-icon> 根据你的浏览记录推荐</span>
      </div>
      <div class="rec-list">
        <div v-for="r in recommend" :key="r.id" class="rec-card" @click="open(r)">
          <div class="rc-title">{{ r.title }}</div>
          <div class="rc-meta">{{ typeLabel(r.type) }} · {{ r.category }}</div>
        </div>
      </div>
    </div>

    <div class="row wrap" style="margin: 18px 0 16px; gap: 8px">
      <el-radio-group v-model="type" size="small" @change="load(1)">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="AUDIO">音频</el-radio-button>
        <el-radio-button value="EXERCISE">练习</el-radio-button>
        <el-radio-button value="ARTICLE">图文</el-radio-button>
      </el-radio-group>
      <el-select v-model="category" placeholder="全部分类" clearable size="small" style="width: 150px" @change="load(1)">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
    </div>

    <div class="grid g3">
      <div v-for="r in records" :key="r.id" class="card res" @click="open(r)">
        <div class="icon" :class="r.type">
          <el-icon :size="20"><component :is="iconOf(r.type)" /></el-icon>
        </div>
        <div class="body">
          <div class="title">{{ r.title }}</div>
          <div class="meta muted">
            <el-tag size="small" effect="plain">{{ typeLabel(r.type) }}</el-tag>
            <span>{{ r.category }}</span>
            <span>· {{ r.viewCount }} 次</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="records.length === 0" description="暂无资源" />

    <el-dialog v-model="dialog" :title="current?.title" width="620px">
      <div v-if="current" class="detail-body">
        <div class="row" style="gap: 8px; margin-bottom: 14px">
          <el-tag size="small" effect="plain">{{ typeLabel(current.type) }}</el-tag>
          <el-tag size="small" type="info" effect="plain">{{ current.category }}</el-tag>
        </div>
        <div class="markdown-body" v-html="render(current.content)"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { marked } from 'marked'
import { api } from '../api'

const records = ref([])
const recommend = ref([])
const type = ref('')
const category = ref('')
const categories = ['放松训练', '正念', '情绪管理', '睡眠', 'CBT', '自我觉察', '自我成长']
const dialog = ref(false)
const current = ref(null)

const typeLabel = (t) => ({ ARTICLE: '图文', AUDIO: '音频', VIDEO: '视频', EXERCISE: '练习' }[t] || t)
const iconOf = (t) => ({ AUDIO: 'Headset', EXERCISE: 'EditPen', VIDEO: 'VideoPlay', ARTICLE: 'Document' }[t] || 'Document')
const render = (t) => marked.parse((t || '').replace(/\n/g, '  \n'))

const load = async (page = 1) => {
  const res = await api.resources({ page, size: 24, type: type.value, category: category.value })
  records.value = res.data.records || []
}

const open = async (r) => {
  const res = await api.resource(r.id)
  current.value = res.data
  dialog.value = true
  r.viewCount = (r.viewCount || 0) + 1
}

onMounted(async () => {
  await load()
  try {
    const res = await api.recommend(3)
    recommend.value = res.data || []
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.rec-strip {
  background: linear-gradient(110deg, #eef5f0, #f7f6f2);
  border: 1px solid #e2ebe5;
  border-radius: var(--radius);
  padding: 16px 18px;
}

.rec-head {
  font-size: 12.5px;
  color: var(--sage-dark);
  margin-bottom: 11px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.rec-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.rec-card {
  flex: 1;
  min-width: 200px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 11px 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.rec-card:hover {
  border-color: var(--sage);
}

.rc-title {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 4px;
}

.rc-meta {
  font-size: 11.5px;
  color: var(--ink-3);
}

.res {
  display: flex;
  gap: 13px;
  cursor: pointer;
  transition: all 0.16s;
  align-items: center;
}

.res:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(30, 43, 38, 0.09);
}

.icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  flex: 0 0 42px;
  background: var(--sage-soft);
  color: var(--sage-dark);
}

.icon.AUDIO {
  background: #eaf0f7;
  color: #4a6b8f;
}

.icon.EXERCISE {
  background: #f7f0e7;
  color: #a4763f;
}

.icon.VIDEO {
  background: #f7eaea;
  color: #a04f4f;
}

.title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 7px;
  line-height: 1.5;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
}

.detail-body {
  max-height: 60vh;
  overflow-y: auto;
}
</style>
