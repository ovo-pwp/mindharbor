import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 200) {
      if (body.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        if (!location.hash.includes('/login')) {
          location.hash = '#/login'
        }
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      location.hash = '#/login'
    } else {
      ElMessage.error(err.response?.data?.msg || '网络异常，请稍后重试')
    }
    return Promise.reject(err)
  }
)

export default http

export const api = {
  // 公开配置（是否展示演示账号等）
  publicConfig: () => http.get('/public/config'),

  // 认证
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data),
  me: () => http.get('/auth/me'),
  logout: () => http.post('/auth/logout'),

  // 用户
  profile: () => http.get('/user/profile'),
  updateProfile: (data) => http.put('/user/profile', data),
  changePassword: (data) => http.post('/user/password', data),

  // 医生
  doctors: (params) => http.get('/doctors', { params }),
  doctor: (id) => http.get(`/doctors/${id}`),
  applyDoctor: (data) => http.post('/doctors/apply', data),
  myDoctorApply: () => http.get('/doctors/my-apply'),

  // AI
  aiSessions: () => http.get('/ai/sessions'),
  aiCreateSession: (title) => http.post('/ai/sessions', null, { params: { title } }),
  aiDeleteSession: (id) => http.delete(`/ai/sessions/${id}`),
  aiMessages: (id) => http.get(`/ai/sessions/${id}/messages`),

  // 测评
  assessments: () => http.get('/assessments'),
  assessment: (id) => http.get(`/assessments/${id}`),
  submitAssessment: (id, answers) => http.post(`/assessments/${id}/submit`, { answers }),
  myAssessmentRecords: () => http.get('/assessments/records/mine'),
  assessmentRecord: (id) => http.get(`/assessments/records/${id}`),

  // 症状
  addSymptom: (data) => http.post('/symptoms', data),
  symptoms: (days) => http.get('/symptoms', { params: { days } }),
  symptomTrend: (days) => http.get('/symptoms/trend', { params: { days } }),
  deleteSymptom: (id) => http.delete(`/symptoms/${id}`),

  // 今天的小事（微行动）
  microLevels: () => http.get('/micro/levels'),
  microOptions: (level) => http.get('/micro/options', { params: { level } }),
  microLog: (data) => http.post('/micro/log', data),
  microRecent: (days = 14) => http.get('/micro/recent', { params: { days } }),
  microToday: () => http.get('/micro/today'),

  // 医患
  applyBind: (data) => http.post('/consult/apply', data),
  myDoctors: () => http.get('/consult/my-doctors'),
  conversations: () => http.get('/consult/conversations'),
  history: (otherId) => http.get(`/consult/history/${otherId}`),
  sendMessage: (data) => http.post('/consult/messages', data),

  // 医生端
  applications: () => http.get('/doctor/workbench/applications'),
  patients: () => http.get('/doctor/workbench/patients'),
  auditBind: (data) => http.post('/doctor/workbench/audit', data),
  reports: () => http.get('/doctor/workbench/reports'),
  saveReport: (data) => http.post('/doctor/workbench/reports', data),
  publishReport: (id) => http.post(`/doctor/workbench/reports/${id}/publish`),
  aiDraft: (params) => http.get('/doctor/workbench/reports/ai-draft', { params }),
  publishResource: (data) => http.post('/doctor/workbench/resources', data),
  myResources: () => http.get('/doctor/workbench/resources'),

  // 内容
  resources: (params) => http.get('/content/resources', { params }),
  resource: (id) => http.get(`/content/resources/${id}`),
  articles: (params) => http.get('/content/articles', { params }),
  article: (id) => http.get(`/content/articles/${id}`),
  notices: () => http.get('/content/notices'),
  recommend: (limit = 4) => http.get('/content/recommend', { params: { limit } }),

  // 报告（患者）
  myReports: () => http.get('/reports/mine'),
  report: (id) => http.get(`/reports/${id}`),

  // 管理端
  dashboard: () => http.get('/admin/dashboard'),
  adminUsers: (params) => http.get('/admin/users', { params }),
  toggleUser: (id) => http.post(`/admin/users/${id}/toggle`),
  adminDoctors: (params) => http.get('/admin/doctors', { params }),
  auditDoctor: (id, data) => http.post(`/admin/doctors/${id}/audit`, data),
  adminAssessments: () => http.get('/admin/assessments'),
  saveAssessment: (data) => http.post('/admin/assessments', data),
  questions: (id) => http.get(`/admin/assessments/${id}/questions`),
  saveQuestion: (data) => http.post('/admin/questions', data),
  deleteQuestion: (id) => http.delete(`/admin/questions/${id}`),
  adminArticles: () => http.get('/admin/articles'),
  saveArticle: (data) => http.post('/admin/articles', data),
  deleteArticle: (id) => http.delete(`/admin/articles/${id}`),
  adminNotices: () => http.get('/admin/notices'),
  saveNotice: (data) => http.post('/admin/notices', data),
  deleteNotice: (id) => http.delete(`/admin/notices/${id}`),
  adminResources: (params) => http.get('/admin/resources', { params }),
  auditResource: (id, data) => http.post(`/admin/resources/${id}/audit`, data)
}

export { http }
