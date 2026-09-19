import { defineConfig } from 'vite';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  server: {
    port: 5173,
  },
  build: {
    rollupOptions: {
      input: {
        login: fileURLToPath(new URL('./login.html', import.meta.url)),
        signup: fileURLToPath(new URL('./signup.html', import.meta.url)),
        home: fileURLToPath(new URL('./home.html', import.meta.url)),
      },
    },
  },
});