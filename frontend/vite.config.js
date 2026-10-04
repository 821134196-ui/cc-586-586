import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

export default defineConfig({
  plugins: [svelte()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' }
  },
  build: {
    // 构建产物直接输出到 Quarkus 静态资源目录，由后端统一对外服务
    outDir: '../backend/src/main/resources/META-INF/resources',
    emptyOutDir: true
  }
})
