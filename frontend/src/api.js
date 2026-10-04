const BASE = '/api'

async function request(path, options = {}) {
  const res = await fetch(BASE + path, {
    headers: options.body instanceof FormData ? undefined : { 'Content-Type': 'application/json' },
    ...options
  })
  if (!res.ok) {
    let message = `请求失败 (${res.status})`
    try {
      const data = await res.json()
      if (data.error) message = data.error
      else if (data.message) message = data.message
    } catch { /* 保留默认提示 */ }
    throw new Error(message)
  }
  return res.json()
}

export const api = {
  // 查询
  searchBooks: (q) => request(`/books?q=${encodeURIComponent(q)}`),
  getBook: (code) => request(`/books/${encodeURIComponent(code)}`),
  getBookAdmin: (code) => request(`/books/${encodeURIComponent(code)}/admin`),
  getHistory: (code) => request(`/books/${encodeURIComponent(code)}/history`),
  listBoxes: () => request('/boxes'),
  // 借还
  borrow: (payload) => request('/borrow', { method: 'POST', body: JSON.stringify(payload) }),
  returnBook: (payload) => request('/returns', { method: 'POST', body: JSON.stringify(payload) }),
  // 管理
  donate: (payload) => request('/donations', { method: 'POST', body: JSON.stringify(payload) }),
  pendingBooks: () => request('/books/pending'),
  shelve: (code, payload) => request(`/books/${encodeURIComponent(code)}/shelve`, { method: 'POST', body: JSON.stringify(payload) }),
  uploadCover: (code, file) => {
    const form = new FormData()
    form.append('file', file)
    return request(`/books/${encodeURIComponent(code)}/cover`, { method: 'POST', body: form })
  },
  listLoans: (activeOnly) => request(activeOnly ? '/loans?active=true' : '/loans'),
  remindOverdue: () => request('/admin/remind-overdue', { method: 'POST' }),
  handlingRecords: () => request('/admin/handling-records'),
  createBox: (payload) => request('/boxes', { method: 'POST', body: JSON.stringify(payload) })
}

export const CONDITION_LABELS = { NEW: '全新', GOOD: '良好', FAIR: '一般', POOR: '较差' }
export const STATUS_LABELS = {
  PENDING_INSPECTION: '待检查',
  ON_SHELF: '在架',
  BORROWED: '已借出',
  RETIRED: '已下架'
}
export const EVENT_LABELS = {
  DONATED: '捐赠登记',
  SHELVED: '上架',
  BORROWED: '借出',
  RETURNED: '归还',
  RETIRED: '下架'
}
export const HANDLING_LABELS = { OVERDUE: '逾期未还', DAMAGE: '破损' }

export function fmtTime(iso) {
  if (!iso) return '—'
  return iso.replace('T', ' ').slice(0, 16)
}
export function fmtDate(iso) {
  if (!iso) return '—'
  return iso.slice(0, 10)
}
