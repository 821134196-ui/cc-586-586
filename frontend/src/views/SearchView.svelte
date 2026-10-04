<script>
  import { get, STATUS_LABELS, CONDITION_LABELS } from '../api.js'
  import BookDetail from './BookDetail.svelte'

  let q = ''
  let results = []
  let error = ''
  let loading = false
  let selected = null

  async function search() {
    loading = true
    error = ''
    selected = null
    try {
      results = await get('/api/books/search?q=' + encodeURIComponent(q))
    } catch (e) {
      error = e.message
    } finally {
      loading = false
    }
  }

  search()
</script>

<div class="card">
  <h2>查书</h2>
  <div class="row">
    <input class="grow" bind:value={q} placeholder="输入实体编号 / 书名 / ISBN / 作者"
           on:keydown={(e) => e.key === 'Enter' && search()} />
    <button on:click={search} disabled={loading}>查询</button>
  </div>
  <div class="muted">每本实体书都有独立编号（如 BK-000001），同名同 ISBN 的书是不同的实物。</div>
  {#if error}<div class="error-box">{error}</div>{/if}
</div>

{#if results.length > 0}
  <div class="card">
    <table>
      <thead>
        <tr><th>编号</th><th>书名</th><th>作者</th><th>ISBN</th><th>书况</th><th>状态</th><th>所在箱</th></tr>
      </thead>
      <tbody>
        {#each results as b}
          <tr class="clickable" on:click={() => (selected = b.copyCode)}>
            <td><code>{b.copyCode}</code></td>
            <td>{b.title}</td>
            <td>{b.author ?? '—'}</td>
            <td>{b.isbn ?? '—'}</td>
            <td>{CONDITION_LABELS[b.condition]}</td>
            <td>
              <span class="tag" class:ok={b.status === 'ON_SHELF'} class:warn={b.status === 'BORROWED'}>
                {STATUS_LABELS[b.status]}
              </span>
            </td>
            <td>{b.boxCode ? `${b.boxName}（${b.boxCode}）` : '—'}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
{:else if !loading}
  <div class="card muted">没有匹配的图书。</div>
{/if}

{#if selected}
  <div class="card">
    <BookDetail copyCode={selected} />
  </div>
{/if}
