<script>
  import { api, CONDITION_LABELS } from '../../api.js'
  import BookCard from '../../components/BookCard.svelte'

  let pending = $state([])
  let boxes = $state([])
  let choices = $state({}) // code -> { boxCode, condition }
  let error = $state('')
  let message = $state('')

  async function load() {
    error = ''
    try {
      ;[pending, boxes] = await Promise.all([api.pendingBooks(), api.listBoxes()])
      for (const b of pending) {
        if (!choices[b.code]) {
          const free = boxes.find((x) => x.free > 0)
          choices[b.code] = { boxCode: free ? free.code : '', condition: b.condition }
        }
      }
    } catch (e) {
      error = e.message
    }
  }

  async function shelve(book) {
    error = ''
    message = ''
    const choice = choices[book.code]
    try {
      await api.shelve(book.code, { boxCode: choice.boxCode, condition: choice.condition })
      message = `✅ ${book.code}《${book.title}》已上架到 ${choice.boxCode}`
      await load()
    } catch (e) {
      error = e.message
      await load()
    }
  }

  load()
</script>

{#if message}<div class="alert ok">{message}</div>{/if}
{#if error}<div class="alert err">{error}</div>{/if}

{#if pending.length === 0}
  <div class="card muted">没有待上架的捐赠图书</div>
{/if}

{#each pending as book}
  <div class="card">
    <BookCard {book} showDonor />
    <div class="row" style="margin-top:14px">
      <div class="grow">
        <label>检查后书况</label>
        <select bind:value={choices[book.code].condition}>
          {#each Object.entries(CONDITION_LABELS) as [value, label]}
            <option {value}>{label}</option>
          {/each}
        </select>
      </div>
      <div class="grow">
        <label>上架到箱位</label>
        <select bind:value={choices[book.code].boxCode}>
          {#each boxes as box}
            <option value={box.code} disabled={box.free === 0}>
              {box.name}（{box.code}，{box.load}/{box.capacity}{box.free === 0 ? ' 已满' : ''}）
            </option>
          {/each}
        </select>
      </div>
      <button onclick={() => shelve(book)} disabled={!choices[book.code].boxCode}>检查通过，上架</button>
    </div>
  </div>
{/each}
