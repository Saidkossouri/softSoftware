/// <reference types="vitest/config" />
import path from 'node:path'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

// En développement, le backend Spring Boot tourne sur :8080. L'en-tête Host est conservé
// (changeOrigin: false) pour que l'URI de retour Google soit http://localhost:5173/login/oauth2/code/google.
const backend = { target: 'http://localhost:8080', changeOrigin: false }

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, 'src') },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': backend,
      '/oauth2': backend,
      '/login': backend,
      '/logout': backend,
      '/openapi.yaml': backend,
      '/swagger-ui': backend,
      '/v3': backend,
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./tests/setup.ts'],
  },
})
