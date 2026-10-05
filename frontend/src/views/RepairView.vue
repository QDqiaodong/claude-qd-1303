<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const zoneFilter = inject('zoneFilter', ref(''))

const list = ref([])
const lamps = ref([])
const dialog = ref(false)
const editing = ref(null)
const form = ref({})
const faultTypes = ['不亮', '闪烁', '破损', '线路故障']
const statuses = ['待派工', '维修中', '已修复', '无法修复']

const assignDialog = ref(false)
const finishDialog = ref(false)
const actingId = ref(null)
const assignWorker = ref('')
const finishForm = ref({})

const emit = defineEmits(['changed'])

const lampMap = computed(() => Object.fromEntries(lamps.value.map(l => [l.id, l])))
const lampLabel = (id) => {
  const l = lampMap.value[id]
  return l ? `${l.lampNo}（${l.status}）` : `灯具#${id}`
}
// 只能给「故障」状态的灯具报修
const faultLamps = computed(() => lamps.value.filter(l => l.status === '故障'))

async function load() {
  const [ls, rs] = await Promise.all([
    api.listLamps(null, zoneFilter.value),
    api.listRepairs(null, zoneFilter.value)
  ])
  lamps.value = ls
  list.value = rs
}

function openCreate() {
  editing.value = null
  form.value = {
    lampId: faultLamps.value[0] ? faultLamps.value[0].id : null,
    faultType: '不亮',
    reportDate: null,
    worker: '',
    status: '待派工'
  }
  dialog.value = true
}
function openEdit(r) {
  editing.value = r.id
  form.value = { ...r }
  dialog.value = true
}

async function save() {
  const payload = {
    lampId: Number(form.value.lampId),
    faultType: form.value.faultType,
    reportDate: form.value.reportDate || null,
    worker: form.value.worker || '',
    status: form.value.status
  }
  try {
    if (editing.value) {
      await api.updateRepair(editing.value, payload)
      ElMessage.success('已更新工单')
    } else {
      await api.createRepair(payload)
      ElMessage.success('报修工单已开出')
    }
    dialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

function openAssign(r) {
  actingId.value = r.id
  assignWorker.value = r.worker || ''
  assignDialog.value = true
}
async function submitAssign() {
  try {
    await api.assignRepair(actingId.value, assignWorker.value)
    ElMessage.success('已派工，工单进入维修中')
    assignDialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

function openFinish(r) {
  actingId.value = r.id
  finishForm.value = { status: '已修复', finishDate: null, cost: null, worker: r.worker || '' }
  finishDialog.value = true
}
async function submitFinish() {
  try {
    await api.finishRepair(actingId.value, {
      status: finishForm.value.status,
      finishDate: finishForm.value.finishDate || null,
      cost: finishForm.value.cost === null || finishForm.value.cost === '' ? null : Number(finishForm.value.cost),
      worker: finishForm.value.worker || ''
    })
    ElMessage.success('完工已登记，灯具状态同步更新')
    finishDialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

const isFinal = (s) => s === '已修复' || s === '无法修复'
const statusType = (s) => (s === '已修复' ? 'success' : s === '无法修复' ? 'info' : s === '维修中' ? 'warning' : 'danger')

onMounted(load)
watch(zoneFilter, load)
</script>

<template>
  <div>
    <div class="bar">
      <h2>报修工单</h2>
      <el-button type="primary" @click="openCreate">开报修工单</el-button>
    </div>

    <el-table :data="list" border stripe size="small">
      <el-table-column prop="id" label="工单号" width="80" />
      <el-table-column label="报修灯具" width="180">
        <template #default="{ row }">{{ lampLabel(row.lampId) }}</template>
      </el-table-column>
      <el-table-column prop="faultType" label="故障类型" width="100" />
      <el-table-column prop="reportDate" label="报修日期" width="120" />
      <el-table-column prop="worker" label="维修人员" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="finishDate" label="完工日期" width="120" />
      <el-table-column prop="cost" label="费用（元）" width="110" />
      <el-table-column label="操作" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="isFinal(row.status)" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" :disabled="row.status !== '待派工'" @click="openAssign(row)">派工</el-button>
          <el-button link type="success" :disabled="isFinal(row.status)" @click="openFinish(row)">完工登记</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑工单' : '开报修工单'" width="420px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="报修灯具">
          <el-select v-model="form.lampId">
            <el-option v-for="l in faultLamps" :key="l.id" :label="lampLabel(l.id)" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障类型">
          <el-select v-model="form.faultType">
            <el-option v-for="t in faultTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="报修日期">
          <el-date-picker v-model="form.reportDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" />
        </el-form-item>
        <el-form-item label="维修人员"><el-input v-model="form.worker" placeholder="可留空，待派工" /></el-form-item>
        <el-form-item v-if="editing" label="工单状态">
          <el-select v-model="form.status">
            <el-option v-for="s in statuses.filter(s => !isFinal(s))" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <div class="hint">只能给「故障」状态的灯具报修；已修复 / 无法修复为终态不可再改</div>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialog" title="派工" width="360px">
      <el-form label-width="90px">
        <el-form-item label="维修人员"><el-input v-model="assignWorker" placeholder="必填" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">派工</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="finishDialog" title="完工登记" width="380px">
      <el-form :model="finishForm" label-width="100px">
        <el-form-item label="完工结论">
          <el-select v-model="finishForm.status">
            <el-option label="已修复" value="已修复" />
            <el-option label="无法修复" value="无法修复" />
          </el-select>
        </el-form-item>
        <el-form-item label="完工日期">
          <el-date-picker v-model="finishForm.finishDate" type="date" value-format="YYYY-MM-DD" placeholder="已修复必填" />
        </el-form-item>
        <el-form-item label="维修费用"><el-input-number v-model="finishForm.cost" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="维修人员"><el-input v-model="finishForm.worker" /></el-form-item>
      </el-form>
      <div class="hint">已修复：灯具回到「亮灯」；无法修复：灯具置「报废」。已修复必须填完工日期且不早于报修日期</div>
      <template #footer>
        <el-button @click="finishDialog = false">取消</el-button>
        <el-button type="primary" @click="submitFinish">登记完工</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.bar h2 { margin: 0; font-size: 18px; }
.hint { font-size: 12px; color: var(--muted); }
</style>
