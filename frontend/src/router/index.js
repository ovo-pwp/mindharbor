import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
      { path: 'ai', name: 'ai', component: () => import('../views/AiChat.vue'), meta: { title: 'AI 心理助手' } },
      { path: 'assessments', name: 'assessments', component: () => import('../views/Assessments.vue'), meta: { title: '心理测评' } },
      { path: 'assessments/:id', name: 'assessment-take', component: () => import('../views/AssessmentTake.vue'), meta: { title: '在线测评' } },
      { path: 'assessments/result/:id', name: 'assessment-result', component: () => import('../views/AssessmentResult.vue'), meta: { title: '测评结果' } },
      { path: 'doctors', name: 'doctors', component: () => import('../views/Doctors.vue'), meta: { title: '找咨询师' } },
      { path: 'doctors/:id', name: 'doctor-detail', component: () => import('../views/DoctorDetail.vue'), meta: { title: '咨询师详情' } },
      { path: 'resources', name: 'resources', component: () => import('../views/Resources.vue'), meta: { title: '心理资源' } },
      { path: 'articles', name: 'articles', component: () => import('../views/Articles.vue'), meta: { title: '心理科普' } },
      { path: 'articles/:id', name: 'article-detail', component: () => import('../views/ArticleDetail.vue'), meta: { title: '科普详情' } },
      { path: 'symptoms', name: 'symptoms', component: () => import('../views/Symptoms.vue'), meta: { title: '情绪记录' } },
      { path: 'micro', name: 'micro', component: () => import('../views/MicroActions.vue'), meta: { title: '今天的小事' } },
      { path: 'consult', name: 'consult', component: () => import('../views/Consult.vue'), meta: { title: '消息' } },
      { path: 'reports', name: 'reports', component: () => import('../views/Reports.vue'), meta: { title: '我的报告' } },
      { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } },
      { path: 'workbench', name: 'workbench', component: () => import('../views/Workbench.vue'), meta: { title: '医生工作台', role: 'DOCTOR' } },
      { path: 'admin', name: 'admin', component: () => import('../views/Admin.vue'), meta: { title: '管理后台', role: 'ADMIN' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) {
    return true
  }
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
