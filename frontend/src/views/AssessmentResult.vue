<template>
  <div v-if="record" class="result">
    <div class="card top">
      <div>
        <div class="muted" style="font-size: 12.5px">{{ record.assessmentName }}</div>
        <h1>{{ record.level }}</h1>
        <p class="muted time">测评时间 {{ fmt(record.createdAt) }}</p>
      </div>
      <div ref="gauge" class="gauge"></div>
    </div>

    <div class="grid g3" style="margin-top: 16px">
      <div class="card mini">
        <div class="muted">粗分</div>
        <div class="v">{{ record.rawScore }}</div>
      </div>
      <div class="card mini">
        <div class="muted">标准分</div>
        <div class="v">{{ record.standardScore }}</div>
      </div>
      <div class="card mini">
        <div class="muted">分级</div>
        <div class="v" style="font-size: 18px">{{ record.level }}</div>
      </div>
    </div>

    <div class="card" style="margin-top: 16px">
      <div class="card-title">结果解读与建议</div>
      <div class="markdown-body" v-html="render(record.suggestion)"></div>
    </div>

    <div class="card warn" style="margin-top: 16px">
      <el-icon><WarningFilled /></el-icon>
      <div>
        本次结果基于自评量表，<b>不能作为医学诊断依据</b>。如果困扰持续两周以上，或影响到睡眠、进食与工作，
        建议预约专业咨询师，或到精神卫生专科就诊。危机情况请拨打 400-161-9995。
      </div>
    </div>

    <div class="row" style="margin-top: 18px; gap: 10px">
      <el-button @click="$router.push('/assessments')">返回测评列表</el-button>
      <el-button type="primary" @click="$router.push('/doctors')">预约咨询师</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import * as echarts from 'echarts'
import { api } from '../api'

const route = useRoute()
const record = ref(null)
const gauge = ref(null)

const fmt = (s) => (s ? String(s).replace('T', ' ').slice(0, 16) : '')
const render = (t) => marked.parse(t || '')

const levelColor = (level) => {
  if (!level) return '#7b8a83'
  if (level.includes('正常')) return '#4f7d6b'
  if (level.includes('轻度')) return '#b08a4a'
  if (level.includes('中度')) return '#c2704f'
  return '#b04a4a'
}

onMounted(async () => {
  const res = await api.assessmentRecord(route.params.id)
  record.value = res.data
  await nextTick()
  if (gauge.value) {
    const chart = echarts.init(gauge.value)
    const color = levelColor(record.value.level)
    chart.setOption({
      series: [
        {
          type: 'gauge',
          startAngle: 210,
          endAngle: -30,
          radius: '96%',
          min: 0,
          max: 100,
          progress: { show: true, width: 14, itemStyle: { color } },
          axisLine: { lineStyle: { width: 14, color: [[1, '#eeece6']] } },
          axisTick: { show: false },
          splitLine: { show: false },
          axisLabel: { show: false },
          pointer: { show: false },
          anchor: { show: false },
          detail: {
            valueAnimation: true,
            offsetCenter: [0, '6%'],
            fontSize: 30,
            fontWeight: 600,
            color,
            formatter: '{value}'
          },
          title: { offsetCenter: [0, '42%'], fontSize: 12, color: '#7b8a83' },
          data: [{ value: Math.min(100, record.value.standardScore), name: '标准分' }]
        }
      ]
    })
    window.addEventListener('resize', () => chart.resize())
  }
})
</script>

<style scoped>
.result {
  max-width: 880px;
  margin: 0 auto;
}

.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

h1 {
  font-size: 30px;
  margin: 8px 0 6px;
  letter-spacing: -0.5px;
}

.time {
  font-size: 12.5px;
  margin: 0;
}

.gauge {
  width: 200px;
  height: 150px;
  flex: 0 0 200px;
}

.mini {
  text-align: center;
}

.mini .v {
  font-size: 26px;
  font-weight: 600;
  margin-top: 6px;
  color: var(--ink);
}

.warn {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  background: #fdf8f2;
  border-color: #f0e2cf;
  color: #7a5a2e;
  font-size: 13px;
  line-height: 1.75;
}

.warn .el-icon {
  color: var(--gold);
  font-size: 17px;
  margin-top: 2px;
  flex: 0 0 auto;
}
</style>
