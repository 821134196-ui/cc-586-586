<script>
  import { api, fmtTime, HANDLING_LABELS } from '../../api.js'

  let records = $state([])
  let error = $state('')

  async function load() {
    try {
      records = await api.handlingRecords()
    } catch (e) {
      error = e.message
    }
  }

  load()
</script>

{#if error}<div class="alert err">{error}</div>{/if}

<div class="card" style="overflow-x:auto">
  <h2>处理记录（逾期 / 破损）</h2>
  {#if records.length === 0}
    <span class="muted">暂无处理记录</span>
  {:else}
    <table>
      <thead>
        <tr><th>时间</th><th>类型</th><th>编号</th><th>书名</th><th>说明</th><th>记录人</th></tr>
      </thead>
      <tbody>
        {#each records as r}
          <tr>
            <td>{fmtTime(r.createdAt)}</td>
            <td>
              <span class="badge {r.type === 'DAMAGE' ? 'overdue' : 'borrowed'}">
                {HANDLING_LABELS[r.type] || r.type}
              </span>
            </td>
            <td class="bookcode">{r.bookCode}</td>
            <td>《{r.bookTitle}》</td>
            <td>{r.note}</td>
            <td>{r.handledBy || '—'}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</div>
