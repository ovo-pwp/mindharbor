<template>
  <div>
    <div class="page-head row between wrap">
      <div>
        <h1>情绪记录</h1>
        <p>每天花一分钟记录，趋势比单次感受更有信息量。</p>
      </div>
      <el-button type="primary" @click="dialog = true">
        <el-icon><Plus /></el-icon> 记录今天
      </el-button>
    </div>

    <div class="grid g3 stat3" style="margin-bottom: 16px">
      <div class="card mini">
        <div class="muted">记录天数</div>
        <div class="v">{{ trend.count || 0 }}<span>天</span></div>
      </div>
      <div class="card mini">
        <div class="muted">平均情绪</div>
        <div class="v">{{ (trend.avgMood || 0).toFixed(1) }}<span>/5</span></div>
      </div>
      <div class="card mini">
        <div class="muted">平均困扰程度</div>
        <div class="v">{{ (trend.avgSeverity || 0).toFixed(1) }}<span>/5</span></div>
      </div>
    </div>

    <div class="card" style="margin-bottom: 16px">
      <div class="card-title row between">
        <span>近 30 天趋势</span>
        <el-radio-group v-model="days" size="small" @change="load">
          <el-radio-button :value="7">7 天</el-radio-button>
          <el-radio-button :value="30">30 天</el-radio-button>
          <el-radio-button :value="90">90 天</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="chartEl" class="chart"></div>
    </div>

    <div class="card">
      <div class="card-title">记录明细</div>
      <div class="table-scroll"><el-table :data="records" style="width: 100%">
        <el-table-column prop="recordDate" label="日期" width="110" />
        <el-table-column label="情绪" width="110">
          <template #default="{ row }"><span :style="{ color: moodColor(row.mood) }">{{ moodEmoji(row.mood) }} {{ row.mood }}</span></template>
        </el-table-column>
        <el-table-column prop="severity" label="困扰" width="70" align="center" />
        <el-table-column prop="energy" label="精力" width="70" align="center" />
        <el-table-column prop="triggerEvent" label="触发事件" min-width="160" show-overflow-tooltip />
        <el-table-column prop="symptoms" label="躯体感受" min-width="140" show-overflow-tooltip />
        <el-table-column prop="coping" label="应对方式" min-width="130" show-overflow-tooltip />
        <el-table-column label="" width="70" align="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table></div>
      <el-empty v-if="records.length === 0" description="还没有记录，从今天开始吧" :image-size="80" />
    </div>

    <el-dialog v-model="dialog" title="记录今天的情绪" width="560px">
      <el-form label-position="top">
        <el-form-item label="今天整体情绪">
          <el-slider v-model="form.mood" :min="1" :max="5" :step="1" show-stops :marks="moodMarks" />
        </el-form-item>
        <div class="grid g2">
          <el-form-item label="困扰程度">
            <el-slider v-model="form.severity" :min="1" :max="5" :step="1" show-stops />
          </el-form-item>
          <el-form-item label="精力水平">
            <el-slider v-model="form.energy" :min="1" :max="5" :step="1" show-stops />
          </el-form-item>
        </div>
        <el-form-item label="触发事件">
          <el-input v-model="form.triggerEvent" placeholder="今天发生了什么让你有这样的感受" />
        </el-form-item>
        <el-form-item label="躯体感受">
          <el-checkbox-group v-model="form.symptoms">
            <el-checkbox v-for="s in symptomOptions" :key="s" :value="s">{{ s }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="应对方式">
          <el-checkbox-group v-model="form.coping">
            <el-checkbox v-for="c in copingOptions" :key="c" :value="c">{{ c }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="想补充的话">
          <el-input v-model="form.note" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { api } from '../api'

const records = ref([])
const trend = ref({})
const days = ref(30)
const dialog = ref(false)
const saving = ref(false)
const chartEl = ref(null)
let chart = null

const symptomOptions = ['入睡困难', '心慌', '头痛', '胸闷', '食欲下降', '乏力', '注意力涣散']
const copingOptions = ['散步', '运动', '听音乐', '刷手机', '找朋友聊', '写日记', '独处', '喝酒']
const moodMarks = { 1: '很差', 2: '低落', 3: '一般', 4: '还行', 5: '很好' }

const form = reactive({
  mood: 3,
  severity: 3,
  energy: 3,
  triggerEvent: '',
  symptoms: [],
  coping: [],
  note: '',
  recordDate: new Date().toISOString().slice(0, 10)
})

const moodEmoji = (m) => ({ 1: '😞', 2: '😔', 3: '😐', 4: '🙂', 5: '😊' }[m] || '')
const moodColor = (m) => (m >= 4 ? '#4f7d6b' : m === 3 ? '#8a7b4a' : '#b0603f')

const load = async () => {
  const [a, b] = await Promise.all([api.symptomTrend(days.value), api.symptoms(days.value)])
  trend.value = a.data || {}
  records.value = b.data || []
  await nextTick()
  renderChart()
}

const renderChart = () => {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)
  const t = trend.value
  chart.setOption({
    grid: { left: 34, right: 18, top: 30, bottom: 28 },
    tooltip: { trigger: 'axis' },
    legend: { data: ['情绪', '困扰', '精力'], right: 0, top: 0, icon: 'roundRect', itemWidth: 9, itemHeight: 9 },
    xAxis: {
      type: 'category',
      data: (t.dates || []).map((d) => String(d).slice(5)),
      axisLine: { lineStyle: { color: '#e6e3dc' } },
      axisLabel: { color: '#7b8a83', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 5,
      splitLine: { lineStyle: { color: '#f0efe9' } },
      axisLabel: { color: '#7b8a83', fontSize: 11 }
    },
    series: [
      { name: '情绪', type: 'line', smooth: true, data: t.mood || [], lineStyle: { width: 2.4, color: '#4f7d6b' }, itemStyle: { color: '#4f7d6b' }, areaStyle: { color: 'rgba(79,125,107,0.08)' } },
      { name: '困扰', type: 'line', smooth: true, data: t.severity || [], lineStyle: { width: 2, color: '#c2704f' }, itemStyle: { color: '#c2704f' } },
      { name: '精力', type: 'line', smooth: true, data: t.energy || [], lineStyle: { width: 2, color: '#b08a4a', type: 'dashed' }, itemStyle: { color: '#b08a4a' } }
    ]
  })
  window.addEventListener('resize', () => chart?.resize())
}

const save = async () => {
  saving.value = true
  try {
    await api.addSymptom(form)
    ElMessage.success('已记录')
    dialog.value = false
    form.triggerEvent = ''
    form.note = ''
    form.symptoms = []
    form.coping = []
    await load()
  } catch (e) {
    // ignore
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm('删除这条记录？', '确认', { type: 'warning' })
  } catch (e) {
    return
  }
  await api.deleteSymptom(row.id)
  await load()
}

onMounted(load)
</script>

<style scoped>
.mini {
  text-align: center;
}

.mini .v {
  font-size: 28px;
  font-weight: 600;
  margin-top: 6px;
}

.mini .v span {
  font-size: 12px;
  color: var(--ink-3);
  margin-left: 3px;
  font-weight: 400;
}

.chart {
  height: 260px;
  width: 100%;
}

@media (max-width: 768px) {
  .stat3 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }
  .stat3 .card {
    padding: 12px 6px;
  }
  .stat3 .mini .v {
    font-size: 19px;
  }
  .stat3 .muted {
    font-size: 11.5px;
  }
  .chart {
    height: 200px;
  }
}

@media (max-width: 768px) {
  .table-scroll {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    margin: 0 -4px;
    padding: 0 4px;
  }
  .table-scroll :deep(.el-table) {
    min-width: 680px;
  }
}
</style>
