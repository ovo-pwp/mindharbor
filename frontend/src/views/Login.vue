<template>
  <div class="auth">
    <div class="left">
      <div class="quote">
        <div class="mark">“</div>
        <p>你不需要先变好，才开始被理解。</p>
        <span>心屿 · 一个可以慢慢说的地方</span>
      </div>
      <div class="features">
        <div class="f"><el-icon><ChatDotRound /></el-icon> AI 心理助手，7×24 小时在线</div>
        <div class="f"><el-icon><Document /></el-icon> SDS / SAS 专业量表，自动评分</div>
        <div class="f"><el-icon><FirstAidKit /></el-icon> 认证咨询师，一对一沟通</div>
      </div>
    </div>

    <div class="right">
      <div class="form-wrap">
        <h2>欢迎回来</h2>
        <p class="muted sub">登录后继续你的心理成长记录</p>

        <el-form :model="form" @submit.prevent="submit" label-position="top">
          <el-form-item label="账号 / 数字">
            <el-input v-model="form.username" size="large" placeholder="你的数字，如 1024" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" size="large" style="width: 100%; margin-top: 6px" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form>

        <div class="hint">
          <span>还没有账号？</span>
          <router-link to="/register" class="link">立即注册</router-link>
        </div>

        <div v-if="showDemo" class="demo">
          <div class="demo-title">演示账号（密码均为 123456）</div>
          <div class="demo-list">
            <span @click="fill('zhangxiaoyu')">患者 zhangxiaoyu</span>
            <span @click="fill('liyisheng')">医生 liyisheng</span>
            <span @click="fill('admin')">管理员 admin</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import { api } from '../api'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const loading = ref(false)
const showDemo = ref(false)
const form = reactive({ username: '', password: '' })

// 演示账号只在后端允许时展示（本地开发 true，公开部署 false）
onMounted(async () => {
  try {
    const res = await api.publicConfig()
    showDemo.value = !!(res.data && res.data.demoAccounts)
  } catch (e) {
    showDemo.value = false
  }
})

const fill = (name) => {
  form.username = name
  form.password = '123456'
}

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await store.login(form)
    ElMessage.success('登录成功')
    const role = store.role
    if (role === 'ADMIN') router.push('/admin')
    else if (role === 'DOCTOR') router.push('/workbench')
    else router.push(route.query.redirect || '/')
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth {
  display: flex;
  height: 100vh;
  background: var(--bg);
}

.left {
  flex: 1.1;
  background: radial-gradient(circle at 30% 20%, #2c443b, #16231e 70%);
  color: #eaf1ec;
  padding: 64px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

.left::after {
  content: '';
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(125, 178, 154, 0.18), transparent 65%);
  right: -120px;
  bottom: -80px;
}

.quote .mark {
  font-size: 74px;
  line-height: 0.6;
  color: #5d8f7a;
  font-family: Georgia, serif;
}

.quote p {
  font-size: 27px;
  line-height: 1.65;
  margin: 18px 0 14px;
  letter-spacing: 0.5px;
}

.quote span {
  color: #8fa89c;
  font-size: 13px;
  letter-spacing: 1px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 14px;
  font-size: 13.5px;
  color: #b6c7be;
}

.f {
  display: flex;
  align-items: center;
  gap: 10px;
}

.right {
  flex: 1;
  display: grid;
  place-items: center;
  padding: 40px;
}

.form-wrap {
  width: 356px;
}

.form-wrap h2 {
  font-size: 25px;
  margin: 0 0 6px;
  letter-spacing: -0.4px;
}

.sub {
  margin: 0 0 28px;
  font-size: 13px;
}

.hint {
  margin-top: 18px;
  font-size: 13px;
  color: var(--ink-3);
  text-align: center;
}

.link {
  color: var(--sage);
  font-weight: 500;
  margin-left: 4px;
}

.demo {
  margin-top: 30px;
  padding: 14px 16px;
  border: 1px dashed var(--line);
  border-radius: 10px;
  background: #fff;
}

.demo-title {
  font-size: 12px;
  color: var(--ink-3);
  margin-bottom: 9px;
}

.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-list span {
  font-size: 12px;
  padding: 4px 9px;
  border-radius: 6px;
  background: var(--sage-soft);
  color: var(--sage-dark);
  cursor: pointer;
  transition: all 0.15s;
}

.demo-list span:hover {
  background: var(--sage);
  color: #fff;
}

@media (max-width: 900px) {
  .left {
    display: none;
  }
}
</style>
