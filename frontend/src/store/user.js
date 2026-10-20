import { defineStore } from 'pinia'
import { api } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    doctorId: localStorage.getItem('doctorId') || null
  }),
  getters: {
    isLogin: (s) => !!s.token,
    role: (s) => s.user?.role || 'USER',
    roleLabel: (s) => {
      const map = { USER: '患者', DOCTOR: '咨询师', ADMIN: '管理员' }
      return map[s.user?.role] || '用户'
    },
    name: (s) => s.user?.realName || s.user?.username || ''
  },
  actions: {
    setLogin(data) {
      this.token = data.token
      this.user = data.user
      this.doctorId = data.doctorId
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
      if (data.doctorId) {
        localStorage.setItem('doctorId', String(data.doctorId))
      }
    },
    async login(payload) {
      const res = await api.login(payload)
      this.setLogin(res.data)
      return res.data
    },
    async register(payload) {
      const res = await api.register(payload)
      this.setLogin(res.data)
      return res.data
    },
    async refresh() {
      try {
        const res = await api.me()
        this.user = res.data
        localStorage.setItem('user', JSON.stringify(res.data))
      } catch (e) {
        // ignore
      }
    },
    logout() {
      api.logout().catch(() => {})
      this.token = ''
      this.user = null
      this.doctorId = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('doctorId')
    }
  }
})
