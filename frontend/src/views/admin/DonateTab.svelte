<script>
  import { api } from '../../api.js'

  let title = $state('')
  let isbn = $state('')
  let author = $state('')
  let condition = $state('GOOD')
  let donorName = $state('')
  let donorContact = $state('')
  let anonymous = $state(false)
  let note = $state('')
  let coverFile = $state(null)

  let error = $state('')
  let created = $state(null)
  let loading = $state(false)

  async function submit() {
    error = ''
    created = null
    loading = true
    try {
      const book = await api.donate({
        title: title.trim(),
        isbn: isbn.trim() || null,
        author: author.trim() || null,
        condition,
        donorName: donorName.trim() || null,
        donorContact: donorContact.trim() || null,
        anonymous,
        note: note.trim() || null
      })
      if (coverFile) {
        await api.uploadCover(book.code, coverFile)
      }
      created = book
      title = isbn = author = donorName = donorContact = note = ''
      condition = 'GOOD'
      anonymous = false
      coverFile = null
    } catch (e) {
      error = e.message
    } finally {
      loading = false
    }
  }
</script>

{#if created}
  <div class="alert ok">
    ✅ 登记成功！实体编号
    <div class="big-code">{created.code}</div>
    请将编号标签贴到书上，然后在「待上架」中完成检查上架。
  </div>
{/if}
{#if error}<div class="alert err">{error}</div>{/if}

<div class="card">
  <h2>居民捐书登记</h2>
  <div class="row">
    <div class="grow"><label>书名 *</label><input bind:value={title} /></div>
    <div class="grow"><label>ISBN</label><input bind:value={isbn} placeholder="978…" /></div>
    <div class="grow"><label>作者</label><input bind:value={author} /></div>
  </div>
  <div class="row" style="margin-top:12px">
    <div class="grow">
      <label>书况</label>
      <select bind:value={condition}>
        <option value="NEW">全新</option>
        <option value="GOOD">良好</option>
        <option value="FAIR">一般</option>
        <option value="POOR">较差</option>
      </select>
    </div>
    <div class="grow"><label>捐赠人姓名</label><input bind:value={donorName} /></div>
    <div class="grow"><label>捐赠人联系方式</label><input bind:value={donorContact} /></div>
  </div>
  <div class="row" style="margin-top:12px">
    <div class="grow"><label>备注</label><input bind:value={note} /></div>
    <div class="grow">
      <label>封面图（选填）</label>
      <input type="file" accept="image/*" onchange={(e) => (coverFile = e.target.files[0] || null)} />
    </div>
    <div>
      <label>&nbsp;</label>
      <label style="color:var(--ink)"><input type="checkbox" bind:checked={anonymous} style="width:auto" /> 匿名捐赠（不对读者展示身份）</label>
    </div>
  </div>
  <div style="margin-top:16px">
    <button onclick={submit} disabled={loading || !title.trim()}>{loading ? '提交中…' : '登记捐书'}</button>
  </div>
</div>
