<template>
  <div>
    <div class="page-head">
      <h1>医生工作台</h1>
      <p>处理患者申请、查看档案、撰写评估报告。</p>
    </div>

    <el-tabs v-model="tab" class="tabs">
      <el-tab-pane name="apply">
        <template #label>待审申请 <el-badge v-if="applications.length" :value="applications.length" class="tab-badge" /></template>
        <div class="card">
          <div class="table-scroll"><el-table v-if="applications.length" :data="applications" style="width: 100%">
            <el-table-column label="患者" width="150">
              <template #default="{ row }">
                <div class="row" style="gap: 8px">
                  <div class="avatar sm">{{ row.initial }}</div>{{ row.name }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="applyReason" label="申请理由" min-width="240" show-overflow-tooltip />
            <el-table-column label="申请时间" width="140">
              <template #default="{ row }">{{ fmt(row.appliedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="audit(row, 'APPROVED')">通过</el-button>
                <el-button link type="danger" @click="audit(row, 'REJECTED')">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table></div>
          <el-empty v-if="applications.length === 0" description="暂无待处理申请" :image-size="80" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的患者" name="patients">
        <div class="grid g3">
          <div v-for="p in patients" :key="p.userId" class="card patient" @click="openPatient(p)">
            <div class="avatar">{{ p.initial }}</div>
            <div class="p-name">{{ p.name }}</div>
            <div class="muted p-meta">{{ p.gender || '—' }} · {{ p.age || '—' }} 岁</div>
            <div class="row" style="margin-top: 12px; gap: 8px">
              <el-button size="small" @click.stop="openChat(p)">发消息</el-button>
              <el-button size="small" type="primary" plain @click.stop="openPatient(p)">查看档案</el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="patients.length === 0" description="还没有已绑定的患者" />
      </el-tab-pane>

      <el-tab-pane label="评估报告" name="reports">
        <div class="row between" style="margin-bottom: 14px">
          <span class="muted" style="font-size: 13px">共 {{ reports.length }} 份报告</span>
          <el-button type="primary" @click="newReport">
            <el-icon><Plus /></el-icon> 新建报告
          </el-button>
        </div>
        <div class="card">
          <div class="table-scroll"><el-table v-if="reports.length" :data="reports" style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'PUBLISHED' ? 'success' : 'info'" effect="light">
                  {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="150">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="170" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="editReport(row)">查看/编辑</el-button>
                <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="publish(row)">发布</el-button>
              </template>
            </el-table-column>
          </el-table></div>
          <el-empty v-if="reports.length === 0" description="还没有报告" :image-size="80" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的资源" name="resources">
        <div class="row between" style="margin-bottom: 14px">
          <span class="muted" style="font-size: 13px">发布后需平台审核，通过后对患者可见</span>
          <el-button type="primary" @click="resourceDialog = true">
            <el-icon><Plus /></el-icon> 发布资源
          </el-button>
        </div>
        <div class="card">
          <div class="table-scroll"><el-table v-if="myResources.length" :data="myResources" style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ typeLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'APPROVED' ? 'success' : row.status === 'PENDING' ? 'warning' : 'info'" effect="light">
                  {{ row.status === 'APPROVED' ? '已通过' : row.status === 'PENDING' ? '待审核' : '已拒绝' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="80" align="center" />
          </el-table></div>
          <el-empty v-if="myResources.length === 0" description="还没有发布资源" :image-size="80" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 患者档案 -->
    <el-drawer v-model="patientDrawer" :title="patient?.name + ' · 档案'" size="640px">
      <div v-if="overview">
        <div class="grid g3" style="margin-bottom: 16px">
          <div class="card mini">
            <div class="muted">情绪记录</div>
            <div class="v">{{ overview.symptoms?.length || 0 }}</div>
          </div>
          <div class="card mini">
            <div class="muted">测评次数</div>
            <div class="v">{{ overview.assessments?.length || 0 }}</div>
          </div>
          <div class="card mini">
            <div class="muted">平均困扰</div>
            <div class="v">{{ avgSeverity }}</div>
          </div>
        </div>

        <div class="card" style="margin-bottom: 16px">
          <div class="card-title">近期情绪记录</div>
          <div class="table-scroll"><el-table :data="overview.symptoms" size="small" max-height="260">
            <el-table-column prop="recordDate" label="日期" width="105" />
            <el-table-column prop="mood" label="情绪" width="60" align="center" />
            <el-table-column prop="severity" label="困扰" width="60" align="center" />
            <el-table-column prop="energy" label="精力" width="60" align="center" />
            <el-table-column prop="triggerEvent" label="触发事件" min-width="150" show-overflow-tooltip />
          </el-table></div>
        </div>

        <div class="card" style="margin-bottom: 16px">
          <div class="card-title">测评历史</div>
          <div class="table-scroll"><el-table :data="overview.assessments" size="small">
            <el-table-column prop="assessmentName" label="量表" min-width="150" />
            <el-table-column prop="standardScore" label="标准分" width="80" align="center" />
            <el-table-column prop="level" label="结论" width="110" />
            <el-table-column label="时间" width="120">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
          </el-table></div>
        </div>

        <el-button type="primary" style="width: 100%" @click="draftFromPatient">
          <el-icon><MagicStick /></el-icon> AI 生成阶段性评估报告
        </el-button>
      </div>
    </el-drawer>

    <!-- 报告编辑 -->
    <el-dialog v-model="reportDialog" title="评估报告" width="720px" :close-on-click-modal="false">
      <el-form label-position="top">
        <div class="grid g2">
          <el-form-item label="报告标题">
            <el-input v-model="reportForm.title" />
          </el-form-item>
          <el-form-item label="对应患者">
            <el-select v-model="reportForm.userId" placeholder="选择患者" style="width: 100%" :disabled="!!reportForm.id">
              <el-option v-for="p in patients" :key="p.userId" :label="p.name" :value="p.userId" />
            </el-select>
          </el-form-item>
        </div>

        <div class="ai-bar">
          <span class="muted" style="font-size: 12.5px">AI 可基于患者近 30 天记录生成四段式报告草稿，生成后请务必人工复核。</span>
          <el-button size="small" :loading="aiLoading" :disabled="!reportForm.userId" @click="aiGenerate">
            <el-icon><MagicStick /></el-icon> AI 生成
          </el-button>
        </div>

        <el-form-item label="总体概述">
          <el-input v-model="reportForm.overview" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="数据洞察">
          <el-input v-model="reportForm.insight" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="风险预警">
          <el-input v-model="reportForm.riskWarning" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="改善建议">
          <el-input v-model="reportForm.suggestion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button :loading="saving" @click="saveReport(false)">保存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="saveReport(true)">保存并发布</el-button>
      </template>
    </el-dialog>

    <!-- 发布资源 -->
    <el-dialog v-model="resourceDialog" title="发布心理资源" width="600px">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="resourceForm.title" />
        </el-form-item>
        <div class="grid g2">
          <el-form-item label="类型">
            <el-select v-model="resourceForm.type" style="width: 100%">
              <el-option label="图文" value="ARTICLE" />
              <el-option label="音频" value="AUDIO" />
              <el-option label="视频" value="VIDEO" />
              <el-option label="练习" value="EXERCISE" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类">
            <el-input v-model="resourceForm.category" placeholder="如：放松训练" />
          </el-form-item>
        </div>
        <el-form-item label="内容">
          <el-input v-model="resourceForm.content" type="textarea" :rows="6" placeholder="支持 Markdown" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resourceDialog = false">取消</el-button>
        <el-button type="primary" @click="submitResource">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, http } from '../api'

const router = useRouter()
const tab = ref('patients')
const applications = ref([])
const patients = ref([])
const reports = ref([])
const myResources = ref([])

const patientDrawer = ref(false)
const patient = ref(null)
const overview = ref(null)

const reportDialog = ref(false)
const reportForm = reactive({ id: null, userId: null, title: '', overview: '', insight: '', riskWarning: '', suggestion: '' })
const aiLoading = ref(false)
const saving = ref(false)

const resourceDialog = ref(false)
const resourceForm = reactive({ title: '', type: 'ARTICLE', category: '', content: '' })

const fmt = (s) => (s ? String(s).replace('T', ' ').slice(0, 16) : '')
const typeLabel = (t) => ({ ARTICLE: '图文', AUDIO: '音频', VIDEO: '视频', EXERCISE: '练习' }[t] || t)

const avgSeverity = computed(() => {
  const list = overview.value?.symptoms || []
  if (!list.length) return '—'
  return (list.reduce((s, x) => s + x.severity, 0) / list.length).toFixed(1)
})

const loadAll = async () => {
  const [a, b, c, d] = await Promise.all([
    api.applications(),
    api.patients(),
    api.reports(),
    api.myResources()
  ])
  applications.value = a.data || []
  patients.value = b.data || []
  reports.value = c.data || []
  myResources.value = d.data || []
}

const audit = async (row, status) => {
  let remark = ''
  if (status === 'REJECTED') {
    try {
      const r = await ElMessageBox.prompt('填写拒绝原因（患者可见）', '拒绝申请', {
        inputPlaceholder: '例如：建议先到医院做一次评估'
      })
      remark = r.value
    } catch (e) {
      return
    }
  }
  await api.auditBind({ id: row.bindId, status, remark })
  ElMessage.success('已处理')
  await loadAll()
}

const openPatient = async (p) => {
  patient.value = p
  patientDrawer.value = true
  const res = await http.get(`/doctor/workbench/patients/${p.userId}/overview`)
  overview.value = res.data
}

const openChat = (p) => {
  router.push('/consult')
}

const newReport = () => {
  Object.assign(reportForm, { id: null, userId: null, title: '', overview: '', insight: '', riskWarning: '', suggestion: '' })
  reportDialog.value = true
}

const editReport = (row) => {
  Object.assign(reportForm, row)
  reportDialog.value = true
}

const draftFromPatient = async () => {
  if (!patient.value) return
  aiLoading.value = true
  try {
    const res = await api.aiDraft({ userId: patient.value.userId, name: patient.value.name })
    Object.assign(reportForm, {
      id: null,
      userId: patient.value.userId,
      title: res.data.title,
      overview: res.data.overview,
      insight: res.data.insight,
      riskWarning: res.data.riskWarning,
      suggestion: res.data.suggestion
    })
    patientDrawer.value = false
    reportDialog.value = true
    ElMessage.success('已生成草稿，请复核后发布')
  } catch (e) {
    // ignore
  } finally {
    aiLoading.value = false
  }
}

const aiGenerate = async () => {
  const p = patients.value.find((x) => x.userId === reportForm.userId)
  if (!p) {
    ElMessage.warning('请先选择患者')
    return
  }
  aiLoading.value = true
  try {
    const res = await api.aiDraft({ userId: p.userId, name: p.name })
    Object.assign(reportForm, {
      title: reportForm.title || res.data.title,
      overview: res.data.overview,
      insight: res.data.insight,
      riskWarning: res.data.riskWarning,
      suggestion: res.data.suggestion
    })
    ElMessage.success('已生成')
  } catch (e) {
    // ignore
  } finally {
    aiLoading.value = false
  }
}

const saveReport = async (publishNow) => {
  if (!reportForm.userId) {
    ElMessage.warning('请选择患者')
    return
  }
  saving.value = true
  try {
    const res = await api.saveReport(reportForm)
    if (publishNow) {
      await api.publishReport(res.data.id)
      ElMessage.success('报告已发布，患者可见')
    } else {
      ElMessage.success('草稿已保存')
    }
    reportDialog.value = false
    await loadAll()
  } catch (e) {
    // ignore
  } finally {
    saving.value = false
  }
}

const publish = async (row) => {
  await api.publishReport(row.id)
  ElMessage.success('已发布')
  await loadAll()
}

const submitResource = async () => {
  if (!resourceForm.title) {
    ElMessage.warning('请填写标题')
    return
  }
  await api.publishResource(resourceForm)
  ElMessage.success('已提交，等待平台审核')
  resourceDialog.value = false
  Object.assign(resourceForm, { title: '', type: 'ARTICLE', category: '', content: '' })
  await loadAll()
}

onMounted(async () => {
  const me = await api.myDoctorApply()
  if (!me.data || me.data.status !== 'APPROVED') {
    ElMessage.warning('医生档案尚未通过审核')
  }
  await loadAll()
})
</script>

<style scoped>
.tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.tab-badge {
  margin-left: 4px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e8f0eb, #d6e4dc);
  color: var(--sage-dark);
  display: grid;
  place-items: center;
  font-weight: 600;
  margin: 0 auto 10px;
}

.avatar.sm {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  font-size: 12px;
  margin: 0;
}

.patient {
  text-align: center;
  cursor: pointer;
  transition: all 0.16s;
}

.patient:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(30, 43, 38, 0.09);
}

.p-name {
  font-size: 14.5px;
  font-weight: 600;
}

.p-meta {
  font-size: 12px;
  margin-top: 4px;
}

.mini {
  text-align: center;
}

.mini .v {
  font-size: 22px;
  font-weight: 600;
  margin-top: 5px;
}

.ai-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--sage-soft);
  border-radius: 9px;
  padding: 9px 13px;
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .table-scroll {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .table-scroll :deep(.el-table) {
    min-width: 680px;
  }
}

@media (max-width: 768px) {
  :deep(.el-tabs__item) {
    font-size: 12.5px;
    padding: 0 10px;
    height: 38px;
    line-height: 38px;
  }
  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }
}
</style>
