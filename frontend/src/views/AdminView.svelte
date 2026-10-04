<script>
  import { api } from '../api.js'
  import DonateTab from './admin/DonateTab.svelte'
  import ShelveTab from './admin/ShelveTab.svelte'
  import LoansTab from './admin/LoansTab.svelte'
  import RecordsTab from './admin/RecordsTab.svelte'
  import BoxesTab from './admin/BoxesTab.svelte'

  let tab = $state('donate')
  const tabs = [
    { key: 'donate', label: '捐赠登记' },
    { key: 'shelve', label: '待上架' },
    { key: 'loans', label: '当前借阅' },
    { key: 'records', label: '处理记录' },
    { key: 'boxes', label: '箱位管理' }
  ]
</script>

<h1>管理后台</h1>

<div class="tabs">
  {#each tabs as t}
    <button class:active={tab === t.key} onclick={() => (tab = t.key)}>{t.label}</button>
  {/each}
</div>

{#if tab === 'donate'}
  <DonateTab />
{:else if tab === 'shelve'}
  <ShelveTab />
{:else if tab === 'loans'}
  <LoansTab />
{:else if tab === 'records'}
  <RecordsTab />
{:else if tab === 'boxes'}
  <BoxesTab />
{/if}
