import { fileURLToPath, URL } from 'node:url';
import tailwindcss from '@tailwindcss/vite';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 7070,
    proxy: {
      '/api': 'http://localhost:7071',
      '/ws-chatapp': {
        target: 'http://localhost:7071',
        ws: true
      }
    }
  },
  build: {
    target: 'es2022',
    outDir: '../../target/frontend-dist',
    emptyOutDir: true,
    sourcemap: false
  }
});
