<script>
  import { api, fmtDate } from '../api.js'
  import BookCard from '../components/BookCard.svelte'

  let { initialCode = '' } = $props()

  let code = $state(initialCode)
  let book = $state(null)
  let boxes = $state([])
  let boxCode = $state('')
  let damaged = $state(false)
  let note = $state('')
  let error = $state('')
  let done = $state(null)
  let loading = $state(false)

  async function loadBoxes() {
    boxes = await api.listBoxes()
  }

  async function lookup() {
    error = ''
    done = null
    book = null
    if (!code.trim()) return
    try {
      book = await api.getBook(code.trim())
      await loadBoxes()
      // 默认选中第一个有空位的箱
      const firstFree = boxes.find((b) => b.free > 0)
      boxCode = firstFree ? firstFree.code : ''
    } catch (e) {
      error = e.message
    }
  }

  async function submit() {
    error = ''
    loading = true
    try {
      const loan = await api.returnBook({
        bookCode: code.trim(),
        boxCode,
        damaged,
        note: note.trim() || null
      })
      done = loan
      book = null
      code = ''
      note = ''
      damaged = false
    } catch (e) {
      error = e.message
      await loadBoxes()
    } finally {
      loading = false
    }
  }

  if (initialCode) lookup()
</script>

<h1>扫码还书</h1>

{#if done}
  <div class="alert ok">
    ✅ 归还成功！编号 <span class="bookcode">{done.bookCode}</span>《{done.bookTitle}》
    已放入箱位 <strong>{done.returnBoxCode}</strong>。
  </div>
{/if}

<div class="card">
  <div class="row">
    <div class="grow">
      <label>扫描或输入图书编号</label>
      <input bind:value={code} placeholder="BK-000001" onkeydown={(e) => e.key === 'Enter' && lookup()} />
    </div>
    <button class="ghost" onclick={lookup}>确认图书</button>
  </div>
</div>

{#if error}<div class="alert err">{error}</div>{/if}

{#if book}
  <div class="card">
    <BookCard {book} />
    {#if book.status === 'BORROWED'}
      <div style="margin-top:16px">
        <label>归还到哪个漂流箱（可跨箱归还，已满的箱不可选）</label>
        <div class="grid">
          {#each boxes as box}
            <label
              class="card"
              style="margin:0; cursor:{box.free > 0 ? 'pointer' : 'not-allowed'}; opacity:{box.free > 0 ? 1 : 0.55}; border:{boxCode === box.code ? '2px solid var(--accent)' : '1px solid var(--line)'}"
            >
              <div>
                <input
                  type="radio"
                  name="box"
                  value={box.code}
                  bind:group={boxCode}
                  disabled={box.free === 0}
                  style="width:auto; margin-right:6px"
                />
                <strong>{box.name}</strong>
                {#if box.free === 0}<span class="badge overdue">已满</span>{/if}
              </div>
              <div class="muted">{box.code}{box.location ? ' · ' + box.location : ''}</div>
              <div class="capacity-bar" class:full={box.free === 0} style="margin-top:8px">
                <div style="width:{box.capacity ? Math.min(100, (box.load / box.capacity) * 100) : 0}%"></div>
              </div>
              <div class="muted">{box.load}/{box.capacity} 在架</div>
            </label>
          {/each}
        </div>

        <div class="row" style="margin-top:16px">
          <div>
            <label>书况确认</label>
            <label style="color:var(--ink)"><input type="radio" bind:group={damaged} value={false} style="width:auto" /> 完好</label>
            <label style="color:var(--ink)"><input type="radio" bind:group={damaged} value={true} style="width:auto" /> 发现破损</label>
          </div>
          <div class="grow">
            <label>备注（破损情况等，选填）</label>
            <input bind:value={note} placeholder="如：封面撕裂、内页缺页" />
          </div>
          <button onclick={submit} disabled={loading || !boxCode}>
            {loading ? '办理中…' : '确认归还'}
          </button>
        </div>
        {#if damaged}
          <div class="alert err">破损情况将记录在处理记录中，由管理员跟进。</div>
        {/if}
      </div>
    {:else}
      <div class="alert err">这本书当前没有未归还的借阅记录。</div>
    {/if}
  </div>
{/if}
