import { defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test-setup.ts'],
      reporters: ['verbose'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json-summary'],
        include: ['src/**/*.{ts,tsx}'],
        exclude: [
          'src/**/*.test.{ts,tsx}',
          'src/**/locales/**',       // JSON re-exported through a one-line module
          'src/test-setup.ts',
          'src/shared/test/**',      // the harness the tests run on
          'src/**/*.d.ts',
          /*
           * The composition root, and only it: the files whose entire content is wiring — building
           * the query client, listing the routes, mounting a provider, initialising i18next. Covering
           * them means rendering the whole application, which is an end-to-end concern this project
           * does not have a layer for, and a test that only asserts "it mounted" would raise the
           * figure without raising the confidence.
           *
           * Named one by one rather than excluding src/app: the shell (AppLayout) and useAuthGuard
           * hold real logic, so they stay counted even though nothing covers them today — which is
           * the point of counting.
           */
          'src/main.tsx',
          'src/app/App.tsx',
          'src/app/i18n.ts',
          'src/app/router/AppRouter.tsx',
          'src/app/providers/AuthProvider.tsx',
          'src/app/layouts/paletteSetups.tsx',
        ],
        /*
         * Set two points under what the suite measures today, not at some round number nobody meets.
         * A threshold is a ratchet: its job is to make a drop visible in the pull request that caused
         * it, while leaving room for an honest refactor that moves a few lines around. Raise it when
         * the real figure has been comfortably above for a while — never lower it to make a build pass.
         *
         * Measured at the commit that introduced this: statements 89.2, branches 84.0, functions 86.6,
         * lines 90.8.
         */
        thresholds: {
          statements: 87,
          branches: 82,
          functions: 84,
          lines: 88,
        },
      },
    },
  }),
)
