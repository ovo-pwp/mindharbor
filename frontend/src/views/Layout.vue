<template>
  <div class="shell">
    <aside class="side">
      <div class="brand">
        <div class="logo">心</div>
        <div>
          <div class="brand-name">心屿</div>
          <div class="brand-sub">MindHarbor</div>
        </div>
      </div>

      <nav class="nav">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="nav-item"
          :class="{ active: isActive(m.path) }"
        >
          <el-icon :size="17"><component :is="m.icon" /></el-icon>
          <span>{{ m.label }}</span>
        </router-link>
      </nav>

      <div class="side-foot">
        <div class="avatar">{{ store.name.slice(0, 1) }}</div>
        <div class="who">
          <div class="name">{{ store.name }}</div>
          <div class="role">{{ store.roleLabel }}</div>
        </div>
        <el-tooltip content="退出登录" placement="top">
          <el-icon class="logout" @click="logout"><SwitchButton /></el-icon>
        </el-tooltip>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="crumb">{{ $route.meta.title || '心屿' }}</div>
        <div class="row" style="gap: 12px">
          <span class="dot" /> <span class="muted" style="font-size: 12px">AI 助手在线</span>
        </div>
      </header>
      <div class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const store = useUserStore()
const route = useRoute()
const router = useRouter()

// 有 token 但本地没有用户信息（例如换设备、清过缓存）时补拉一次
onMounted(() => {
  if (store.token && !store.user) store.refresh()
})

const patientMenus = [
  { path: '/', label: '首页', icon: 'HomeFilled' },
  { path: '/ai', label: 'AI 心理助手', icon: 'ChatDotRound' },
  { path: '/assessments', label: '心理测评', icon: 'Document' },
  { path: '/doctors', label: '找咨询师', icon: 'FirstAidKit' },
  { path: '/resources', label: '心理资源', icon: 'Headset' },
  { path: '/articles', label: '心理科普', icon: 'Reading' },
  { path: '/symptoms', label: '情绪记录', icon: 'TrendCharts' },
  { path: '/micro', label: '今天的小事', icon: 'Sunny' },
  { path: '/consult', label: '消息', icon: 'Message' },
  { path: '/reports', label: '我的报告', icon: 'Tickets' },
  { path: '/profile', label: '个人中心', icon: 'Setting' }
]

const doctorMenus = [
  { path: '/workbench', label: '医生工作台', icon: 'Odometer' },
  { path: '/consult', label: '消息', icon: 'Message' },
  { path: '/profile', label: '个人中心', icon: 'Setting' }
]

const adminMenus = [
  { path: '/admin', label: '管理后台', icon: 'DataBoard' },
  { path: '/profile', label: '个人中心', icon: 'Setting' }
]

const menus = computed(() => {
  if (store.role === 'ADMIN') return adminMenus
  if (store.role === 'DOCTOR') return doctorMenus
  return patientMenus
})

const isActive = (path) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const logout = () => {
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.side {
  width: 228px;
  flex: 0 0 228px;
  background: #1b2a24;
  color: #e8efe9;
  display: flex;
  flex-direction: column;
  padding: 22px 14px 14px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 8px 24px;
}

.logo {
  width: 36px;
  height: 36px;
  border-radius: 11px;
  background: linear-gradient(135deg, #5d8f7a, #3c6455);
  display: grid;
  place-items: center;
  font-size: 17px;
  font-weight: 600;
  color: #fff;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

.brand-sub {
  font-size: 10px;
  color: #7f968b;
  letter-spacing: 1.2px;
  text-transform: uppercase;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow-y: auto;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 9px;
  color: #b6c7be;
  font-size: 13.5px;
  transition: all 0.16s;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.nav-item.active {
  background: linear-gradient(90deg, rgba(93, 143, 122, 0.32), rgba(93, 143, 122, 0.1));
  color: #fff;
  box-shadow: inset 2px 0 0 #7fb39c;
}

.side-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 10px 4px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  margin-top: 8px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.12);
  display: grid;
  place-items: center;
  font-size: 13px;
}

.who {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role {
  font-size: 11px;
  color: #7f968b;
}

.logout {
  cursor: pointer;
  color: #7f968b;
  transition: color 0.16s;
}

.logout:hover {
  color: #e08c6a;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  height: 56px;
  flex: 0 0 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--line);
}

.crumb {
  font-size: 13px;
  color: var(--ink-3);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4caf7d;
  box-shadow: 0 0 0 3px rgba(76, 175, 125, 0.16);
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 26px 28px 40px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-leave-to {
  opacity: 0;
}
</style>
