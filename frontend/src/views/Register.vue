<template>
  <div class="auth">
    <div class="panel">
      <h2>创建账号</h2>
      <p class="muted sub">不用真名。选一个数字，它就是你在心屿的身份。</p>

      <el-form :model="form" label-position="top">
        <el-form-item label="你的数字">
          <div class="digit-row">
            <el-input
              v-model="form.username"
              size="large"
              inputmode="numeric"
              maxlength="8"
              placeholder="比如生日 0317"
              @input="onDigitInput"
            >
              <template #prefix><span class="hash">#</span></template>
            </el-input>
            <el-button size="large" @click="rollDigit">
              <el-icon><Refresh /></el-icon> 随机
            </el-button>
          </div>
          <div class="digit-hint">
            3 位以上纯数字。生日、幸运数、球衣号都行——它只属于你。
          </div>
        </el-form-item>

        <el-form-item label="怎么称呼你（可留空）">
          <el-input v-model="form.realName" size="large" maxlength="12" placeholder="留空就用 #数字 显示" />
        </el-form-item>

        <div class="grid g2">
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" size="large" show-password placeholder="6 位以上" />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="confirm" type="password" size="large" show-password placeholder="再输一次" />
          </el-form-item>
        </div>

        <div class="grid g2">
          <el-form-item label="性别">
            <el-select v-model="form.gender" size="large" placeholder="选择" style="width: 100%">
              <el-option label="女" value="女" />
              <el-option label="男" value="男" />
              <el-option label="不便透露" value="保密" />
            </el-select>
          </el-form-item>
          <el-form-item label="年龄">
            <el-input-number v-model="form.age" :min="12" :max="100" size="large" style="width: 100%" />
          </el-form-item>
        </div>

        <el-button type="primary" size="large" style="width: 100%; margin-top: 8px" :loading="loading" @click="submit">
          注册并登录
        </el-button>
      </el-form>

      <div class="hint">
        <span>已有账号？</span>
        <router-link to="/login" class="link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const confirm = ref('')
const form = reactive({ username: '', realName: '', password: '', gender: '保密', age: 24 })

// 只接受数字：心理场景下用数字做匿名身份，避免被迫起名的负担
const onDigitInput = (v) => {
  form.username = String(v || '').replace(/\D/g, '').slice(0, 8)
}

const rollDigit = () => {
  form.username = String(Math.floor(1000 + Math.random() * 9000))
}

const submit = async () => {
  if (!/^\d{3,8}$/.test(form.username)) {
    ElMessage.warning('请输入 3 到 8 位纯数字')
    return
  }
  if (!form.password || form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (form.password !== confirm.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await store.register({
      ...form,
      // 不填称呼时，用 #数字 作为显示名
      realName: form.realName.trim() || '#' + form.username
    })
    ElMessage.success(`记住你的数字 #${form.username}，以后用它登录`)
    router.push('/')
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: radial-gradient(circle at 70% 10%, #eaf1ec, #f7f6f2 60%);
  padding: 40px;
}

.panel {
  width: 440px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 34px 36px 28px;
  box-shadow: var(--shadow);
}

h2 {
  font-size: 24px;
  margin: 0 0 6px;
}

.sub {
  margin: 0 0 22px;
  font-size: 13px;
}

.digit-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.digit-row :deep(.el-input) {
  flex: 1;
}

.hash {
  color: var(--sage);
  font-weight: 500;
  font-size: 15px;
}

.digit-hint {
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-3);
  margin-top: 2px;
}

.hint {
  margin-top: 16px;
  font-size: 13px;
  color: var(--ink-3);
  text-align: center;
}

.link {
  color: var(--sage);
  margin-left: 4px;
}
</style>
