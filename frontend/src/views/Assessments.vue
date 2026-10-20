<template>
  <div>
    <div class="page-head">
      <h1>心理测评</h1>
      <p>标准化自评量表，约 3-5 分钟。结果仅供参考，不构成医学诊断。</p>
    </div>

    <div class="grid g2">
      <div v-for="a in list" :key="a.id" class="card scale-card">
        <div class="head">
          <div class="badge">{{ a.code }}</div>
          <div>
            <div class="name">{{ a.name }}</div>
            <div class="muted meta">{{ a.questionCount }} 题 · 最近一周</div>
          </div>
        </div>
        <p class="desc muted">{{ a.description }}</p>
        <div class="row between">
          <div class="levels">
            <span v-for="l in a.levels.split(',')" :key="l">{{ l }}</span>
          </div>
          <el-button type="primary" @click="$router.push(`/assessments/${a.id}`)">开始测评</el-button>
        </div>
      </div>
    </div>

    <div class="card" style="margin-top: 22px">
      <div class="card-title">历史记录</div>
      <el-table v-if="records.length" :data="records" style="width: 100%">
        <el-table-column prop="assessmentName" label="量表" min-width="160" />
        <el-table-column prop="rawScore" label="粗分" width="80" align="center" />
        <el-table-column prop="standardScore" label="标准分" width="90" align="center" />
        <el-table-column label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="tagType(row.level)" effect="light">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="" width="100" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/assessments/result/${row.id}`)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="muted" style="font-size: 13px; padding: 10px 0">还没有测评记录</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const list = ref([])
const records = ref([])

const fmt = (s) => (s ? String(s).replace('T', ' ').slice(0, 16) : '')
const tagType = (level) => {
  if (!level) return 'info'
  if (level.includes('正常')) return 'success'
  if (level.includes('轻')) return 'warning'
  return 'danger'
}

onMounted(async () => {
  const [a, b] = await Promise.all([api.assessments(), api.myAssessmentRecords()])
  list.value = a.data || []
  records.value = b.data || []
})
</script>

<style scoped>
.scale-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.badge {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: var(--sage-soft);
  color: var(--sage-dark);
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.5px;
}

.name {
  font-size: 15.5px;
  font-weight: 600;
}

.meta {
  font-size: 12px;
  margin-top: 3px;
}

.desc {
  font-size: 13px;
  line-height: 1.7;
  margin: 0;
  flex: 1;
}

.levels {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.levels span {
  font-size: 11.5px;
  padding: 2px 8px;
  border-radius: 5px;
  background: #f2f1ec;
  color: var(--ink-3);
}
</style>
