<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '@/services/http.js'

defineProps({ data: Object })

const fId      = ref('')
const fAccount = ref('')
const fChannel = ref('')
const fStatus  = ref('')

const loading  = ref(false)
const rows     = ref([])
const total    = ref(0)
const pageNum  = ref(1)
const pageSize = 20

const selectedIds = ref([])

const selectedAmount = computed(() => {
  let sum = 0
  rows.value.forEach(r => {
    if (selectedIds.value.includes(r.id)) sum += parseFloat(r.amount) || 0
  })
  return sum
})

const allSelected = computed({
  get: () => rows.value.length > 0 && rows.value.every(r => selectedIds.value.includes(r.id)),
  set: (v) => { selectedIds.value = v ? rows.value.map(r => r.id) : [] }
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function fetchData() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (fId.value.trim())      params.id           = fId.value.trim()
    if (fAccount.value.trim()) params.account      = fAccount.value.trim()
    if (fChannel.value.trim()) params.channelTitle = fChannel.value.trim()
    if (fStatus.value !== '')  params.status       = parseInt(fStatus.value)
    const res = await http.get('/admin/bill/page', { params })
    rows.value  = res.data?.data?.records ?? []
    total.value = res.data?.data?.total   ?? 0
  } catch (e) {
    console.error('bill page error', e)
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  selectedIds.value = []
  fetchData()
}

function reset() {
  fId.value = fAccount.value = fChannel.value = fStatus.value = ''
  search()
}

function toggleRow(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx === -1) selectedIds.value.push(id)
  else selectedIds.value.splice(idx, 1)
}

async function downloadBill(row) {
  try {
    const res = await http.get('/admin/bill/download', {
      params: { billId: row.id },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = `bill_${row.id}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('download failed', e)
  }
}

function changePage(p) {
  if (p < 1 || p > totalPages.value) return
  pageNum.value = p
  fetchData()
}

function fmt(v) {
  if (v == null) return '—'
  return '¥' + Number(v).toFixed(2)
}

function fmtDate(v) {
  if (!v) return '—'
  const d = new Date(v)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(fetchData)
</script>

<template>
  <div class="bill-view">
    <!-- Filter bar -->
    <div class="filter-bar panel">
      <input v-model="fId"      class="f-input" placeholder="输入ID" @keyup.enter="search" />
      <input v-model="fAccount" class="f-input" placeholder="商户" @keyup.enter="search" />
      <input v-model="fChannel" class="f-input" placeholder="支付通道" @keyup.enter="search" />
      <select v-model="fStatus" class="f-select">
        <option value="">全部状态</option>
        <option value="1">Success</option>
        <option value="0">Fail</option>
      </select>
      <button class="btn-primary" @click="search">查询</button>
      <button class="btn-ghost"   @click="reset">重置</button>
    </div>

    <!-- Table card -->
    <div class="table-wrap panel">
      <div v-if="loading" class="loading-mask">加载中...</div>

      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th class="col-check">
                <input type="checkbox" :checked="allSelected" @change="allSelected = $event.target.checked" />
              </th>
              <th>ID</th>
              <th>商户</th>
              <th>支付通道</th>
              <th>金额</th>
              <th>状态</th>
              <th>实际结算时间</th>
              <th>创建时间</th>
              <th>订单ID</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!rows.length && !loading">
              <td colspan="10" class="empty">暂无数据</td>
            </tr>
            <tr
              v-for="row in rows"
              :key="row.id"
              :class="{ selected: selectedIds.includes(row.id) }"
            >
              <td class="col-check">
                <input type="checkbox" :checked="selectedIds.includes(row.id)" @change="toggleRow(row.id)" />
              </td>
              <td>{{ row.id }}</td>
              <td>{{ row.account ?? '—' }}</td>
              <td>{{ row.channelTitle ?? '—' }}</td>
              <td class="amount">{{ fmt(row.amount) }}</td>
              <td>
                <span :class="['badge', row.status === 1 ? 'badge-success' : 'badge-fail']">
                  {{ row.status === 1 ? 'Success' : 'Fail' }}
                </span>
              </td>
              <td>{{ fmtDate(row.settleAt) }}</td>
              <td>{{ fmtDate(row.createdAt) }}</td>
              <td class="order-ids" :title="row.orderIds">{{ row.orderIds || '—' }}</td>
              <td class="col-action">
                <button class="icon-btn" title="下载" @click="downloadBill(row)">⬇</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Bottom bar -->
      <div class="bottom-bar">
        <div class="selection-info">
          <template v-if="selectedIds.length > 0">
            <span>已选择 <b>{{ selectedIds.length }}</b> 条，金额合计：<b class="amount-sum">{{ '¥' + selectedAmount.toFixed(2) }}</b></span>
            <button class="btn-primary ml">结算</button>
          </template>
          <span v-else class="muted">共 {{ total }} 条记录</span>
        </div>
        <div class="pagination">
          <button class="page-btn" :disabled="pageNum <= 1" @click="changePage(pageNum - 1)">‹</button>
          <span class="page-info">{{ pageNum }} / {{ totalPages }}</span>
          <button class="page-btn" :disabled="pageNum >= totalPages" @click="changePage(pageNum + 1)">›</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bill-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  border-radius: 16px;
  padding: 20px 24px;
  background: rgba(18, 19, 37, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

/* Filter bar */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.f-input,
.f-select {
  height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
  font-size: 13px;
  outline: none;
  min-width: 140px;
}

.f-select option {
  background: #1a1b2e;
  color: #fff;
}

.f-input::placeholder { color: rgba(255,255,255,0.35); }

.btn-primary {
  height: 36px;
  padding: 0 20px;
  border-radius: 8px;
  border: none;
  background: #4a5fff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}
.btn-primary:hover { background: #5c70ff; }

.btn-ghost {
  height: 36px;
  padding: 0 20px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: transparent;
  color: rgba(255,255,255,0.7);
  font-size: 13px;
  cursor: pointer;
}
.btn-ghost:hover { background: rgba(255,255,255,0.05); }

/* Table */
.table-wrap { position: relative; }

.loading-mask {
  position: absolute;
  inset: 0;
  background: rgba(10,10,20,0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255,255,255,0.7);
  font-size: 14px;
  border-radius: 16px;
  z-index: 10;
}

.table-scroll { overflow-x: auto; }

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

thead th {
  text-align: left;
  padding: 10px 12px;
  color: rgba(255,255,255,0.5);
  font-weight: 500;
  border-bottom: 1px solid rgba(255,255,255,0.07);
  white-space: nowrap;
}

tbody td {
  padding: 12px 12px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  color: rgba(255,255,255,0.85);
  vertical-align: middle;
}

tr.selected td { background: rgba(74,95,255,0.08); }
tbody tr:hover td { background: rgba(255,255,255,0.02); }

.col-check { width: 40px; }
.col-action { width: 48px; }

.amount { font-variant-numeric: tabular-nums; }

.order-ids {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgba(255,255,255,0.5);
  font-size: 12px;
}

.empty {
  text-align: center;
  padding: 48px 0;
  color: rgba(255,255,255,0.3);
}

/* Badge */
.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}
.badge-success { background: rgba(52,199,89,0.15); color: #34c759; }
.badge-fail    { background: rgba(255,69,58,0.15);  color: #ff453a; }

/* Icon btn */
.icon-btn {
  background: transparent;
  border: none;
  color: rgba(255,255,255,0.45);
  cursor: pointer;
  font-size: 14px;
  padding: 4px 6px;
  border-radius: 6px;
}
.icon-btn:hover { color: #fff; background: rgba(255,255,255,0.08); }

/* Bottom bar */
.bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-wrap: wrap;
  gap: 12px;
}

.selection-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: rgba(255,255,255,0.75);
}

.amount-sum { color: #4a5fff; }

.muted { color: rgba(255,255,255,0.4); }

.ml { margin-left: 4px; }

.pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.12);
  background: transparent;
  color: rgba(255,255,255,0.7);
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.page-btn:disabled { opacity: 0.3; cursor: default; }
.page-btn:not(:disabled):hover { background: rgba(255,255,255,0.07); }

.page-info {
  font-size: 13px;
  color: rgba(255,255,255,0.5);
  min-width: 52px;
  text-align: center;
}
</style>
