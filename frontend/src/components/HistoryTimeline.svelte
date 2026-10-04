<script>
  import { EVENT_LABELS, fmtTime } from '../api.js'

  let { events } = $props()

  function describe(e) {
    const parts = []
    if (e.fromBoxName) parts.push(`离开「${e.fromBoxName}」`)
    if (e.toBoxName) parts.push(`进入「${e.toBoxName}」`)
    return parts.join('，')
  }
</script>

{#if events.length === 0}
  <p class="muted">暂无流转记录</p>
{:else}
  <ul class="timeline">
    {#each events as e}
      <li>
        <div>
          <strong>{EVENT_LABELS[e.type] || e.type}</strong>
          {describe(e)}
          {#if e.actor}<span class="muted"> · 操作人：{e.actor}</span>{/if}
        </div>
        {#if e.note}<div class="muted">{e.note}</div>{/if}
        <div class="t-time">{fmtTime(e.occurredAt)}</div>
      </li>
    {/each}
  </ul>
{/if}
