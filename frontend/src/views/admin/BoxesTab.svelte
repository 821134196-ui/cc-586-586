<script>
  import { api } from '../../api.js'

  let boxes = $state([])
  let error = $state('')
  let message = $state('')

  let code = $state('')
  let name = $state('')
  let location = $state('')
  let capacity = $state(10)

  async function load() {
    try {
      boxes = await api.listBoxes()
    } catch (e) {
      error = e.message
    }
  }

  async function create() {
    error = ''
    message = ''
    try {
      await api.createBox({ code: code.trim(), name: name.trim(), location: location.trim() || null, capacity })
      message = `✅ 箱位 ${code.trim().toUpperCase()} 已创建`
      code = name = location = ''
      capacity = 10
      await load()
    } catch (e) {
      error = e.message
    }
  }

  load()
</script>

{#if message}<div class="alert ok">{message}</div>{/if}
{#if error}<div class="alert err">{error}</div>{/if}

<div class="grid">
  {#each boxes as box}
    <div class="card" style="margin:0">
      <h2>{box.name}</h2>
      <div class="muted">{box.code}{box.location ? ' · ' + box.location : ''}</div>
      <div class="capacity-bar" class:full={box.free === 0} style="margin:10px 0 4px">
        <div style="width:{box.capacity ? Math.min(100, (box.load / box.capacity) * 100) : 0}%"></div>
      </div>
      <div>
        {box.load}/{box.capacity} 在架
        {#if box.free === 0}<span class="badge overdue">已满</span>{/if}
      </div>
    </div>
  {/each}
</div>

<div class="card" style="margin-top:16px">
  <h2>新增箱位</h2>
  <div class="row">
    <div class="grow"><label>箱位编号 *</label><input bind:value={code} placeholder="BOX-C" /></div>
    <div class="grow"><label>名称 *</label><input bind:value={name} placeholder="北门漂流箱" /></div>
    <div class="grow"><label>位置</label><input bind:value={location} placeholder="北门门卫室旁" /></div>
    <div style="width:110px"><label>容量</label><input type="number" min="1" bind:value={capacity} /></div>
    <button onclick={create} disabled={!code.trim() || !name.trim()}>创建</button>
  </div>
</div>
