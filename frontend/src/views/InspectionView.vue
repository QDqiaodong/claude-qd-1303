<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const zoneFilter = inject('zoneFilter', ref(''))

const list = ref([])
const roads = ref([])
const dialog = ref(false)
const editing = ref(null)
const form = ref({})
const statuses = ['正常', '有故障', '待复查']

const emit = defineEmits(['changed'])

const roadMap = computed(() => Object.fromEntries(roads.value.map(r => [r.id, r])))
const roadName = (id) => roadMap.value[id]?.name || `路段#${id}`
const roadLampCount = (id) => roadMap.value[id]?.lampCount ?? 0

// 可巡检路段：停用路段不可排巡检
const usableRoads = computed(() => roads.value.filter(r => r.status !== '停用'))

const total = computed(() => Number(form.value.litCount || 0) + Number(form.value.faultCount || 0))

async function load() {
  const [rs, is] = await Promise.all([
    api.listRoads(zoneFilter.value),
    api.listInspections(null, zoneFilter.value)
  ])
  roads.value = rs
  list.value = is
}

function blank() {
  const first = usableRoads.value[0]
  return {
    roadId: first ? first.id : null,
    inspector: '',
    checkDate: null,
    litCount: 0,
    faultCount: 0,
    status: '正常'
  }
}

function openCreate() {
  editing.value = null
  form.value = blank()
  dialog.value = true
}
function openEdit(r) {
  editing.value = r.id
  form.value = { ...r }
  dialog.value = true
}

async function save() {
  const payload = {
    roadId: Number(form.value.roadId),
    inspector: form.value.inspector,
    checkDate: form.value.checkDate || null,
    litCount: Number(form.value.litCount || 0),
    faultCount: Number(form.value.faultCount || 0),
    status: form.value.status
  }
  try {
    if (editing.value) {
      await api.updateInspection(editing.value, payload)
      ElMessage.success('已更新巡检记录')
    } else {
      await api.createInspection(payload)
      ElMessage.success('已登记巡检')
    }
    dialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

const statusType = (s) => (s === '正常' ? 'success' : s === '有故障' ? 'danger' : 'warning')

onMounted(load)
watch(zoneFilter, load)
</script>

<template>
  <div>
    <div class="bar">
      <h2>巡检记录</h2>
      <el-button type="primary" @click="openCreate">登记巡检</el-button>
    </div>

    <el-table :data="list" border stripe size="small">
      <el-table-column label="路段" width="150">
        <template #default="{ row }">{{ roadName(row.roadId) }}</template>
      </el-table-column>
      <el-table-column prop="inspector" label="巡检员" width="100" />
      <el-table-column prop="checkDate" label="巡检日期" width="120" />
      <el-table-column prop="litCount" label="亮灯数" width="90" />
      <el-table-column prop="faultCount" label="故障数" width="90" />
      <el-table-column label="结论" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑巡检记录' : '登记巡检'" width="420px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="路段">
          <el-select v-model="form.roadId">
            <el-option
              v-for="r in usableRoads"
              :key="r.id"
              :label="`${r.name}（${r.lampCount} 杆）`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="巡检员"><el-input v-model="form.inspector" placeholder="如 张巡检" /></el-form-item>
        <el-form-item label="巡检日期">
          <el-date-picker v-model="form.checkDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" />
        </el-form-item>
        <el-form-item label="亮灯数"><el-input-number v-model="form.litCount" :min="0" /></el-form-item>
        <el-form-item label="故障数"><el-input-number v-model="form.faultCount" :min="0" /></el-form-item>
        <el-form-item label="巡检结论">
          <el-select v-model="form.status">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="hint">
        合计 {{ total }} 盏 / 该路段灯杆总数 {{ roadLampCount(form.roadId) }} 盏；
        故障数大于 0 时结论必须是有故障或待复查
      </div>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.bar h2 { margin: 0; font-size: 18px; }
.hint { font-size: 12px; color: var(--muted); padding: 0 0 4px 0; }
</style>
