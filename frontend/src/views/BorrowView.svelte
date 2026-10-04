<script>
  import { api, fmtDate } from '../api.js'
  import BookCard from '../components/BookCard.svelte'

  let { initialCode = '' } = $props()

  let code = $state(initialCode)
  let book = $state(null)
  let borrowerName = $state('')
  let borrowerContact = $state('')
  let error = $state('')
  let done = $state(null)
  let loading = $state(false)

  async function lookup() {
    error = ''
    done = null
    book = null
    if (!code.trim()) return
    try {
      book = await api.getBook(code.trim())
    } catch (e) {
      error = e.message
    }
  }

  async function submit() {
    error = ''
    loading = true
    try {
      const loan = await api.borrow({
        bookCode: code.trim(),
        borrowerName: borrowerName.trim(),
        borrowerContact: borrowerContact.trim() || null
      })
      done = loan
      book = null
      code = ''
      borrowerName = ''
      borrowerContact = ''
    } catch (e) {
      error = e.message
    } finally {
      loading = false
    }
  }

  if (initialCode) lookup()
</script>

<h1>扫码借书</h1>

{#if done}
  <div class="alert ok">
    ✅ 借阅成功！编号 <span class="bookcode">{done.bookCode}</span>《{done.bookTitle}》，
    应还日期 <strong>{fmtDate(done.dueAt)}</strong>。可归还到任一漂流箱。
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
    {#if book.status === 'ON_SHELF'}
      <div class="row" style="margin-top:16px">
        <div class="grow">
          <label>借阅人姓名 *</label>
          <input bind:value={borrowerName} placeholder="您的姓名" />
        </div>
        <div class="grow">
          <label>联系方式（用于逾期提醒，选填）</label>
          <input bind:value={borrowerContact} placeholder="手机号" />
        </div>
        <button onclick={submit} disabled={loading || !borrowerName.trim()}>
          {loading ? '办理中…' : '确认借出'}
        </button>
      </div>
    {:else}
      <div class="alert err">这本书当前不可借阅（{book.status === 'BORROWED' ? '已被借出' : '未在架'}）。</div>
    {/if}
  </div>
{/if}
