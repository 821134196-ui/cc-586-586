<script>
  import { api } from '../api.js'
  import BookCard from '../components/BookCard.svelte'
  import HistoryTimeline from '../components/HistoryTimeline.svelte'

  let { initialCode = '' } = $props()

  let q = $state(initialCode)
  let results = $state([])
  let selected = $state(null)
  let history = $state([])
  let error = $state('')
  let searched = $state(false)

  async function search() {
    error = ''
    selected = null
    searched = true
    try {
      results = await api.searchBooks(q)
      // 输入精确命中某本实体书编号时直接展开详情
      const exact = results.find((b) => b.code.toLowerCase() === q.trim().toLowerCase())
      if (exact) await showDetail(exact)
    } catch (e) {
      error = e.message
      results = []
    }
  }

  async function showDetail(book) {
    error = ''
    try {
      selected = await api.getBook(book.code)
      history = await api.getHistory(book.code)
    } catch (e) {
      error = e.message
    }
  }

  if (initialCode) search()
</script>

<h1>查询图书</h1>

<div class="card">
  <div class="row">
    <div class="grow">
      <label>实体编号 / 书名 / ISBN / 作者</label>
      <input bind:value={q} placeholder="如 BK-000001 或 三体" onkeydown={(e) => e.key === 'Enter' && search()} />
    </div>
    <button onclick={search}>查询</button>
  </div>
  <p class="muted">每本实体书有独立编号，同书名同 ISBN 的不同副本互不混淆。</p>
</div>

{#if error}<div class="alert err">{error}</div>{/if}

{#if selected}
  <div class="card">
    <h2>当前位置</h2>
    <BookCard book={selected} />
  </div>
  <div class="card">
    <h2>流转记录</h2>
    <HistoryTimeline events={history} />
  </div>
{:else if searched}
  {#if results.length === 0 && !error}
    <div class="card muted">没有找到匹配的图书</div>
  {/if}
  {#each results as book}
    <div class="card" style="cursor:pointer" onclick={() => showDetail(book)}>
      <BookCard {book} />
    </div>
  {/each}
{/if}
