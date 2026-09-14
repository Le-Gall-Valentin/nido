import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    outDir: '../resources/static',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        /*
         * Split what changes on a different clock from what we write.
         *
         * The initial download was one 522 kB file holding React, the router, i18next, axios and the
         * whole application: every deploy invalidated all of it, so a returning user re-downloaded
         * React because a label changed. These libraries move a few times a year, our code moves
         * daily — separating them means a deploy costs the application chunk and nothing else.
         *
         * Deliberately not split: lucide-react. It is already alone in a chunk of its own, pulled in
         * lazily by the finance category picker (resolveCategoryIcon imports the namespace, so the
         * whole icon set comes with it — 864 kB that never reaches the initial load). Naming it here
         * would change nothing; shrinking it means picking a fixed set of icons, which decides
         * something for the product rather than for the build.
         */
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          query: ['@tanstack/react-query'],
          i18n: ['i18next', 'react-i18next', 'i18next-browser-languagedetector'],
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})