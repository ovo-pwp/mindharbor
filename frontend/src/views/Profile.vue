<template>
  <div class="profile">
    <div class="page-head">
      <h1>个人中心</h1>
      <p>管理你的资料与账号安全。</p>
    </div>

    <div class="grid g2" style="align-items: start">
      <div class="card">
        <div class="card-title">基本资料</div>
        <el-form label-position="top">
          <div class="grid g2">
            <el-form-item label="姓名">
              <el-input v-model="form.realName" />
            </el-form-item>
            <el-form-item label="数字身份">
              <el-input :model-value="'#' + (store.user?.username || '')" disabled />
            </el-form-item>
          </div>
          <div class="grid g2">
            <el-form-item label="性别">
              <el-select v-model="form.gender" style="width: 100%">
                <el-option label="女" value="女" />
                <el-option label="男" value="男" />
                <el-option label="保密" value="保密" />
              </el-select>
            </el-form-item>
            <el-form-item label="年龄">
              <el-input-number v-model="form.age" :min="12" :max="100" style="width: 100%" />
            </el-form-item>
          </div>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存资料</el-button>
        </el-form>
      </div>

      <div class="stack">
        <div class="card">
          <div class="card-title">账号信息</div>
          <div class="kv"><span>数字身份</span><b>#{{ store.user?.username }}</b></div>
          <div class="kv"><span>角色</span><b>{{ store.roleLabel }}</b></div>
          <div class="kv"><span>注册时间</span><b>{{ fmt(store.user?.createdAt) }}</b></div>
          <div class="kv"><span>账号状态</span>
            <el-tag size="small" :type="store.user?.status === 'ACTIVE' ? 'success' : 'info'">
              {{ store.user?.status === 'ACTIVE' ? '正常' : '已停用' }}
            </el-tag>
          </div>
        </div>

        <div class="card">
          <div class="card-title">修改密码</div>
          <el-form label-position="top">
            <el-form-item label="原密码">
              <el-input v-model="pwd.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwd.newPassword" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
            <el-button @click="changePwd">更新密码</el-button>
          </el-form>
        </div>

        <div v-if="role === 'USER'" class="card">
          <div class="card-title">咨询师入驻</div>
          <template v-if="apply && apply.status === 'PENDING'">
            <el-alert type="warning" :closable="false" show-icon title="申请审核中"
                      description="平台会在 1-3 个工作日内完成资质审核。" />
          </template>
          <template v-else-if="apply && apply.status === 'REJECTED'">
            <el-alert type="error" :closable="false" show-icon title="申请未通过"
                      :description="apply.auditRemark || '资质材料不符合要求，可修改后重新提交。'" />
            <el-button style="margin-top: 12px" @click="applyDialog = true">重新提交</el-button>
          </template>
          <template v-else-if="!apply">
            <p class="muted" style="font-size: 12.5px; line-height: 1.7">
              如果你持有心理咨询相关资质，可以申请入驻，为来访者提供专业服务。
            </p>
            <el-button type="primary" @click="applyDialog = true">申请成为咨询师</el-button>
          </template>
        </div>
      </div>
    </div>

    <el-dialog v-model="applyDialog" title="咨询师入驻申请" width="560px">
      <el-form label-position="top">
        <div class="grid g2">
          <el-form-item label="所在机构 / 医院">
            <el-input v-model="applyForm.hospital" placeholder="如：杭州市第一人民医院" />
          </el-form-item>
          <el-form-item label="职称">
            <el-input v-model="applyForm.title" placeholder="如：国家二级心理咨询师" />
          </el-form-item>
        </div>
        <el-form-item label="擅长领域（逗号分隔）">
          <el-input v-model="applyForm.specialty" placeholder="焦虑,抑郁,亲密关系" />
        </el-form-item>
        <div class="grid g2">
          <el-form-item label="从业年限">
            <el-input-number v-model="applyForm.years" :min="0" :max="60" style="width: 100%" />
          </el-form-item>
          <el-form-item label="单次咨询价格（元）">
            <el-input-number v-model="applyForm.price" :min="0" :step="50" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="资质证书编号">
          <el-input v-model="applyForm.certNo" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="applyForm.intro" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialog = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store/user'

const store = useUserStore()
const role = computed(() => store.role)
const saving = ref(false)
const applying = ref(false)
const applyDialog = ref(false)
const apply = ref(null)

const form = reactive({ realName: '', gender: '女', age: 24, phone: '', email: '' })
const pwd = reactive({ oldPassword: '', newPassword: '' })
const applyForm = reactive({ hospital: '', title: '', specialty: '', years: 3, price: 300, certNo: '', intro: '' })

const fmt = (s) => (s ? String(s).replace('T', ' ').slice(0, 16) : '-')

const save = async () => {
  saving.value = true
  try {
    const res = await api.updateProfile(form)
    store.user = res.data
    localStorage.setItem('user', JSON.stringify(res.data))
    ElMessage.success('已保存')
  } catch (e) {
    // ignore
  } finally {
    saving.value = false
  }
}

const changePwd = async () => {
  if (!pwd.oldPassword || !pwd.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  try {
    await api.changePassword(pwd)
    ElMessage.success('密码已更新，请重新登录')
    store.logout()
    location.hash = '#/login'
  } catch (e) {
    // ignore
  }
}

const submitApply = async () => {
  applying.value = true
  try {
    await api.applyDoctor(applyForm)
    ElMessage.success('申请已提交，等待审核')
    applyDialog.value = false
    await loadApply()
  } catch (e) {
    // ignore
  } finally {
    applying.value = false
  }
}

const loadApply = async () => {
  if (role.value !== 'USER') return
  try {
    const res = await api.myDoctorApply()
    apply.value = res.data
    if (res.data) {
      Object.assign(applyForm, {
        hospital: res.data.hospital,
        title: res.data.title,
        specialty: res.data.specialty,
        years: res.data.years,
        price: Number(res.data.price || 300),
        certNo: res.data.certNo,
        intro: res.data.intro
      })
    }
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  const res = await api.profile()
  store.user = res.data
  Object.assign(form, {
    realName: res.data.realName,
    gender: res.data.gender || '女',
    age: res.data.age || 24,
    phone: res.data.phone || '',
    email: res.data.email || ''
  })
  await loadApply()
})
</script>

<style scoped>
.profile {
  max-width: 1000px;
  margin: 0 auto;
}

.kv {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
}

.kv:last-child {
  border-bottom: none;
}

.kv span {
  color: var(--ink-3);
}
</style>
