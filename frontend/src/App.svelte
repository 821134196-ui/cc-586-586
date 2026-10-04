<script>
  import SearchView from './views/SearchView.svelte'
  import BorrowView from './views/BorrowView.svelte'
  import ReturnView from './views/ReturnView.svelte'
  import AdminView from './views/AdminView.svelte'

  const routes = [
    { hash: '', label: '查书', component: SearchView },
    { hash: 'borrow', label: '借书', component: BorrowView },
    { hash: 'return', label: '还书', component: ReturnView },
    { hash: 'admin', label: '管理', component: AdminView }
  ]

  let current = location.hash.replace(/^#\/?/, '')
  window.addEventListener('hashchange', () => {
    current = location.hash.replace(/^#\/?/, '')
  })

  $: active = routes.find(r => r.hash === current) ?? routes[0]
</script>

<header class="topbar">
  <div class="brand">📚 社区图书漂流箱</div>
  <nav>
    {#each routes as r}
      <a href="#/{r.hash}" class:active={active.hash === r.hash}>{r.label}</a>
    {/each}
  </nav>
</header>

<main>
  <svelte:component this={active.component} />
</main>
