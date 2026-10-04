<script>
  import { onMount } from 'svelte'
  import {
    get, post, uploadCover,
    STATUS_LABELS, CONDITION_LABELS, INCIDENT_LABELS, fmtTime, fmtDate
  } from '../api.js'

  const tabs = ['捐赠审核', '借阅记录', '处理记录', '箱位管理', '全部图书']
  let tab = '捐赠审核'

  let boxes = []
  let donations = []
  let loans = []
  let showAllLoans = false
  let incidents = []
  let copies = []
  let error = ''
  let notice = ''

  // 捐赠登记表单
  let form = { title: '', author: '', isbn: '', condition: 'GOOD', donorName: '', anonymous: false, note: '' }
  // 审核时选择的箱
  let approveBox = {}
  // 重新上架选择的箱
  let reshelveBox = {}

  onMount(refresh)

  async function refresh() {
    error = ''
    try {
      boxes = await get('/api/admin/boxes')
      donations = await get('/api/admin/donations')
      await loadLoans()
      incidents = await get('/api/admin/incidents')
      copies = await get('/api/admin/copies')
    } catch (e) {
      error = e.message
    }
  }

  async function loadLoans() {
    loans = await get('/api/admin/loans' + (showAllLoans ? '?all=true' : ''))
  }

  function flash(msg) {
    notice = msg
    setTimeout(() => (notice = ''), 4000)
  }

  async function run(action, okMsg) {
    error = ''
    try {
      await action()
      await refresh()
      if (okMsg) flash(okMsg)
    } catch (e) {
      error = e.message
    }
  }

  // ---------- 捐赠 ----------

  function registerDonation() {
    return run(async () => {
      await post('/api/admin/donations', {
        title: form.title, author: form.author || null, isbn: form.isbn || null,
        condition: form.condition,
        donorName: form.anonymous ? null : (form.donorName || null),
        anonymous: form.anonymous, note: form.note || null
      })
      form = { title: '', author: '', isbn: '', condition: 'GOOD', donorName: '', anonymous: false, note: '' }
    }, '捐赠已登记，等待审核')
  }

  const approve = (d) => run(async () => {
    await post(`/api/admin/donations/${d.id}/approve`, { boxCode: approveBox[d.id] })
  }, '已上架')

  const reject = (d) => run(async () => {
    const note = prompt('驳回原因（可留空）') ?? ''
    await post(`/api/admin/donations/${d.id}/reject`, { note })
  }, '已驳回')

  // ---------- 处理记录 ----------

  const resolve = (r) => run(async () => {
    const note = prompt('处理说明（可留空）') ?? ''
    await post(`/api/admin/incidents/${r.id}/resolve`, { note })
  }, '已结案')

  const scanOverdue = () => run(async () => {
    const r = await post('/api/admin/reminders/run', {})
    flash(`扫描完成，新建 ${r.created} 条逾期记录并已发送模拟提醒`)
  })

  // ---------- 箱 ----------

  let boxForm = { code: '', name: '', location: '', capacity: 5 }
  const createBox = () => run(async () => {
    await post('/api/admin/boxes', { ...boxForm, capacity: Number(boxForm.capacity) })
    boxForm = { code: '', name: '', location: '', capacity: 5 }
  }, '箱位已创建')

  // ---------- 图书 ----------

  const reshelve = (c) => run(async () => {
    await post(`/api/admin/copies/${c.copyCode}/reshelve`, { boxCode: reshelveBox[c.copyCode] })
  }, '已重新上架')

  const pickCover = (c, ev) => run(async () => {
    const file = ev.target.files?.[0]
    if (file) await uploadCover(c.copyCode, file)
  }, '封面已上传')

  $: pending = donations.filter(d => d.status === 'PENDING')
  $: reviewed = donations.filter(d => d.status !== 'PENDING')
</script>

<div class="tabs">
  {#each tabs as t}
    <button class:active={tab === t} on:click={() => (tab = t)}>{t}</button>
  {/each}
</div>

{#if error}<div class="error-box">{error}</div>{/if}
{#if notice}<div class="success-box">{notice}</div>{/if}

{#if tab === '捐赠审核'}
  <div class="card">
    <h2>登记捐赠</h2>
    <form class="grid" on:submit|preventDefault={registerDonation}>
      <label>书名 *<input bind:value={form.title} required /></label>
      <label>作者<input bind:value={form.author} /></label>
      <label>ISBN<input bind:value={form.isbn} /></label>
      <label>书况
        <select bind:value={form.condition}>
          {#each Object.entries(CONDITION_LABELS) as [k, v]}<option value={k}>{v}</option>{/each}
        </select>
      </label>
      <label>捐赠者姓名<input bind:value={form.donorName} disabled={form.anonymous} /></label>
      <label>备注<input bind:value={form.note} /></label>
      <label style="flex-direction:row;align-items:center;gap:0.4rem">
        <input type="checkbox" bind:checked={form.anonymous} style="width:auto" /> 匿名捐赠
      </label>
      <label>&nbsp;<button type="submit" disabled={!form.title.trim()}>登记</button></label>
    </form>
  </div>

  <div class="card">
    <h2>待审核（{pending.length}）</h2>
    {#if pending.length === 0}<div class="muted">暂无待审核捐赠。</div>{/if}
    {#each pending as d}
      <div class="row" style="border-bottom:1px solid var(--line);padding:0.5rem 0">
        <div class="grow">
          <code>{d.copyCode}</code> 《{d.title}》
          <span class="muted">ISBN {d.isbn ?? '—'} · {CONDITION_LABELS[d.condition]} · {d.donorName}{d.note ? ` · ${d.note}` : ''}</span>
        </div>
        <select bind:value={approveBox[d.id]}>
          <option value="" disabled selected>选择箱位</option>
          {#each boxes as b}
            <option value={b.code} disabled={b.occupied >= b.capacity}>
              {b.code} {b.occupied}/{b.capacity}{b.occupied >= b.capacity ? ' 已满' : ''}
            </option>
          {/each}
        </select>
        <button on:click={() => approve(d)} disabled={!approveBox[d.id]}>通过并上架</button>
        <button class="ghost" on:click={() => reject(d)}>驳回</button>
      </div>
    {/each}
  </div>

  {#if reviewed.length > 0}
    <div class="card">
      <h2>已处理捐赠</h2>
      <table>
        <thead><tr><th>编号</th><th>书名</th><th>捐赠者</th><th>结果</th><th>时间</th></tr></thead>
        <tbody>
          {#each reviewed as d}
            <tr>
              <td><code>{d.copyCode}</code></td><td>{d.title}</td><td>{d.donorName}</td>
              <td><span class="tag" class:ok={d.status === 'APPROVED'} class:bad={d.status === 'REJECTED'}>
                {d.status === 'APPROVED' ? '已上架' : '已驳回'}</span></td>
              <td>{fmtTime(d.createdAt)}</td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}

{:else if tab === '借阅记录'}
  <div class="card">
    <div class="row">
      <h2 class="grow">借阅记录{showAllLoans ? '（全部）' : '（在借）'}</h2>
      <label class="muted"><input type="checkbox" bind:checked={showAllLoans} on:change={loadLoans} style="width:auto" /> 含已归还</label>
      <button class="secondary" on:click={scanOverdue}>扫描逾期并提醒</button>
    </div>
    <table>
      <thead><tr><th>编号</th><th>书名</th><th>借书人</th><th>借出时间</th><th>应还</th><th>归还</th><th>还到箱</th></tr></thead>
      <tbody>
        {#each loans as l}
          <tr>
            <td><code>{l.copyCode}</code></td><td>{l.title}</td>
            <td>{l.borrowerName}<div class="muted">{l.borrowerContact ?? ''}</div></td>
            <td>{fmtTime(l.borrowedAt)}</td>
            <td>
              {#if l.overdue}<span class="tag bad">逾期</span>{/if}
              {fmtDate(l.dueAt)}
            </td>
            <td>{l.returnedAt ? fmtTime(l.returnedAt) : '—'}</td>
            <td>{l.returnBoxCode ?? '—'}</td>
          </tr>
        {/each}
      </tbody>
    </table>
    {#if loans.length === 0}<div class="muted">暂无记录。</div>{/if}
  </div>

{:else if tab === '处理记录'}
  <div class="card">
    <h2>逾期 / 破损处理记录</h2>
    <table>
      <thead><tr><th>类型</th><th>编号</th><th>书名</th><th>借书人</th><th>说明</th><th>时间</th><th>状态</th><th></th></tr></thead>
      <tbody>
        {#each incidents as r}
          <tr>
            <td><span class="tag" class:warn={r.type === 'OVERDUE'} class:bad={r.type === 'DAMAGE'}>{INCIDENT_LABELS[r.type]}</span></td>
            <td><code>{r.copyCode}</code></td><td>{r.title}</td><td>{r.borrowerName ?? '—'}</td>
            <td>{r.note}{r.handleNote ? `<div class="muted">处理：${r.handleNote}</div>` : ''}</td>
            <td>{fmtTime(r.createdAt)}</td>
            <td>{r.resolved ? '已结案' : '待处理'}</td>
            <td>{#if !r.resolved}<button class="ghost" on:click={() => resolve(r)}>结案</button>{/if}</td>
          </tr>
        {/each}
      </tbody>
    </table>
    {#if incidents.length === 0}<div class="muted">暂无处理记录。</div>{/if}
  </div>

{:else if tab === '箱位管理'}
  <div class="card">
    <h2>箱位</h2>
    <table>
      <thead><tr><th>编号</th><th>名称</th><th>位置</th><th>占用</th></tr></thead>
      <tbody>
        {#each boxes as b}
          <tr>
            <td><code>{b.code}</code></td><td>{b.name}</td><td>{b.location ?? '—'}</td>
            <td>
              <span class="tag" class:ok={b.occupied < b.capacity} class:bad={b.occupied >= b.capacity}>
                {b.occupied}/{b.capacity}{b.occupied >= b.capacity ? ' 已满' : ''}
              </span>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
  <div class="card">
    <h3>新增箱位</h3>
    <form class="grid" on:submit|preventDefault={createBox}>
      <label>编号 *<input bind:value={boxForm.code} placeholder="BOX-D" required /></label>
      <label>名称 *<input bind:value={boxForm.name} required /></label>
      <label>位置<input bind:value={boxForm.location} /></label>
      <label>容量 *<input type="number" min="1" bind:value={boxForm.capacity} required /></label>
      <label>&nbsp;<button type="submit">创建</button></label>
    </form>
  </div>

{:else if tab === '全部图书'}
  <div class="card">
    <h2>全部实体书（{copies.length}）</h2>
    <table>
      <thead><tr><th>编号</th><th>书名</th><th>ISBN</th><th>状态</th><th>所在箱</th><th>封面</th><th>操作</th></tr></thead>
      <tbody>
        {#each copies as c}
          <tr>
            <td><code>{c.copyCode}</code></td>
            <td>{c.title}</td>
            <td>{c.isbn ?? '—'}</td>
            <td><span class="tag" class:ok={c.status === 'ON_SHELF'} class:warn={c.status === 'BORROWED'} class:bad={c.status === 'DAMAGED'}>{STATUS_LABELS[c.status]}</span></td>
            <td>{c.boxCode ?? '—'}</td>
            <td>
              {#if c.coverUrl}<img src={c.coverUrl} alt="" style="height:40px;border-radius:3px" />{/if}
              <label class="muted" style="cursor:pointer">
                上传<input type="file" accept="image/*" style="display:none" on:change={(e) => pickCover(c, e)} />
              </label>
            </td>
            <td>
              {#if c.status === 'DAMAGED'}
                <div class="row">
                  <select bind:value={reshelveBox[c.copyCode]}>
                    <option value="" disabled selected>箱位</option>
                    {#each boxes as b}
                      <option value={b.code} disabled={b.occupied >= b.capacity}>{b.code} {b.occupied}/{b.capacity}</option>
                    {/each}
                  </select>
                  <button class="ghost" on:click={() => reshelve(c)} disabled={!reshelveBox[c.copyCode]}>重新上架</button>
                </div>
              {/if}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
{/if}
