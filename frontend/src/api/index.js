import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error?.response?.data?.message || error?.message || '请求失败'
    return Promise.reject(new Error(message))
  }
)

async function call(promise) {
  const response = await promise
  const body = response.data
  if (!body || body.ok === false) {
    throw new Error((body && body.message) || '请求失败')
  }
  return body.data
}

// 去掉空值，避免把 undefined 拼进 query string
const params = (o) => ({
  params: Object.fromEntries(
    Object.entries(o).filter(([, v]) => v !== null && v !== undefined && v !== '')
  )
})

export default {
  // ---------------- 路段 road ----------------
  listRoads: (zone) => call(http.get('/roads', params({ zone }))),
  createRoad: (d) => call(http.post('/roads', d)),
  updateRoad: (id, d) => call(http.put(`/roads/${id}`, d)),
  changeRoadStatus: (id, status) => call(http.post(`/roads/${id}/status`, null, params({ status }))),
  syncRoadLampCount: (id) => call(http.post(`/roads/${id}/sync-lamp-count`)),

  // ---------------- 灯具 lamp ----------------
  listLamps: (roadId, zone) => call(http.get('/lamps', params({ roadId, zone }))),
  createLamp: (d) => call(http.post('/lamps', d)),
  updateLamp: (id, d) => call(http.put(`/lamps/${id}`, d)),
  changeLampStatus: (id, status) => call(http.post(`/lamps/${id}/status`, null, params({ status }))),

  // ---------------- 巡检记录 inspection ----------------
  listInspections: (roadId, zone) => call(http.get('/inspections', params({ roadId, zone }))),
  createInspection: (d) => call(http.post('/inspections', d)),
  updateInspection: (id, d) => call(http.put(`/inspections/${id}`, d)),

  // ---------------- 报修工单 repair ----------------
  listRepairs: (lampId, zone) => call(http.get('/repairs', params({ lampId, zone }))),
  createRepair: (d) => call(http.post('/repairs', d)),
  updateRepair: (id, d) => call(http.put(`/repairs/${id}`, d)),
  assignRepair: (id, worker) => call(http.post(`/repairs/${id}/assign`, null, params({ worker }))),
  finishRepair: (id, d) => call(http.post(`/repairs/${id}/finish`, d))
}
