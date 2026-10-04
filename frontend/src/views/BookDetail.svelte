<script>
  import { onMount } from 'svelte'
  import { get, STATUS_LABELS, CONDITION_LABELS, MOVEMENT_LABELS, fmtTime } from '../api.js'

  export let copyCode

  let copy = null
  let history = []
  let error = ''

  onMount(load)

  $: if (copyCode) load()

  async function load() {
    error = ''
    try {
      copy = await get(`/api/copies/${copyCode}`)
      history = await get(`/api/copies/${copyCode}/history`)
    } catch (e) {
      error = e.message
      copy = null
    }
  }
</script>

{#if error}
  <div class="error-box">{error}</div>
{:else if copy}
  <div class="book-head">
    {#if copy.coverUrl}
      <img class="cover" src={copy.coverUrl} alt="封面" />
    {:else}
      <div class="cover">暂无封面</div>
    {/if}
    <div>
      <h3>《{copy.title}》 <span class="muted">{copy.author ?? ''}</span></h3>
      <div class="muted">实体编号 {copy.copyCode} · ISBN {copy.isbn ?? '—'} · 书况 {CONDITION_LABELS[copy.condition]}</div>
      <p>
        状态：<span class="tag" class:ok={copy.status === 'ON_SHELF'} class:warn={copy.status === 'BORROWED'} class:bad={copy.status === 'DAMAGED'}>{STATUS_LABELS[copy.status]}</span>
        {#if copy.boxCode}
          · 当前位于 <strong>{copy.boxName}</strong>（{copy.boxCode}，{copy.boxLocation ?? '位置未登记'}）
        {:else if copy.status === 'BORROWED'}
          · 已被借出，不在箱中
        {/if}
      </p>
    </div>
  </div>

  <h3>流转记录</h3>
  <ul class="timeline">
    {#each history as ev}
      <li>
        <span class="tag">{MOVEMENT_LABELS[ev.type] ?? ev.type}</span>
        {#if ev.fromBox}从 {ev.fromBox}{/if}
        {#if ev.toBox}→ {ev.toBox}{/if}
        <span class="muted">{fmtTime(ev.happenedAt)}</span>
        {#if ev.note}<div class="muted">{ev.note}</div>{/if}
      </li>
    {/each}
  </ul>
{/if}
