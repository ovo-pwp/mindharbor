<template>
  <div v-if="doctor" class="detail">
    <div class="card head">
      <div class="avatar">{{ doctor.initial }}</div>
      <div class="info">
        <div class="row" style="gap: 10px">
          <h1>{{ doctor.name }}</h1>
          <el-tag size="small" effect="plain" type="success">已认证</el-tag>
        </div>
        <div class="muted sub">{{ doctor.title }} · {{ doctor.hospital }}</div>
        <div class="row wrap" style="gap: 16px; margin-top: 12px; font-size: 12.5px">
          <span><el-icon color="#b08a4a"><StarFilled /></el-icon> {{ doctor.rating }} 分</span>
          <span class="muted">{{ doctor.consultCount }} 次咨询</span>
          <span class="muted">{{ doctor.years }} 年从业</span>
          <span class="price">¥{{ doctor.price }} / 50 分钟</span>
        </div>
      </div>
      <div class="actions">
        <el-tag v-if="doctor.bound" type="success" effect="light" size="large">已建立咨询关系</el-tag>
        <el-tag v-else-if="doctor.bindStatus === 'PENDING'" type="warning" effect="light" size="large">申请待确认</el-tag>
        <el-button v-else type="primary" size="large" @click="dialog = true">申请咨询</el-button>
      </div>
    </div>

    <div class="grid g2" style="margin-top: 16px; align-items: start">
      <div class="stack">
        <div class="card">
          <div class="card-title">擅长领域</div>
          <div class="row wrap" style="gap: 8px">
            <span v-for="s in doctor.specialty" :key="s" class="chip">{{ s }}</span>
          </div>
        </div>
        <div class="card">
          <div class="card-title">个人简介</div>
          <p class="intro">{{ doctor.intro }}</p>
        </div>
      </div>

      <div class="stack">
        <div class="card">
          <div class="card-title">咨询设置</div>
          <div class="kv"><span>咨询方式</span><b>线上文字 / 语音</b></div>
          <div class="kv"><span>单次时长</span><b>50 分钟</b></div>
          <div class="kv"><span>费用</span><b>¥{{ doctor.price }}</b></div>
          <div class="kv"><span>资质编号</span><b>PSY-{{ 1000 + doctor.userId }}</b></div>
        </div>
        <div class="card note">
          <div class="card-title">开始之前</div>
          <ul>
            <li>首次咨询前，医生会先查看你授权的情绪记录与测评结果</li>
            <li>咨询关系建立后，可在「消息」中直接沟通</li>
            <li>平台不提供药物建议，如需用药请前往精神科就诊</li>
          </ul>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialog" title="申请建立咨询关系" width="480px">
      <p class="muted" style="font-size: 13px; margin-top: 0">
        简单说明你想解决的问题，医生确认后会与你联系。
      </p>
      <el-input v-model="reason" type="textarea" :rows="4" placeholder="例如：最近三个月经常失眠，工作压力很大…" />
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="apply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const route = useRoute()
const doctor = ref(null)
const dialog = ref(false)
const reason = ref('')
const submitting = ref(false)

const load = async () => {
  const res = await api.doctor(route.params.id)
  doctor.value = res.data
}

const apply = async () => {
  submitting.value = true
  try {
    await api.applyBind({ doctorId: doctor.value.id, reason: reason.value })
    ElMessage.success('申请已提交，等待医生确认')
    dialog.value = false
    await load()
  } catch (e) {
    // ignore
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail {
  max-width: 1000px;
  margin: 0 auto;
}

.head {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: linear-gradient(135deg, #e8f0eb, #d3e2da);
  color: var(--sage-dark);
  display: grid;
  place-items: center;
  font-size: 28px;
  font-weight: 600;
  flex: 0 0 72px;
}

.info {
  flex: 1;
  min-width: 0;
}

h1 {
  font-size: 24px;
  margin: 0;
  letter-spacing: -0.3px;
}

.sub {
  font-size: 13px;
  margin-top: 6px;
}

.price {
  color: var(--clay);
  font-weight: 600;
}

.actions {
  flex: 0 0 auto;
}

.chip {
  padding: 6px 13px;
  border-radius: 8px;
  background: var(--sage-soft);
  color: var(--sage-dark);
  font-size: 12.5px;
}

.intro {
  font-size: 13.5px;
  line-height: 1.85;
  color: var(--ink-2);
  margin: 0;
}

.kv {
  display: flex;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
}

.kv:last-child {
  border-bottom: none;
}

.kv span {
  color: var(--ink-3);
}

.note ul {
  margin: 0;
  padding-left: 18px;
  font-size: 12.5px;
  color: var(--ink-3);
  line-height: 2;
}
</style>
