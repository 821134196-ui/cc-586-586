<script>
  import { api, fmtDate, fmtTime } from '../../api.js'

  let loans = $state([])
  let error = $state('')
  let message = $state('')
  let showAll = $state(false)

  async function load() {
    error = ''
    try {
      loans = await api.listLoans(!showAll)
    } catch (e) {
      error = e.message
    }
  }

  async function remind() {
    error = ''
    message = ''
    try {
      const res = await api.remindOverdue()
      message = res.reminded > 0
        ? `✅ 已发送 ${res.reminded} 条逾期提醒（模拟接口），并生成处理记录`
        : '当前没有需要提醒的逾期借阅'
      await load()
    } catch (e) {
      error = e.message
    }
  }

  load()
</script>

{#if message}<div class="alert ok">{message}</div>{/if}
{#if error}<div class="alert err">{error}</div>{/if}

<div class="card">
  <div class="row">
    <h2 class="grow" style="margin:0">借阅记录</h2>
    <label style="color:var(--ink)">
      <input type="checkbox" bind:checked={showAll} onchange={load} style="width:auto" /> 显示已归还
    </label>
    <button class="warn" onclick={remind}>检查逾期并发送提醒</button>
  </div>
</div>

<div class="card" style="overflow-x:auto">
  {#if loans.length === 0}
    <span class="muted">暂无借阅记录</span>
  {:else}
    <table>
      <thead>
        <tr>
          <th>编号</th><th>书名</th><th>借阅人</th><th>联系方式</th>
          <th>借出时间</th><th>应还日期</th><th>状态</th>
        </tr>
      </thead>
      <tbody>
        {#each loans as loan}
          <tr>
            <td class="bookcode">{loan.bookCode}</td>
            <td>《{loan.bookTitle}》</td>
            <td>{loan.borrowerName}</td>
            <td>{loan.borrowerContact || '—'}</td>
            <td>{fmtTime(loan.borrowedAt)}</td>
            <td>{fmtDate(loan.dueAt)}</td>
            <td>
              {#if loan.status === 'RETURNED'}
                <span class="badge shelf">已还 → {loan.returnBoxCode}</span>
              {:else if loan.overdue}
                <span class="badge overdue">逾期</span>
              {:else}
                <span class="badge borrowed">在借</span>
              {/if}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</div>
