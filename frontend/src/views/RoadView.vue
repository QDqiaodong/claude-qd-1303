<script setup>
import { inject, onMounted, ref, watch } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const zoneFilter = inject('zoneFilter', ref(''))

const list = ref([])
const dialog = ref(false)
const editing = ref(null)
const form = ref({})
const zones = ['城东', '城西', '城南', '城北', '开发区']
const statuses = ['正常', '施工', '停用']

// 路段状态机：正常↔施工；停用只能恢复为正常
const nextStatus = {
  '正常': ['施工', '停用'],
  '施工': ['正常', '停用'],
  '停用': ['正常']
}

const emit = defineEmits(['changed'])

function blank() {
  return { roadNo: '', name: '', zone: '城东', lengthMeter: null, lampCount: 0, status: '正常' }
}

async function load() {
  list.value = await api.listRoads(zoneFilter.value)
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
    roadNo: form.value.roadNo,
    name: form.value.name,
    zone: form.value.zone,
    lengthMeter: Number(form.value.lengthMeter),
    lampCount: Number(form.value.lampCount || 0),
    status: form.value.status
  }
  try {
    if (editing.value) {
      await api.updateRoad(editing.value, payload)
      ElMessage.success('已更新路段')
    } else {
      await api.createRoad(payload)
      ElMessage.success('已登记路段')
    }
    dialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

async function shift(r, status) {
  try {
    await api.changeRoadStatus(r.id, status)
    ElMessage.success(`已流转至「${status}」`)
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

async function syncCount(r) {
  try {
    await api.syncRoadLampCount(r.id)
    ElMessage.success('灯杆数已按实际灯具数校准')
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

const statusType = (s) => (s === '正常' ? 'success' : s === '施工' ? 'warning' : 'info')

onMounted(load)
watch(zoneFilter, load)
</script>

<template>
  <div>
    <div class="bar">
      <h2>路段</h2>
      <el-button type="primary" @click="openCreate">登记路段</el-button>
    </div>

    <el-table :data="list" border stripe size="small">
      <el-table-column prop="roadNo" label="路段编号" width="130" />
      <el-table-column prop="name" label="路段名" width="140" />
      <el-table-column prop="zone" label="片区" width="90" />
      <el-table-column prop="lengthMeter" label="长度（米）" width="110" />
      <el-table-column prop="lampCount" label="灯杆数" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-for="s in nextStatus[row.status] || []"
            :key="s"
            link
            type="warning"
            @click="shift(row, s)"
          >转·{{ s }}</el-button>
          <el-button link type="info" @click="syncCount(row)">校准灯杆数</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑路段' : '登记路段'" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="路段编号"><el-input v-model="form.roadNo" placeholder="如 RD-2026-007" /></el-form-item>
        <el-form-item label="路段名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="片区">
          <el-select v-model="form.zone">
            <el-option v-for="z in zones" :key="z" :label="z" :value="z" />
          </el-select>
        </el-form-item>
        <el-form-item label="长度（米）"><el-input-number v-model="form.lengthMeter" :min="1" :precision="2" /></el-form-item>
        <el-form-item label="灯杆数"><el-input-number v-model="form.lampCount" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
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
</style>
