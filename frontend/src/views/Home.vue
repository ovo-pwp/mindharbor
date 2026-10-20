<template>
  <div>
    <div class="hero">
      <div>
        <div class="hi">{{ greeting }}，{{ store.name }}</div>
        <p class="muted">今天感觉怎么样？不管好坏，都可以在这里放一放。</p>
        <div class="row wrap" style="margin-top: 16px">
          <el-button type="primary" @click="$router.push('/ai')">
            <el-icon><ChatDotRound /></el-icon> 和 AI 聊聊
          </el-button>
          <el-button @click="$router.push('/assessments')">做个测评</el-button>
        </div>
      </div>
      <div class="hero-art">
        <div class="ring r1" /><div class="ring r2" /><div class="ring r3" />
      </div>
    </div>

    <div class="grid g4" style="margin-bottom: 18px">
      <div class="card stat">
        <div class="label">完成测评</div>
        <div class="value">{{ stats.records }}<span>次</span></div>
      </div>
      <div class="card stat">
        <div class="label">情绪记录</div>
        <div class="value">{{ stats.symptoms }}<span>条</span></div>
      </div>
      <div class="card stat">
        <div class="label">AI 对话</div>
        <div class="value">{{ stats.sessions }}<span>个</span></div>
      </div>
      <div class="card stat">
        <div class="label">绑定咨询师</div>
        <div class="value">{{ stats.doctors }}<span>位</span></div>
      </div>
    </div>

    <div class="grid g2">
      <div class="card">
        <div class="card-title">为你推荐</div>
        <div v-if="recommend.length === 0" class="muted empty">暂无推荐，先去逛逛心理资源吧</div>
        <div v-for="r in recommend" :key="r.id" class="rec-item" @click="$router.push('/resources')">
          <div class="rec-icon"><el-icon><Headset /></el-icon></div>
          <div class="rec-body">
            <div class="rec-title">{{ r.title }}</div>
            <div class="rec-meta">
              <el-tag size="small" effect="plain">{{ typeLabel(r.type) }}</el-tag>
              <span class="muted">{{ r.category }}</span>
              <span class="muted">· {{ r.viewCount }} 次浏览</span>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">平台公告</div>
        <div v-for="n in notices" :key="n.id" class="notice">
          <div class="notice-title">{{ n.title }}</div>
          <div class="notice-body muted">{{ n.content }}</div>
        </div>
        <div v-if="notices.length === 0" class="muted empty">暂无公告</div>
      </div>
    </div>

    <div class="card" style="margin-top: 18px">
      <div class="card-title">最近一次测评</div>
      <div v-if="lastRecord" class="row between">
        <div>
          <div style="font-weight: 600">{{ lastRecord.assessmentName }}</div>
          <div class="muted" style="margin-top: 4px">
            标准分 {{ lastRecord.standardScore }} · {{ lastRecord.level }} · {{ fmt(lastRecord.createdAt) }}
          </div>
        </div>
        <el-button @click="$router.push(`/assessments/result/${lastRecord.id}`)">查看报告</el-button>
      </div>
      <div v-else class="muted empty">
        还没有测评记录，
        <a class="link" @click="$router.push('/assessments')">去做一次 SDS 或 SAS</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { useUserStore } from '../store/user'

const store = useUserStore()
const stats = reactive({ records: 0, symptoms: 0, sessions: 0, doctors: 0 })
const recommend = ref([])
const notices = ref([])
const lastRecord = ref(null)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const typeLabel = (t) => ({ ARTICLE: '图文', AUDIO: '音频', VIDEO: '视频', EXERCISE: '练习' }[t] || t)
const fmt = (s) => (s ? String(s).replace('T', ' ').slice(0, 16) : '')

onMounted(async () => {
  try {
    const [r1, r2, r3, r4, r5, r6] = await Promise.all([
      api.myAssessmentRecords(),
      api.symptoms(90),
      api.aiSessions(),
      api.myDoctors(),
      api.recommend(4),
      api.notices()
    ])
    stats.records = r1.data?.length || 0
    stats.symptoms = r2.data?.length || 0
    stats.sessions = r3.data?.length || 0
    stats.doctors = (r4.data || []).filter((d) => d.status === 'APPROVED').length
    recommend.value = r5.data || []
    notices.value = r6.data || []
    lastRecord.value = r1.data?.[0] || null
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.hero {
  position: relative;
  overflow: hidden;
  background: linear-gradient(120deg, #23443a, #1b2f28 62%, #24463b);
  color: #eaf2ed;
  border-radius: 18px;
  padding: 32px 34px;
  margin-bottom: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hi {
  font-size: 23px;
  font-weight: 600;
  letter-spacing: -0.3px;
}

.hero .muted {
  color: #9fb6aa;
  margin: 8px 0 0;
  font-size: 13.5px;
}

.hero-art {
  position: relative;
  width: 180px;
  height: 110px;
}

.ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(140, 190, 168, 0.32);
}

.r1 {
  width: 190px;
  height: 190px;
  right: -60px;
  top: -50px;
}

.r2 {
  width: 130px;
  height: 130px;
  right: -20px;
  top: -20px;
  background: radial-gradient(circle, rgba(120, 174, 149, 0.16), transparent 70%);
}

.r3 {
  width: 70px;
  height: 70px;
  right: 20px;
  top: 10px;
  background: radial-gradient(circle, rgba(160, 206, 183, 0.22), transparent 70%);
}

.stat .label {
  font-size: 12.5px;
  color: var(--ink-3);
}

.stat .value {
  font-size: 30px;
  font-weight: 600;
  margin-top: 6px;
  color: var(--ink);
  letter-spacing: -0.5px;
}

.stat .value span {
  font-size: 12px;
  color: var(--ink-3);
  margin-left: 4px;
  font-weight: 400;
}

.rec-item {
  display: flex;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px dashed var(--line);
  cursor: pointer;
}

.rec-item:last-child {
  border-bottom: none;
}

.rec-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: var(--sage-soft);
  color: var(--sage-dark);
  display: grid;
  place-items: center;
  flex: 0 0 34px;
}

.rec-title {
  font-size: 13.5px;
  font-weight: 500;
  margin-bottom: 4px;
}

.rec-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.notice {
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
}

.notice:last-child {
  border-bottom: none;
}

.notice-title {
  font-size: 13.5px;
  font-weight: 500;
  margin-bottom: 3px;
}

.notice-body {
  font-size: 12.5px;
  line-height: 1.6;
}

.empty {
  font-size: 13px;
  padding: 16px 0;
}

.link {
  color: var(--sage);
  cursor: pointer;
}

@media (max-width: 768px) {
  .hero {
    padding: 20px 18px;
    border-radius: 14px;
    margin-bottom: 13px;
  }
  .hero-art {
    display: none;
  }
  .hi {
    font-size: 18px;
  }
  .hero .muted {
    font-size: 12.5px;
    margin-top: 6px;
  }
  .stat .value {
    font-size: 22px;
  }
  .stat .label {
    font-size: 11.5px;
  }
  .rec-item {
    padding: 9px 0;
  }
  .rec-icon {
    width: 30px;
    height: 30px;
    flex-basis: 30px;
  }
  .rec-title {
    font-size: 13px;
  }
  .notice-body {
    font-size: 12px;
  }
}
</style>
