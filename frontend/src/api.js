/** 后端 API 封装：统一错误处理，后端返回 {error} 时抛出其消息 */
async function req(method, url, body) {
  const opts = { method, headers: {} }
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json'
    opts.body = JSON.stringify(body)
  }
  const resp = await fetch(url, opts)
  if (!resp.ok) {
    let msg = `请求失败 (${resp.status})`
    try {
      const j = await resp.json()
      if (j.error) msg = j.error
    } catch { /* 保留默认消息 */ }
    throw new Error(msg)
  }
  return resp.json()
}

export const get = (url) => req('GET', url)
export const post = (url, body) => req('POST', url, body)

export async function uploadCover(copyCode, file) {
  const form = new FormData()
  form.append('file', file)
  const resp = await fetch(`/api/admin/copies/${copyCode}/cover`, { method: 'POST', body: form })
  if (!resp.ok) {
    let msg = `上传失败 (${resp.status})`
    try {
      const j = await resp.json()
      if (j.error) msg = j.error
    } catch { /* 保留默认消息 */ }
    throw new Error(msg)
  }
  return resp.json()
}

export const STATUS_LABELS = {
  PENDING_REVIEW: '待审核',
  ON_SHELF: '在架',
  BORROWED: '已借出',
  DAMAGED: '破损待处理',
  RETIRED: '已下架'
}

export const CONDITION_LABELS = {
  NEW: '全新',
  GOOD: '良好',
  WORN: '磨损',
  DAMAGED: '破损'
}

export const MOVEMENT_LABELS = {
  DONATION_REGISTERED: '捐赠登记',
  SHELVED: '上架',
  BORROWED: '借出',
  RETURNED: '归还',
  DAMAGED: '破损标记',
  RESHELVED: '重新上架',
  RETIRED: '下架'
}

export const INCIDENT_LABELS = {
  OVERDUE: '逾期',
  DAMAGE: '破损'
}

export function fmtTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

export function fmtDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('zh-CN')
}
