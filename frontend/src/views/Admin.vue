<template>
  <div>
    <div class="page-head">
      <h1>管理后台</h1>
      <p>平台运营数据与内容审核。</p>
    </div>

    <el-tabs v-model="tab">
      <!-- 仪表盘 -->
      <el-tab-pane label="仪表盘" name="dashboard">
        <div class="grid g4" style="margin-bottom: 16px">
          <div class="card mini"><div class="muted">注册患者</div><div class="v">{{ d.userCount || 0 }}</div></div>
          <div class="card mini"><div class="muted">认证咨询师</div><div class="v">{{ d.doctorCount || 0 }}</div></div>
          <div class="card mini"><div class="muted">测评总数</div><div class="v">{{ d.assessmentCount || 0 }}</div></div>
          <div class="card mini"><div class="muted">累计消息</div><div class="v">{{ d.messageCount || 0 }}</div></div>
        </div>

        <div class="grid g2" style="margin-bottom: 16px">
          <div class="card">
            <div class="card-title">近 7 天测评量</div>
            <div ref="trendEl" class="chart"></div>
          </div>
          <div class="card">
            <div class="card-title">测评分级分布</div>
            <div ref="pieEl" class="chart"></div>
          </div>
        </div>

        <div class="grid g3">
          <div class="card">
            <div class="card-title">待办</div>
            <div class="todo"><span>待审核咨询师</span><b>{{ d.pendingDoctorCount || 0 }}</b></div>
            <div class="todo"><span>待审核资源</span><b>{{ d.pendingResourceCount || 0 }}</b></div>
            <div class="todo"><span>今日新增用户</span><b>{{ d.todayUsers || 0 }}</b></div>
          </div>
          <div class="card" style="grid-column: span 2">
            <div class="card-title">最新注册</div>
            <div class="table-scroll"><el-table :data="d.recentUsers || []" size="small">
              <el-table-column prop="realName" label="姓名" width="100" />
              <el-table-column prop="username" label="账号" width="130" />
              <el-table-column label="角色" width="90">
                <template #default="{ row }">{{ roleLabel(row.role) }}</template>
              </el-table-column>
              <el-table-column label="注册时间">
                <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
              </el-table-column>
            </el-table></div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 用户 -->
      <el-tab-pane label="用户管理" name="users">
        <div class="row between" style="margin-bottom: 14px">
          <div class="row">
            <el-input v-model="userQuery.keyword" placeholder="搜索账号 / 姓名" style="width: 220px" clearable
                      @keyup.enter="loadUsers(1)" @clear="loadUsers(1)" />
            <el-select v-model="userQuery.role" placeholder="全部角色" clearable style="width: 130px" @change="loadUsers(1)">
              <el-option label="患者" value="USER" />
              <el-option label="咨询师" value="DOCTOR" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
            <el-button type="primary" @click="loadUsers(1)">查询</el-button>
          </div>
          <span class="muted" style="font-size: 13px">共 {{ userTotal }} 个账号</span>
        </div>
        <div class="card">
          <div class="table-scroll"><el-table :data="users" style="width: 100%">
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="username" label="账号" width="140" />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">{{ roleLabel(row.role) }}</template>
            </el-table-column>
            <el-table-column label="性别 / 年龄" width="120">
              <template #default="{ row }">{{ row.gender || '—' }} / {{ row.age || '—' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'" effect="light">
                  {{ row.status === 'ACTIVE' ? '正常' : '已停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" width="150">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="right">
              <template #default="{ row }">
                <el-button v-if="row.role !== 'ADMIN'" link :type="row.status === 'ACTIVE' ? 'danger' : 'primary'"
                           @click="toggle(row)">
                  {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table></div>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" layout="total, prev, pager, next"
                         :total="userTotal" :page-size="10" :current-page="userQuery.page" @current-change="loadUsers" />
        </div>
      </el-tab-pane>

      <!-- 医生审核 -->
      <el-tab-pane label="医生审核" name="doctors">
        <div class="row" style="margin-bottom: 14px">
          <el-radio-group v-model="doctorStatus" @change="loadDoctors">
            <el-radio-button value="PENDING">待审核</el-radio-button>
            <el-radio-button value="APPROVED">已通过</el-radio-button>
            <el-radio-button value="REJECTED">未通过</el-radio-button>
            <el-radio-button value="">全部</el-radio-button>
          </el-radio-group>
        </div>
        <div class="card">
          <div class="table-scroll"><el-table :data="doctors" style="width: 100%">
            <el-table-column label="申请人" width="130">
              <template #default="{ row }">{{ row.user?.realName }}<div class="muted" style="font-size: 11px">{{ row.user?.username }}</div></template>
            </el-table-column>
            <el-table-column prop="doctor.hospital" label="机构" min-width="150" show-overflow-tooltip />
            <el-table-column prop="doctor.title" label="职称" width="150" show-overflow-tooltip />
            <el-table-column prop="doctor.specialty" label="擅长" min-width="150" show-overflow-tooltip />
            <el-table-column prop="doctor.years" label="年限" width="70" align="center" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="statusType(row.doctor.status)" effect="light">
                  {{ statusLabel(row.doctor.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="right">
              <template #default="{ row }">
                <template v-if="row.doctor.status === 'PENDING'">
                  <el-button link type="primary" @click="auditDoctor(row, 'APPROVED')">通过</el-button>
                  <el-button link type="danger" @click="auditDoctor(row, 'REJECTED')">拒绝</el-button>
                </template>
                <el-button v-else link type="primary" @click="showDoctor(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table></div>
          <el-empty v-if="doctors.length === 0" description="暂无记录" :image-size="80" />
        </div>
      </el-tab-pane>

      <!-- 量表 -->
      <el-tab-pane label="量表管理" name="assessments">
        <div class="card">
          <div class="table-scroll"><el-table :data="assessments" style="width: 100%">
            <el-table-column prop="code" label="编码" width="90" />
            <el-table-column prop="name" label="名称" min-width="200" />
            <el-table-column prop="questionCount" label="题数" width="80" align="center" />
            <el-table-column prop="thresholds" label="阈值" width="130" />
            <el-table-column prop="levels" label="分级" min-width="200" />
            <el-table-column label="启用" width="80" align="center">
              <template #default="{ row }">
                <el-switch :model-value="row.enabled" @change="toggleAssessment(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openQuestions(row)">题目</el-button>
              </template>
            </el-table-column>
          </el-table></div>
        </div>
      </el-tab-pane>

      <!-- 科普 -->
      <el-tab-pane label="科普管理" name="articles">
        <div class="row between" style="margin-bottom: 14px">
          <span class="muted" style="font-size: 13px">共 {{ articles.length }} 篇文章</span>
          <el-button type="primary" @click="newArticle"><el-icon><Plus /></el-icon> 新建文章</el-button>
        </div>
        <div class="card">
          <div class="table-scroll"><el-table :data="articles" style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="author" label="作者" width="110" />
            <el-table-column prop="viewCount" label="阅读" width="80" align="center" />
            <el-table-column label="操作" width="130" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="editArticle(row)">编辑</el-button>
                <el-button link type="danger" @click="removeArticle(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table></div>
        </div>
      </el-tab-pane>

      <!-- 公告 -->
      <el-tab-pane label="公告管理" name="notices">
        <div class="row between" style="margin-bottom: 14px">
          <span class="muted" style="font-size: 13px">共 {{ notices.length }} 条公告</span>
          <el-button type="primary" @click="newNotice"><el-icon><Plus /></el-icon> 新建公告</el-button>
        </div>
        <div class="card">
          <div class="table-scroll"><el-table :data="notices" style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="220" />
            <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
            <el-table-column label="发布时间" width="150">
              <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="editNotice(row)">编辑</el-button>
                <el-button link type="danger" @click="removeNotice(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table></div>
        </div>
      </el-tab-pane>

      <!-- 资源审核 -->
      <el-tab-pane label="资源审核" name="resources">
        <div class="row" style="margin-bottom: 14px">
          <el-radio-group v-model="resourceStatus" @change="loadResources">
            <el-radio-button value="PENDING">待审核</el-radio-button>
            <el-radio-button value="APPROVED">已通过</el-radio-button>
            <el-radio-button value="REJECTED">已拒绝</el-radio-button>
            <el-radio-button value="">全部</el-radio-button>
          </el-radio-group>
        </div>
        <div class="card">
          <div class="table-scroll"><el-table :data="resources" style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ typeLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="statusType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button link type="primary" @click="auditResource(row, 'APPROVED')">通过</el-button>
                  <el-button link type="danger" @click="auditResource(row, 'REJECTED')">拒绝</el-button>
                </template>
                <el-button v-else link @click="previewResource(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table></div>
          <el-empty v-if="resources.length === 0" description="暂无资源" :image-size="80" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 题目管理 -->
    <el-drawer v-model="questionDrawer" :title="currentAssessment?.name + ' · 题目'" size="620px">
      <div class="row between" style="margin-bottom: 12px">
        <span class="muted" style="font-size: 12.5px">共 {{ questions.length }} 题</span>
        <el-button size="small" type="primary" @click="addQuestion">新增题目</el-button>
      </div>
      <div class="table-scroll"><el-table :data="questions" size="small" max-height="70vh">
        <el-table-column prop="seq" label="#" width="50" />
        <el-table-column prop="content" label="题目" min-width="240" />
        <el-table-column label="反向" width="70" align="center">
          <template #default="{ row }">{{ row.reverse ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="" width="70" align="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeQuestion(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table></div>
    </el-drawer>

    <!-- 文章编辑 -->
    <el-dialog v-model="articleDialog" :title="articleForm.id ? '编辑文章' : '新建文章'" width="700px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="articleForm.title" /></el-form-item>
        <div class="grid g2">
          <el-form-item label="分类"><el-input v-model="articleForm.category" /></el-form-item>
          <el-form-item label="作者"><el-input v-model="articleForm.author" /></el-form-item>
        </div>
        <el-form-item label="摘要"><el-input v-model="articleForm.summary" /></el-form-item>
        <el-form-item label="正文（Markdown）">
          <el-input v-model="articleForm.content" type="textarea" :rows="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="articleDialog = false">取消</el-button>
        <el-button type="primary" @click="saveArticle">保存</el-button>
      </template>
    </el-dialog>

    <!-- 公告编辑 -->
    <el-dialog v-model="noticeDialog" :title="noticeForm.id ? '编辑公告' : '新建公告'" width="560px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="noticeForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="noticeForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveNotice">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resourcePreview" :title="previewItem?.title" width="600px">
      <p style="font-size: 13.5px; line-height: 1.9; white-space: pre-wrap">{{ previewItem?.content }}</p>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { api } from '../api'

const tab = ref('dashboard')
const d = ref({})
const trendEl = ref(null)
const pieEl = ref(null)

const users = ref([])
const userTotal = ref(0)
const userQuery = reactive({ page: 1, keyword: '', role: '' })

const doctors = ref([])
const doctorStatus = ref('PENDING')

const assessments = ref([])
const questionDrawer = ref(false)
const currentAssessment = ref(null)
const questions = ref([])

const articles = ref([])
const articleDialog = ref(false)
const articleForm = reactive({ id: null, title: '', category: '', author: '', summary: '', content: '' })

const notices = ref([])
const noticeDialog = ref(false)
const noticeForm = reactive({ id: null, title: '', content: '' })

const resources = ref([])
const resourceStatus = ref('PENDING')
const resourcePreview = ref(false)
const previewItem = ref(null)

const fmt = (s) => (s ? String(s).replace('T', ' ').slice(0, 16) : '—')
const roleLabel = (r) => ({ USER: '患者', DOCTOR: '咨询师', ADMIN: '管理员' }[r] || r)
const typeLabel = (t) => ({ ARTICLE: '图文', AUDIO: '音频', VIDEO: '视频', EXERCISE: '练习' }[t] || t)
const statusLabel = (s) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', ACTIVE: '正常', DISABLED: '停用' }[s] || s)
const statusType = (s) => (s === 'APPROVED' || s === 'ACTIVE' ? 'success' : s === 'PENDING' ? 'warning' : 'info')

const loadDashboard = async () => {
  const res = await api.dashboard()
  d.value = res.data || {}
  await nextTick()
  renderCharts()
}

const renderCharts = () => {
  if (trendEl.value) {
    const chart = echarts.init(trendEl.value)
    chart.setOption({
      grid: { left: 34, right: 16, top: 22, bottom: 26 },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: d.value.trendDates || [], axisLine: { lineStyle: { color: '#e6e3dc' } }, axisLabel: { color: '#7b8a83', fontSize: 11 } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0efe9' } }, axisLabel: { color: '#7b8a83', fontSize: 11 } },
      series: [{
        type: 'bar',
        data: d.value.trendCounts || [],
        barWidth: '46%',
        itemStyle: { color: '#4f7d6b', borderRadius: [5, 5, 0, 0] }
      }]
    })
    window.addEventListener('resize', () => chart.resize())
  }
  if (pieEl.value) {
    const dist = d.value.levelDist || {}
    const chart = echarts.init(pieEl.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11, color: '#7b8a83' } },
      series: [{
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: Object.entries(dist).map(([name, value]) => ({ name, value })),
        color: ['#4f7d6b', '#8fb49f', '#c9a35e', '#c2704f', '#a14f4f']
      }]
    })
    window.addEventListener('resize', () => chart.resize())
  }
}

const loadUsers = async (page = userQuery.page) => {
  userQuery.page = page
  const res = await api.adminUsers({ page, size: 10, keyword: userQuery.keyword, role: userQuery.role })
  users.value = res.data.records || []
  userTotal.value = res.data.total || 0
}

const toggle = async (row) => {
  await api.toggleUser(row.id)
  ElMessage.success('已更新')
  await loadUsers()
}

const loadDoctors = async () => {
  const res = await api.adminDoctors({ status: doctorStatus.value })
  doctors.value = res.data || []
}

const auditDoctor = async (row, status) => {
  let remark = ''
  if (status === 'REJECTED') {
    try {
      const r = await ElMessageBox.prompt('填写未通过原因', '审核', { inputPlaceholder: '资质材料不完整…' })
      remark = r.value
    } catch (e) {
      return
    }
  }
  await api.auditDoctor(row.doctor.id, { status, remark })
  ElMessage.success('已处理')
  await loadDoctors()
}

const showDoctor = (row) => {
  ElMessageBox.alert(
    `<div style="line-height:1.9;font-size:13px">
      <b>${row.user?.realName}</b><br/>
      机构：${row.doctor.hospital || '—'}<br/>
      职称：${row.doctor.title || '—'}<br/>
      擅长：${row.doctor.specialty || '—'}<br/>
      年限：${row.doctor.years || 0} 年　价格：¥${row.doctor.price || 0}<br/>
      资质编号：${row.doctor.certNo || '—'}<br/>
      简介：${row.doctor.intro || '—'}
    </div>`,
    '咨询师详情',
    { dangerouslyUseHTMLString: true }
  )
}

const loadAssessments = async () => {
  const res = await api.adminAssessments()
  assessments.value = res.data || []
}

const toggleAssessment = async (row) => {
  await api.saveAssessment({ ...row, enabled: !row.enabled })
  await loadAssessments()
}

const openQuestions = async (row) => {
  currentAssessment.value = row
  const res = await api.questions(row.id)
  questions.value = res.data || []
  questionDrawer.value = true
}

const addQuestion = async () => {
  try {
    const r = await ElMessageBox.prompt('题目内容', '新增题目', { inputPlaceholder: '我觉得…' })
    const nextSeq = (questions.value.at(-1)?.seq || 0) + 1
    await api.saveQuestion({ assessmentId: currentAssessment.value.id, seq: nextSeq, content: r.value, reverse: false })
    const res = await api.questions(currentAssessment.value.id)
    questions.value = res.data || []
    ElMessage.success('已添加')
  } catch (e) {
    // cancelled
  }
}

const removeQuestion = async (row) => {
  await api.deleteQuestion(row.id)
  const res = await api.questions(currentAssessment.value.id)
  questions.value = res.data || []
}

const loadArticles = async () => {
  const res = await api.adminArticles()
  articles.value = res.data || []
}

const newArticle = () => {
  Object.assign(articleForm, { id: null, title: '', category: '', author: '平台编辑部', summary: '', content: '' })
  articleDialog.value = true
}

const editArticle = (row) => {
  Object.assign(articleForm, row)
  articleDialog.value = true
}

const saveArticle = async () => {
  if (!articleForm.title) {
    ElMessage.warning('请填写标题')
    return
  }
  await api.saveArticle(articleForm)
  ElMessage.success('已保存')
  articleDialog.value = false
  await loadArticles()
}

const removeArticle = async (row) => {
  try {
    await ElMessageBox.confirm(`删除《${row.title}》？`, '确认', { type: 'warning' })
  } catch (e) {
    return
  }
  await api.deleteArticle(row.id)
  await loadArticles()
}

const loadNotices = async () => {
  const res = await api.adminNotices()
  notices.value = res.data || []
}

const newNotice = () => {
  Object.assign(noticeForm, { id: null, title: '', content: '' })
  noticeDialog.value = true
}

const editNotice = (row) => {
  Object.assign(noticeForm, row)
  noticeDialog.value = true
}

const saveNotice = async () => {
  if (!noticeForm.title) {
    ElMessage.warning('请填写标题')
    return
  }
  await api.saveNotice(noticeForm)
  ElMessage.success('已保存')
  noticeDialog.value = false
  await loadNotices()
}

const removeNotice = async (row) => {
  try {
    await ElMessageBox.confirm('删除这条公告？', '确认', { type: 'warning' })
  } catch (e) {
    return
  }
  await api.deleteNotice(row.id)
  await loadNotices()
}

const loadResources = async () => {
  const res = await api.adminResources({ status: resourceStatus.value })
  resources.value = res.data || []
}

const auditResource = async (row, status) => {
  await api.auditResource(row.id, { status })
  ElMessage.success('已处理')
  await loadResources()
}

const previewResource = (row) => {
  previewItem.value = row
  resourcePreview.value = true
}

onMounted(async () => {
  await loadDashboard()
  await Promise.all([loadUsers(1), loadDoctors(), loadAssessments(), loadArticles(), loadNotices(), loadResources()])
})
</script>

<style scoped>
.mini {
  text-align: center;
}

.mini .v {
  font-size: 27px;
  font-weight: 600;
  margin-top: 6px;
}

.chart {
  height: 240px;
}

.todo {
  display: flex;
  justify-content: space-between;
  padding: 11px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
}

.todo:last-child {
  border-bottom: none;
}

.todo span {
  color: var(--ink-3);
}

.todo b {
  color: var(--sage-dark);
  font-size: 16px;
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
