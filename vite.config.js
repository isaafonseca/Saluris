import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  root: 'views',
  plugins: [react()],
  resolve: {
    preserveSymlinks: true,
  },
  build: {
    outDir: '../dist',
    emptyOutDir: true,
  },
  server: {
    host: true,
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        bypass(req) {
          if (req.url && (req.url.startsWith('/api/client') || /\.jsx?(\?.*)?$/.test(req.url))) {
            return req.url;
          }
        },
      },
    },
  },
});
