import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

export default defineConfig({
  plugins: [svelte()],
  build: {
    // 构建产物直接写入 Quarkus 静态资源目录，由后端统一托管
    outDir: '../backend/src/main/resources/META-INF/resources',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' }
  }
})
