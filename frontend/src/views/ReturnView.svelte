<script>
  import { onMount } from 'svelte'
  import { get, post, STATUS_LABELS } from '../api.js'

  let copyCode = ''
  let copy = null
  let lookupError = ''
  let boxes = []
  let targetBox = ''
  let damageNote = ''
  let error = ''
  let doneMsg = ''
  let busy = false

  onMount(loadBoxes)

  async function loadBoxes() {
    boxes = await get('/api/boxes')
  }

  async function lookup() {
    copy = null
    error = ''
    doneMsg = ''
    lookupError = ''
    if (!copyCode.trim()) return
    try {
      copy = await get('/api/copies/' + encodeURIComponent(copyCode.trim()))
      if (copy.status !== 'BORROWED') {
        lookupError = `该书当前不在借出状态（${STATUS_LABELS[copy.status]}），无需归还`
        copy = null
      }
    } catch (e) {
      lookupError = e.message
    }
  }

  async function submit() {
    busy = true
    error = ''
    try {
      const loan = await post('/api/loans/return', {
        copyCode: copyCode.trim(),
        boxCode: targetBox,
        damageNote: damageNote.trim() || null
      })
      doneMsg = damageNote.trim()
        ? `《${loan.title}》已登记归还并标记破损，管理员会跟进处理。`
        : `《${loan.title}》已归还到 ${loan.returnBoxCode}，感谢漂流！`
      copy = null
      copyCode = ''
      damageNote = ''
      await loadBoxes()
    } catch (e) {
      error = e.message
    } finally {
      busy = false
    }
  }
</script>

<div class="card">
  <h2>还书</h2>
  <div class="muted">可还到任意有空位的漂流箱，不必是原箱。</div>
  <div class="row" style="margin-top:0.6rem">
    <input class="grow" bind:value={copyCode} placeholder="实体编号"
           on:keydown={(e) => e.key === 'Enter' && lookup()} />
    <button class="secondary" on:click={lookup}>查询</button>
  </div>
  {#if lookupError}<div class="error-box">{lookupError}</div>{/if}
</div>

{#if copy}
  <div class="card">
    <h3>归还《{copy.title}》 <span class="muted">{copy.copyCode}</span></h3>
    <form on:submit|preventDefault={submit}>
      <div class="row" style="margin-top:0.6rem">
        <label>
          还到箱：
          <select bind:value={targetBox} required>
            <option value="" disabled selected>请选择箱位</option>
            {#each boxes as b}
              <option value={b.code} disabled={b.occupied >= b.capacity}>
                {b.name}（{b.code}）{b.occupied}/{b.capacity}{b.occupied >= b.capacity ? ' · 已满' : ''}
              </option>
            {/each}
          </select>
        </label>
      </div>
      <div style="margin-top:0.6rem">
        <textarea class="grow" style="width:100%" rows="2" bind:value={damageNote}
                  placeholder="如发现破损请在此说明（留空表示完好归还）"></textarea>
      </div>
      <div class="row" style="margin-top:0.6rem">
        <button type="submit" disabled={busy || !targetBox}>确认归还</button>
      </div>
    </form>
    {#if error}<div class="error-box">{error}</div>{/if}
  </div>
{/if}

{#if doneMsg}
  <div class="card success-box">{doneMsg}</div>
{/if}
