<template>
  <div v-if="article" class="wrap">
    <div class="head">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
    </div>
    <article class="card article">
      <div class="row" style="gap: 8px; margin-bottom: 12px">
        <el-tag size="small" effect="plain">{{ article.category }}</el-tag>
        <span class="muted" style="font-size: 12px">{{ article.viewCount }} 次阅读</span>
      </div>
      <h1>{{ article.title }}</h1>
      <div class="meta muted">
        {{ article.author }} · {{ fmt(article.createdAt) }}
      </div>
      <hr />
      <div class="markdown-body" v-html="render(article.content)"></div>
      <div class="tail">
        <div class="tail-text">如果这篇文章让你想起自己的困扰，可以试试和 AI 助手聊聊，或预约一位咨询师。</div>
        <div class="row" style="gap: 10px; margin-top: 12px">
          <el-button type="primary" @click="$router.push('/ai')">和 AI 聊聊</el-button>
          <el-button @click="$router.push('/doctors')">预约咨询师</el-button>
        </div>
      </div>
    </article>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import { api } from '../api'

const route = useRoute()
const article = ref(null)
const fmt = (s) => (s ? String(s).slice(0, 10) : '')
const render = (t) => marked.parse(t || '')

onMounted(async () => {
  const res = await api.article(route.params.id)
  article.value = res.data
})
</script>

<style scoped>
.wrap {
  max-width: 740px;
  margin: 0 auto;
}

.head {
  margin-bottom: 10px;
}

.article {
  padding: 36px 42px 30px;
}

h1 {
  font-size: 26px;
  line-height: 1.45;
  margin: 0 0 12px;
  letter-spacing: -0.4px;
}

.meta {
  font-size: 12.5px;
}

hr {
  border: none;
  border-top: 1px solid var(--line);
  margin: 22px 0 18px;
}

.tail {
  margin-top: 34px;
  padding-top: 20px;
  border-top: 1px dashed var(--line);
}

.tail-text {
  font-size: 13px;
  color: var(--ink-3);
  line-height: 1.75;
}

@media (max-width: 760px) {
  .article {
    padding: 24px 20px;
  }
}
</style>
