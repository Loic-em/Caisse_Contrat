import react from '@vitejs/plugin-react'
import { resolve } from 'node:path'
import { defineConfig, loadEnv } from 'vite'

// En développement, transmet les appels API au backend sans exposer de détails réseau au navigateur.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  // Le mot de passe démo n'est transmis au navigateur qu'en mode Vite dev.
  const demoEnv = mode === 'development'
    ? loadEnv(mode, resolve(process.cwd(), '../../backend'), 'APP_DEMO_')
    : {}
  const demoAccountsEnabled = mode === 'development'
    && demoEnv.APP_DEMO_ACCOUNTS_ENABLED === 'true'
    && Boolean(demoEnv.APP_DEMO_ACCOUNT_PASSWORD)

  return {
    plugins: [react()],
    define: {
      'import.meta.env.VITE_DEMO_ACCOUNTS_ENABLED': JSON.stringify(demoAccountsEnabled),
      'import.meta.env.VITE_DEMO_ACCOUNT_PASSWORD': JSON.stringify(
        demoAccountsEnabled ? demoEnv.APP_DEMO_ACCOUNT_PASSWORD : '',
      ),
    },
    server: {
      host: '127.0.0.1',
      proxy: {
        '/api': {
          target: env.VITE_BACKEND_URL || 'http://localhost:8081',
          changeOrigin: true,
        },
      },
    },
  }
})
