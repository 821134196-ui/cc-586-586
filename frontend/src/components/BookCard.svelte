<script>
  import { CONDITION_LABELS, STATUS_LABELS, fmtDate } from '../api.js'

  let { book, showDonor = false } = $props()

  const statusClass = $derived(
    book.status === 'ON_SHELF' ? 'shelf' : book.status === 'BORROWED' ? 'borrowed' : 'pending'
  )
</script>

<div class="book-item">
  {#if book.coverUrl}
    <img class="cover-img" src={book.coverUrl} alt="封面" />
  {:else}
    <div class="cover-placeholder">暂无<br />封面</div>
  {/if}
  <div class="info">
    <h3>《{book.title}》 <span class="badge {statusClass}">{STATUS_LABELS[book.status] || book.status}</span></h3>
    <div class="muted">
      编号 <span class="bookcode">{book.code}</span>
      {#if book.isbn} · ISBN {book.isbn}{/if}
      {#if book.author} · {book.author}{/if}
      · 书况 {CONDITION_LABELS[book.condition] || book.condition}
    </div>
    <div style="margin-top:6px">
      {#if book.status === 'ON_SHELF'}
        📍 当前在 <strong>{book.boxName}</strong>（{book.boxCode}）{book.boxLocation ? ' · ' + book.boxLocation : ''}
      {:else if book.status === 'BORROWED'}
        📖 已借出{#if book.dueAt}，应还日期 <strong>{fmtDate(book.dueAt)}</strong>{/if}
      {:else if book.status === 'PENDING_INSPECTION'}
        🕐 捐赠待管理员检查上架
      {/if}
    </div>
    {#if book.donorLabel}
      <div class="muted" style="margin-top:4px">捐赠者：{book.donorLabel}</div>
    {/if}
    {#if showDonor && book.donorName}
      <div class="muted" style="margin-top:4px">
        捐赠人：{book.donorName}{book.donorAnonymous ? '（匿名，不对读者展示）' : ''}
        {book.donorContact ? ' · ' + book.donorContact : ''}
      </div>
    {/if}
  </div>
</div>
