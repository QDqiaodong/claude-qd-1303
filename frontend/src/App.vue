<script setup>
import { computed, onMounted, provide, ref, watch } from 'vue'
import router from './router'
import api from './api'

import RoadView from './views/RoadView.vue'
import LampView from './views/LampView.vue'
import InspectionView from './views/InspectionView.vue'
import RepairView from './views/RepairView.vue'

// ------------------------------------------------------------------
// 导航形态：顶部全局片区下拉（全部 / 城东 / 城西 / 城南 / 城北 / 开发区）。
// 下拉选中的片区就是全局上下文，通过 provide 注入，四个模块一律按它过滤数据。
// 操作范式：SVG 路段灯杆点位图（见 LampView）。
// ------------------------------------------------------------------

const zones = ['全部', '城东', '城西', '城南', '城北', '开发区']
const zone = ref('全部')

// 供各视图注入的过滤上下文（空串表示不过滤）
const zoneFilter = computed(() => (zone.value === '全部' ? '' : zone.value))
provide('zone', zone)
provide('zoneFilter', zoneFilter)

const items = [
  { path: '/road', label: '路段', comp: RoadView },
  { path: '/lamp', label: '灯具', comp: LampView },
  { path: '/inspection', label: '巡检记录', comp: InspectionView },
  { path: '/repair', label: '报修工单', comp: RepairView }
]

const currentPath = ref(router.currentRoute.value.path)
const counts = ref({})

const currentView = computed(() => {
  const m = items.find(i => i.path === currentPath.value)
  return m ? m.comp : RoadView
})

async function loadCounts() {
  const z = zoneFilter.value
  try {
    const [r, l, i, p] = await Promise.all([
      api.listRoads(z), api.listLamps(null, z),
      api.listInspections(null, z), api.listRepairs(null, z)
    ])
    counts.value = {
      '/road': r.length,
      '/lamp': l.length,
      '/inspection': i.length,
      '/repair': p.length
    }
  } catch (e) { /* 计数失败不影响主流程 */ }
}

function onSelect(index) {
  if (index !== currentPath.value) {
    router.push(index)
  }
}

router.afterEach((to) => { currentPath.value = to.path })

watch(zone, loadCounts)

onMounted(async () => {
  currentPath.value = router.currentRoute.value.path
  await loadCounts()
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">灯</span>
        <div class="brand-text">
          <div class="brand-title">城市照明管理所</div>
          <div class="brand-sub">灯黄主题 · 顶部全局片区下拉</div>
        </div>
      </div>

      <div class="zone-picker">
        <span class="zone-label">全局片区</span>
        <el-select v-model="zone" size="default" class="zone-select">
          <el-option v-for="z in zones" :key="z" :label="z" :value="z" />
        </el-select>
      </div>

      <el-menu
        class="nav-menu"
        mode="horizontal"
        :default-active="currentPath"
        :ellipsis="false"
        @select="onSelect"
      >
        <el-menu-item v-for="it in items" :key="it.path" :index="it.path">
          {{ it.label }}（{{ counts[it.path] ?? 0 }}）
        </el-menu-item>
      </el-menu>
    </header>

    <main class="content">
      <component :is="currentView" :key="currentPath + zone" @changed="loadCounts" />
    </main>
  </div>
</template>

<style scoped>
.app-shell { min-height: 100vh; display: flex; flex-direction: column; }

.topbar {
  display: flex; align-items: center; gap: 24px;
  padding: 0 24px; background: #fff;
  border-bottom: 1px solid var(--line);
  position: sticky; top: 0; z-index: 10;
}
.brand { display: flex; align-items: center; gap: 10px; padding: 10px 0; }
.brand-mark {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  background: var(--primary); color: #fff; font-weight: 700; font-size: 18px;
}
.brand-title { font-size: 15px; font-weight: 600; }
.brand-sub { font-size: 12px; color: var(--muted); }

.zone-picker { display: flex; align-items: center; gap: 8px; }
.zone-label { font-size: 13px; color: var(--muted); }
.zone-select { width: 130px; }

.nav-menu { flex: 1; border-bottom: none; }

.content { flex: 1; padding: 20px 24px 28px; overflow: auto; }
</style>
