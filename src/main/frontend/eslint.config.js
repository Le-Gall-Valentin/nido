import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import boundaries from 'eslint-plugin-boundaries'
import importPlugin from 'eslint-plugin-import'

export default tseslint.config(
  { ignores: ['dist'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
  },
  {
    plugins: { boundaries, import: importPlugin },
    settings: {
      'import/resolver': {
        typescript: { alwaysTryTypes: true },
      },
      'boundaries/elements': [
        { type: 'shared',   pattern: ['src/shared/**'] },
        { type: 'entities', pattern: ['src/entities/**'] },
        { type: 'features', pattern: ['src/features/*/**'], capture: ['feature'] },
        { type: 'pages',    pattern: ['src/pages/*/**'],    capture: ['page'] },
        { type: 'app',      pattern: ['src/app/**'] },
        // Note: 'widgets' FSD layer intentionally omitted — project uses 'pages' and 'features' only.
        // Add 'widgets' here if compound UI blocks spanning multiple features are introduced.
      ],
      'boundaries/ignore': ['src/main.tsx', 'src/vite-env.d.ts'],
    },
    rules: {
      'import/no-cycle': 'error',
      'import/no-duplicates': 'error',
      'boundaries/dependencies': ['error', {
        default: 'disallow',
        rules: [
          { from: { type: 'shared' },   allow: [{ to: { type: 'shared' } }] },
          { from: { type: 'entities' }, allow: [{ to: { type: 'shared' } }] },
          {
            from: { type: 'features' },
            allow: [
              { to: { type: 'shared' } },
              { to: { type: 'entities' } },
              { to: { type: 'features', captured: { feature: '{{ from.captured.feature }}' } } },
            ],
          },
          {
            from: { type: 'pages' },
            allow: [
              { to: { type: 'shared' } },
              { to: { type: 'entities' } },
              { to: { type: 'features' } },
              { to: { type: 'pages', captured: { page: '{{ from.captured.page }}' } } },
            ],
          },
          { from: { type: 'app' }, allow: [{ to: { type: ['shared', 'entities', 'features', 'pages', 'app'] } }] },
          // Replaces the deprecated boundaries/no-private (allowUncles: true): forbids importing
          // another element's private (nested) parts, except internal/child/sibling/uncle deps.
          // Evaluated last so it overrides the layer allows above (last-write-wins).
          {
            to: { parent: { type: '*' } },
            disallow: [{ dependency: { relationship: { to: '!(internal|child|sibling|uncle)' } } }],
          },
        ],
      }],
    },
  },
)