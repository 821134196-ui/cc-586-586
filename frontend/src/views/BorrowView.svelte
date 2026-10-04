<script>
  import { get, post, STATUS_LABELS, fmtDate } from '../api.js'

  let copyCode = ''
  let copy = null
  let lookupError = ''
  let borrowerName = ''
  let borrowerContact = ''
  let error = ''
  let done = null
  let busy = false

  async function lookup() {
    copy = null
    done = null
    error = ''
    lookupError = ''
    if (!copyCode.trim()) return
    try {
      copy = await get('/api/copies/' + encodeURIComponent(copyCode.trim()))
      if (copy.status !== 'ON_SHELF') {
        lookupError = `该书当前不可借（${STATUS_LABELS[copy.status]}）`
      }
    } catch (e) {
      lookupError = e.message
    }
  }

  async function submit() {
    busy = true
    error = ''
    try {
      const loan = await post('/api/loans', {
        copyCode: copyCode.trim(),
        borrowerName: borrowerName.trim(),
        borrowerContact: borrowerContact.trim() || null
      })
      done = loan
      copy = null
      copyCode = ''
      borrowerName = ''
      borrowerContact = ''
    } catch (e) {
      error = e.message
    } finally {
      busy = false
    }
  }
</script>

<div class="card">
  <h2>借书</h2>
  <div class="muted">扫描书上二维码或手动输入实体编号（如 BK-000001）。</div>
  <div class="row" style="margin-top:0.6rem">
    <input class="grow" bind:value={copyCode} placeholder="实体编号"
           on:keydown={(e) => e.key === 'Enter' && lookup()} />
    <button class="secondary" on:click={lookup}>查询</button>
  </div>
  {#if lookupError}<div class="error-box">{lookupError}</div>{/if}
</div>

{#if copy && !lookupError}
  <div class="card">
    <h3>《{copy.title}》 <span class="muted">{copy.author ?? ''}</span></h3>
    <div class="muted">编号 {copy.copyCode} · 当前在 {copy.boxName}（{copy.boxCode}）</div>
    <form on:submit|preventDefault={submit} style="margin-top:0.8rem">
      <div class="row">
        <input class="grow" bind:value={borrowerName} placeholder="借书人姓名（必填）" required />
        <input class="grow" bind:value={borrowerContact} placeholder="联系方式（选填）" />
        <button type="submit" disabled={busy || !borrowerName.trim()}>确认借出</button>
      </div>
    </form>
    {#if error}<div class="error-box">{error}</div>{/if}
  </div>
{/if}

{#if done}
  <div class="card success-box">
    已借出《{done.title}》（{done.copyCode}），应还日期：<strong>{fmtDate(done.dueAt)}</strong>。
    归还时可放入任意一个漂流箱。
  </div>
{/if}
