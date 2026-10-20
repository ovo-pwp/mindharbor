<template>
  <div v-if="assessment" class="take">
    <div class="head">
      <div>
        <h1>{{ assessment.name }}</h1>
        <p class="muted">{{ assessment.instruction }}</p>
      </div>
      <div class="progress-box">
        <span class="num">{{ answered }}</span><span class="total">/{{ questions.length }}</span>
        <el-progress :percentage="percent" :show-text="false" :stroke-width="5" style="width: 120px" />
      </div>
    </div>

    <div class="options-legend">
      <span v-for="(o, i) in options" :key="i"><b>{{ i + 1 }}</b>{{ o }}</span>
    </div>

    <div class="card">
      <div v-for="(q, i) in questions" :key="q.id" class="q" :class="{ done: answers[i] }">
        <div class="q-text"><span class="seq">{{ q.seq }}</span>{{ q.content }}</div>
        <el-radio-group v-model="answers[i]" size="large">
          <el-radio-button v-for="(o, oi) in options" :key="oi" :value="oi + 1">{{ oi + 1 }}</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="footer">
      <span class="muted">量表结果仅作参考，不能替代专业诊断。</span>
      <el-button type="primary" size="large" :loading="submitting" @click="submit">提交并查看结果</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const route = useRoute()
const router = useRouter()
const assessment = ref(null)
const questions = ref([])
const answers = ref([])
const submitting = ref(false)

const options = ['没有或很少时间', '小部分时间', '相当多时间', '绝大部分时间']

const answered = computed(() => answers.value.filter((v) => v).length)
const percent = computed(() =>
  questions.value.length ? Math.round((answered.value / questions.value.length) * 100) : 0
)

const submit = async () => {
  if (answered.value < questions.value.length) {
    ElMessage.warning(`还有 ${questions.value.length - answered.value} 题未作答`)
    return
  }
  submitting.value = true
  try {
    const res = await api.submitAssessment(route.params.id, answers.value)
    ElMessage.success('测评完成')
    router.push(`/assessments/result/${res.data.id}`)
  } catch (e) {
    // ignore
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const res = await api.assessment(route.params.id)
  assessment.value = res.data.assessment
  questions.value = res.data.questions || []
  answers.value = new Array(questions.value.length).fill(null)
})
</script>

<style scoped>
.take {
  max-width: 880px;
  margin: 0 auto;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 20px;
}

h1 {
  font-size: 22px;
  margin: 0 0 6px;
}

.head p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  max-width: 620px;
}

.progress-box {
  text-align: right;
  flex: 0 0 auto;
}

.num {
  font-size: 22px;
  font-weight: 600;
  color: var(--sage-dark);
}

.total {
  color: var(--ink-3);
  font-size: 13px;
}

.options-legend {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--ink-3);
  margin-bottom: 12px;
  padding-left: 4px;
}

.options-legend b {
  color: var(--sage-dark);
  margin-right: 4px;
}

.q {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 15px 4px;
  border-bottom: 1px dashed var(--line);
}

.q:last-child {
  border-bottom: none;
}

.q-text {
  font-size: 14px;
  line-height: 1.6;
  display: flex;
  gap: 10px;
}

.seq {
  color: var(--ink-3);
  font-size: 12px;
  min-width: 20px;
  padding-top: 2px;
}

.q.done .q-text {
  color: var(--ink);
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  font-size: 12.5px;
}

@media (max-width: 760px) {
  .q {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
