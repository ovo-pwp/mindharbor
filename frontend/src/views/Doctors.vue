<template>
  <div>
    <div class="page-head row between wrap">
      <div>
        <h1>找咨询师</h1>
        <p>平台已入驻 {{ total }} 位认证咨询师，可根据擅长领域筛选。</p>
      </div>
      <div class="row">
        <el-input v-model="keyword" placeholder="搜索擅长领域 / 职称 / 医院" style="width: 260px" clearable
                  @keyup.enter="load(1)" @clear="load(1)">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="load(1)">搜索</el-button>
      </div>
    </div>

    <div class="row wrap" style="margin-bottom: 18px; gap: 8px">
      <span class="filter-label muted">擅长领域</span>
      <el-check-tag v-for="s in specialties" :key="s" :checked="activeSpec === s" @change="toggleSpec(s)">
        {{ s }}
      </el-check-tag>
    </div>

    <div class="grid g3">
      <div v-for="d in records" :key="d.id" class="card doc" @click="$router.push(`/doctors/${d.id}`)">
        <div class="row" style="gap: 12px; align-items: flex-start">
          <div class="avatar">{{ d.initial }}</div>
          <div style="flex: 1; min-width: 0">
            <div class="row between">
              <div class="name">{{ d.name }}</div>
              <div class="price">¥{{ d.price }}</div>
            </div>
            <div class="title muted">{{ d.title }}</div>
            <div class="hospital muted">{{ d.hospital }}</div>
          </div>
        </div>
        <div class="tags">
          <span v-for="s in d.specialty.slice(0, 3)" :key="s">{{ s }}</span>
        </div>
        <div class="intro">{{ d.intro }}</div>
        <div class="row between foot">
          <div class="muted" style="font-size: 12px">
            <el-icon color="#b08a4a"><StarFilled /></el-icon> {{ d.rating }} · {{ d.consultCount }} 次咨询 · {{ d.years }} 年
          </div>
          <el-tag v-if="d.bound" size="small" type="success" effect="light">已绑定</el-tag>
          <el-tag v-else-if="d.bindStatus === 'PENDING'" size="small" type="warning" effect="light">待确认</el-tag>
          <el-button v-else size="small" type="primary" plain @click.stop="$router.push(`/doctors/${d.id}`)">查看</el-button>
        </div>
      </div>
    </div>

    <div v-if="records.length === 0" class="card muted" style="text-align: center; padding: 48px">
      没有匹配的咨询师，换个关键词试试
    </div>

    <el-pagination
      v-if="pages > 1"
      style="margin-top: 22px; justify-content: center"
      layout="prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="load"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const records = ref([])
const total = ref(0)
const pages = ref(0)
const page = ref(1)
const size = 9
const keyword = ref('')
const activeSpec = ref('')
const specialties = ['焦虑', '抑郁', '睡眠', '亲密关系', '职场', '青少年', '原生家庭']

const load = async (p = 1) => {
  page.value = p
  const res = await api.doctors({ keyword: keyword.value, specialty: activeSpec.value, page: p, size })
  records.value = res.data.records || []
  total.value = res.data.total || 0
  pages.value = res.data.pages || 0
}

const toggleSpec = (s) => {
  activeSpec.value = activeSpec.value === s ? '' : s
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped>
.filter-label {
  font-size: 12.5px;
  margin-right: 4px;
}

.doc {
  cursor: pointer;
  transition: all 0.18s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(30, 43, 38, 0.1);
  border-color: #d9ded8;
}

.avatar {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  background: linear-gradient(135deg, #e8f0eb, #d6e4dc);
  color: var(--sage-dark);
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 600;
  flex: 0 0 46px;
}

.name {
  font-size: 15.5px;
  font-weight: 600;
}

.price {
  font-size: 15px;
  font-weight: 600;
  color: var(--clay);
}

.price::first-letter {
  font-size: 11px;
}

.title {
  font-size: 12.5px;
  margin-top: 3px;
}

.hospital {
  font-size: 12px;
  margin-top: 2px;
}

.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tags span {
  font-size: 11.5px;
  padding: 3px 9px;
  border-radius: 6px;
  background: var(--sage-soft);
  color: var(--sage-dark);
}

.intro {
  font-size: 12.5px;
  color: var(--ink-3);
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.foot {
  padding-top: 10px;
  border-top: 1px dashed var(--line);
}
</style>
