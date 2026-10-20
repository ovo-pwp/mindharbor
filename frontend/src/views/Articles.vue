<template>
  <div>
    <div class="page-head">
      <h1>心理科普</h1>
      <p>由平台咨询师撰写的心理学知识，帮你理解自己的反应。</p>
    </div>

    <div class="row wrap" style="margin-bottom: 18px; gap: 8px">
      <el-check-tag :checked="category === ''" @change="pick('')">全部</el-check-tag>
      <el-check-tag v-for="c in categories" :key="c" :checked="category === c" @change="pick(c)">{{ c }}</el-check-tag>
    </div>

    <div class="grid g2">
      <div v-for="a in records" :key="a.id" class="card art" @click="$router.push(`/articles/${a.id}`)">
        <div class="head">
          <el-tag size="small" effect="plain" type="info">{{ a.category }}</el-tag>
          <span class="muted views">{{ a.viewCount }} 次阅读</span>
        </div>
        <h3>{{ a.title }}</h3>
        <p class="summary muted">{{ a.summary }}</p>
        <div class="foot">
          <span class="author">{{ a.author }}</span>
          <span class="muted">{{ fmt(a.createdAt) }}</span>
        </div>
      </div>
    </div>

    <el-empty v-if="records.length === 0" description="暂无文章" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const records = ref([])
const category = ref('')
const categories = ['情绪管理', '亲密关系', '睡眠', '学业压力', '职场压力', '人际关系', '自我成长']

const fmt = (s) => (s ? String(s).slice(0, 10) : '')

const load = async () => {
  const res = await api.articles({ page: 1, size: 30, category: category.value })
  records.value = res.data.records || []
}

const pick = (c) => {
  category.value = c
  load()
}

onMounted(load)
</script>

<style scoped>
.art {
  cursor: pointer;
  transition: all 0.16s;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.art:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(30, 43, 38, 0.09);
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.views {
  font-size: 11.5px;
}

h3 {
  font-size: 16px;
  margin: 0;
  line-height: 1.5;
  letter-spacing: -0.2px;
}

.summary {
  font-size: 13px;
  line-height: 1.7;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.foot {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
}

.author {
  color: var(--sage-dark);
}
</style>
