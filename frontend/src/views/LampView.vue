<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const zoneFilter = inject('zoneFilter', ref(''))

const roads = ref([])
const roadId = ref(null)
const lamps = ref([])

const drawer = ref(false)
const current = ref(null)
const lampDialog = ref(false)
const editingLamp = ref(null)
const lampForm = ref({})

const repairDialog = ref(false)
const repairForm = ref({})

const types = ['LED', '高压钠灯', '太阳能']
const statuses = ['亮灯', '故障', '维修中', '报废']
const faultTypes = ['不亮', '闪烁', '破损', '线路故障']

// 点位图配色：亮灯=绿，故障=红，维修中=琥珀，报废=灰
const colorOf = (s) => ({
  '亮灯': '#67C23A',
  '故障': '#F56C6C',
  '维修中': '#E6A23C',
  '报废': '#909399'
}[s] || '#909399')

const emit = defineEmits(['changed'])

const road = computed(() => roads.value.find(r => r.id === roadId.value) || null)

// 沿路段排布灯杆：奇数杆在路北侧、偶数杆在路南侧
const poles = computed(() => {
  const n = lamps.value.length
  if (n === 0) return []
  const left = 70
  const right = 880
  const step = n > 1 ? (right - left) / (n - 1) : 0
  return lamps.value.map((l, i) => {
    const x = n > 1 ? left + i * step : (left + right) / 2
    const north = i % 2 === 0
    return {
      lamp: l,
      x,
      poleY1: north ? 78 : 122,
      poleY2: north ? 44 : 156,
      cy: north ? 36 : 164,
      labelY: north ? 22 : 180,
      color: colorOf(l.status)
    }
  })
})

async function loadRoads() {
  roads.value = await api.listRoads(zoneFilter.value)
  if (!roads.value.some(r => r.id === roadId.value)) {
    // 优先选中一个「正常」路段，方便直接新增灯具
    const normal = roads.value.find(r => r.status === '正常')
    roadId.value = (normal || roads.value[0])?.id ?? null
  }
  await loadLamps()
}

async function loadLamps() {
  lamps.value = roadId.value ? await api.listLamps(roadId.value, zoneFilter.value) : []
}

async function load() {
  await loadRoads()
}

/* ---------------- 灯具详情抽屉 ---------------- */
function openPole(lamp) {
  current.value = lamp
  drawer.value = true
}

const lampNext = (s) => ({
  '亮灯': ['故障'],
  '故障': ['维修中'],
  '维修中': ['亮灯'],
  '报废': []
}[s] || [])

async function shift(lamp, status) {
  try {
    await api.changeLampStatus(lamp.id, status)
    ElMessage.success(`已流转至「${status}」`)
    await loadLamps()
    current.value = { ...lamp, status }
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

function openCreateLamp() {
  if (!roadId.value) { ElMessage.warning('请先选择路段'); return }
  editingLamp.value = null
  lampForm.value = {
    lampNo: '', roadId: roadId.value, poleNo: '',
    type: 'LED', watt: 120, installDate: null, status: '亮灯'
  }
  lampDialog.value = true
}
function openEditLamp(lamp) {
  editingLamp.value = lamp.id
  lampForm.value = { ...lamp }
  lampDialog.value = true
}

async function saveLamp() {
  const payload = {
    lampNo: lampForm.value.lampNo,
    roadId: Number(lampForm.value.roadId),
    poleNo: lampForm.value.poleNo,
    type: lampForm.value.type,
    watt: Number(lampForm.value.watt),
    installDate: lampForm.value.installDate || null,
    status: lampForm.value.status
  }
  try {
    if (editingLamp.value) {
      await api.updateLamp(editingLamp.value, payload)
      ElMessage.success('已更新灯具')
    } else {
      await api.createLamp(payload)
      ElMessage.success('已登记灯具')
    }
    lampDialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

/* ---------------- 点击灯杆后直接报修 ---------------- */
function openRepair(lamp) {
  repairForm.value = { lampId: lamp.id, faultType: '不亮', reportDate: null, worker: '' }
  repairDialog.value = true
}

async function submitRepair() {
  try {
    await api.createRepair({
      lampId: repairForm.value.lampId,
      faultType: repairForm.value.faultType,
      reportDate: repairForm.value.reportDate || null,
      worker: repairForm.value.worker || ''
    })
    ElMessage.success('报修工单已开出')
    repairDialog.value = false
    drawer.value = false
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

onMounted(load)
watch(zoneFilter, load)
watch(roadId, loadLamps)
</script>

<template>
  <div>
    <div class="bar">
      <h2>灯具 · 路段灯杆点位图</h2>
      <div class="bar-right">
        <span class="label">路段</span>
        <el-select v-model="roadId" class="road-select" placeholder="选择路段">
          <el-option v-for="r in roads" :key="r.id" :label="`${r.name}（${r.zone}）`" :value="r.id" />
        </el-select>
        <el-button type="primary" @click="openCreateLamp">新增灯具</el-button>
      </div>
    </div>

    <el-card shadow="never" class="map-card">
      <div class="map-head">
        <span class="road-title">{{ road ? road.name : '未选择路段' }}</span>
        <el-tag v-if="road" size="small" :type="road.status === '正常' ? 'success' : road.status === '施工' ? 'warning' : 'info'">
          {{ road.status }}
        </el-tag>
        <el-tag v-if="road" size="small" type="info">{{ road.zone }} · {{ road.lengthMeter }} 米 · {{ road.lampCount }} 杆</el-tag>
        <div class="legend">
          <span v-for="(c, s) in { '亮灯': '#67C23A', '故障': '#F56C6C', '维修中': '#E6A23C', '报废': '#909399' }" :key="s">
            <i :style="{ background: c }" />{{ s }}
          </span>
        </div>
      </div>

      <svg v-if="poles.length" class="pole-map" viewBox="0 0 920 195" preserveAspectRatio="xMidYMid meet">
        <!-- 路面 -->
        <rect x="20" y="78" width="880" height="44" rx="8" fill="#F3EADA" stroke="#E3D3AE" />
        <line x1="34" y1="100" x2="886" y2="100" stroke="#C9B48A" stroke-width="2" stroke-dasharray="14 12" />
        <text x="20" y="72" class="road-tag">{{ road ? road.name : '' }}</text>

        <g v-for="p in poles" :key="p.lamp.id" class="pole" @click="openPole(p.lamp)">
          <!-- 亮灯的灯具带一圈光晕 -->
          <circle v-if="p.lamp.status === '亮灯'" :cx="p.x" :cy="p.cy" r="17" :fill="p.color" opacity="0.18" />
          <line :x1="p.x" :y1="p.poleY1" :x2="p.x" :y2="p.poleY2" stroke="#A89877" stroke-width="3" />
          <circle :cx="p.x" :cy="p.cy" r="10" :fill="p.color" stroke="#fff" stroke-width="2" />
          <text :x="p.x" :y="p.labelY" class="pole-no" text-anchor="middle">{{ p.lamp.poleNo }}</text>
        </g>
      </svg>

      <el-empty v-else description="该路段暂无灯具，点右上角「新增灯具」挂杆" />
      <div class="tip">提示：点击灯杆圆点查看灯具详情并直接报修</div>
    </el-card>

    <!-- 灯具详情抽屉 -->
    <el-drawer v-model="drawer" :title="current ? `灯具 ${current.lampNo}` : '灯具详情'" size="340px">
      <template v-if="current">
        <div class="detail">
          <div class="row"><span>杆号</span><b>{{ current.poleNo }}</b></div>
          <div class="row"><span>类型</span><b>{{ current.type }}</b></div>
          <div class="row"><span>功率</span><b>{{ current.watt }} W</b></div>
          <div class="row"><span>安装日期</span><b>{{ current.installDate || '-' }}</b></div>
          <div class="row"><span>状态</span>
            <el-tag size="small" :style="{ background: colorOf(current.status), borderColor: colorOf(current.status), color: '#fff' }">
              {{ current.status }}
            </el-tag>
          </div>
        </div>
        <div class="drawer-actions">
          <el-button
            v-for="s in lampNext(current.status)"
            :key="s"
            type="warning"
            plain
            @click="shift(current, s)"
          >转·{{ s }}</el-button>
          <el-button
            type="danger"
            :disabled="current.status !== '故障'"
            @click="openRepair(current)"
          >报修</el-button>
          <el-button type="primary" plain @click="openEditLamp(current)">编辑</el-button>
        </div>
        <div v-if="current.status === '报废'" class="final-tip">报废为终态，不可再修改</div>
        <div v-if="current.status !== '故障'" class="final-tip">只有「故障」灯具可以报修</div>
      </template>
    </el-drawer>

    <!-- 新增 / 编辑灯具 -->
    <el-dialog v-model="lampDialog" :title="editingLamp ? '编辑灯具' : '新增灯具'" width="420px">
      <el-form :model="lampForm" label-width="90px">
        <el-form-item label="灯具编号"><el-input v-model="lampForm.lampNo" placeholder="如 L-0020" /></el-form-item>
        <el-form-item label="所属路段">
          <el-select v-model="lampForm.roadId">
            <el-option v-for="r in roads" :key="r.id" :label="`${r.name}（${r.status}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="杆号"><el-input v-model="lampForm.poleNo" placeholder="如 007" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="lampForm.type">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="功率（W）"><el-input-number v-model="lampForm.watt" :min="1" /></el-form-item>
        <el-form-item label="安装日期">
          <el-date-picker v-model="lampForm.installDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="lampForm.status">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lampDialog = false">取消</el-button>
        <el-button type="primary" @click="saveLamp">保存</el-button>
      </template>
    </el-dialog>

    <!-- 报修 -->
    <el-dialog v-model="repairDialog" title="开报修工单" width="380px">
      <el-form :model="repairForm" label-width="90px">
        <el-form-item label="故障类型">
          <el-select v-model="repairForm.faultType">
            <el-option v-for="t in faultTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="报修日期">
          <el-date-picker v-model="repairForm.reportDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" />
        </el-form-item>
        <el-form-item label="维修人员"><el-input v-model="repairForm.worker" placeholder="可留空，待派工" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repairDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRepair">开单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.bar h2 { margin: 0; font-size: 18px; }
.bar-right { display: flex; align-items: center; gap: 8px; }
.label { font-size: 13px; color: var(--muted); }
.road-select { width: 200px; }

.map-card { margin-bottom: 4px; }
.map-head { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; flex-wrap: wrap; }
.road-title { font-size: 15px; font-weight: 600; }
.legend { margin-left: auto; display: flex; gap: 12px; font-size: 12px; color: var(--muted); }
.legend span { display: flex; align-items: center; gap: 4px; }
.legend i { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }

.pole-map { width: 100%; height: auto; }
.pole { cursor: pointer; }
.pole:hover circle { stroke: var(--primary); stroke-width: 3; }
.pole-no { font-size: 11px; fill: var(--muted); }
.road-tag { font-size: 11px; fill: var(--muted); }
.tip { font-size: 12px; color: var(--muted); padding-top: 4px; }

.detail .row { display: flex; justify-content: space-between; padding: 6px 0; color: var(--muted); }
.detail .row b { color: var(--ink); font-weight: 600; }
.drawer-actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 16px; }
.final-tip { margin-top: 12px; font-size: 12px; color: var(--muted); }
</style>
