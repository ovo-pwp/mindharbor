<template>
  <div>
    <div class="page-head">
      <h1>我的报告</h1>
      <p>咨询师为你撰写的阶段性评估报告。</p>
    </div>

    <div class="grid g2">
      <div v-for="r in reports" :key="r.id" class="card rep" @click="open(r)">
        <div class="row between">
          <div class="title">{{ r.title }}</div>
          <el-tag size="small" type="success" effect="light">已发布</el-tag>
        </div>
        <div class="muted meta">{{ fmt(r.publishedAt) }}</div>
        <p class="excerpt">{{ r.overview }}</p>
        <div class="row" style="margin-top: 10px">
          <el-button size="small" type="primary" plain>查看完整报告</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="reports.length === 0" description="还没有收到报告。与咨询师建立关系并持续记录后，医生可以为你生成阶段性报告。" />

    <el-dialog v-model="dialog" :title="current?.title" width="680px">
      <div v-if="current" class="detail">
        <div class="muted meta">发布于 {{ fmt(current.publishedAt) }}</div>
        <section>
          <h4>总体概述</h4>
          <p>{{ current.overview }}</p>
        </section>
        <section>
          <h4>数据洞察</h4>
          <p>{{ current.insight }}</p>
        </section>
        <section class="risk">
          <h4>风险预警</h4>
          <p>{{ current.riskWarning }}</p>
        </section>
        <section>
          <h4>改善建议</h4>
          <p style="white-space: pre-wrap">{{ current.suggestion }}</p>
        </section>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const reports = ref([])
const dialog = ref(false)
const current = ref(null)

const fmt = (s) => (s ? String(s).replace('T', ' ').slice(0, 16) : '')

const open = async (r) => {
  const res = await api.report(r.id)
  current.value = res.data
  dialog.value = true
}

onMounted(async () => {
  const res = await api.myReports()
  reports.value = res.data || []
})
</script>

<style scoped>
.rep {
  cursor: pointer;
  transition: all 0.16s;
}

.rep:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(30, 43, 38, 0.09);
}

.title {
  font-size: 15px;
  font-weight: 600;
}

.meta {
  font-size: 12px;
  margin: 6px 0 10px;
}

.excerpt {
  font-size: 13px;
  line-height: 1.75;
  color: var(--ink-2);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.detail section {
  margin-top: 18px;
}

.detail h4 {
  font-size: 13.5px;
  margin: 0 0 8px;
  color: var(--sage-dark);
  display: flex;
  align-items: center;
  gap: 7px;
}

.detail h4::before {
  content: '';
  width: 3px;
  height: 13px;
  background: var(--sage);
  border-radius: 2px;
}

.detail p {
  font-size: 13.5px;
  line-height: 1.9;
  color: var(--ink-2);
  margin: 0;
}

.risk {
  background: #fdf8f2;
  border: 1px solid #f0e2cf;
  border-radius: 10px;
  padding: 12px 16px;
}

.risk h4 {
  color: #a3732f;
}

.risk h4::before {
  background: var(--gold);
}
</style>
