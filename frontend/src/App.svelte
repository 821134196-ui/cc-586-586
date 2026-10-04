<script>
  import SearchView from './views/SearchView.svelte'
  import BorrowView from './views/BorrowView.svelte'
  import ReturnView from './views/ReturnView.svelte'
  import AdminView from './views/AdminView.svelte'

  // 简易 hash 路由：#/  #/borrow  #/return  #/admin，支持 ?code= 参数（扫码链接）
  let route = $state({ path: '/', params: new URLSearchParams() })

  function parseHash() {
    const hash = location.hash.slice(1) || '/'
    const [path, query] = hash.split('?')
    route = { path, params: new URLSearchParams(query || '') }
  }
  parseHash()

  const links = [
    { path: '/', label: '查书' },
    { path: '/borrow', label: '借书' },
    { path: '/return', label: '还书' },
    { path: '/admin', label: '管理后台' }
  ]
</script>

<svelte:window onhashchange={parseHash} />

<nav class="topnav">
  <span class="brand">📚 社区图书漂流箱</span>
  {#each links as link}
    <a href={'#' + link.path} class:active={route.path === link.path}>{link.label}</a>
  {/each}
</nav>

<main class="page">
  {#if route.path === '/'}
    <SearchView initialCode={route.params.get('code') || ''} />
  {:else if route.path === '/borrow'}
    <BorrowView initialCode={route.params.get('code') || ''} />
  {:else if route.path === '/return'}
    <ReturnView initialCode={route.params.get('code') || ''} />
  {:else if route.path === '/admin'}
    <AdminView />
  {/if}
</main>
