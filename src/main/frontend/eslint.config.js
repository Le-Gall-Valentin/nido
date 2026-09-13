import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import boundaries from 'eslint-plugin-boundaries'
import importPlugin from 'eslint-plugin-import'
import jsxA11y from 'eslint-plugin-jsx-a11y'

export default tseslint.config(
  { ignores: ['dist', 'coverage'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommendedTypeChecked],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
      // Type-aware linting: this is what makes no-floating-promises and no-misused-promises work at
      // all, and those two are the reason the preset was switched on — a mutation whose rejection
      // nobody handles fails in silence, which is a bug class this project has already shipped twice.
      parserOptions: { projectService: true, tsconfigRootDir: import.meta.dirname },
    },
    plugins: {
      'jsx-a11y': jsxA11y,
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      ...jsxA11y.flatConfigs.recommended.rules,
      /*
       * Two adjustments, both because the rule reads JSX and cannot read intent.
       *
       * aria-role fires on <UserAvatar role="USER"> — a component prop carrying this application's
       * own notion of a role, not the ARIA attribute. ignoreNonDOM is the option built for exactly
       * that; the rule stays armed on real DOM elements, where a bad role is a real defect.
       *
       * no-autofocus exists because autofocus on page load moves the caret out from under the
       * reader. Every one of the thirteen here is the first field of a modal the user just opened,
       * inside a focus trap — which is where putting the caret is the expected behaviour, not a
       * surprise. Turned off rather than suppressed thirteen times.
       */
      'jsx-a11y/aria-role': ['error', { ignoreNonDOM: true }],
      'jsx-a11y/no-autofocus': 'off',
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    },
  },
  {
    // Test files, and only test files. Every rule switched off here fires on a vitest idiom rather
    // than on a defect: `expect(api.listMembers)` passes a method around on purpose (unbound-method),
    // `vi.fn()` is typed `any` at its edges so anything read off a mock is "unsafe", and a stub is
    // often `async` with nothing to await. The two rules the preset was turned on for —
    // no-floating-promises and no-misused-promises — stay on everywhere, and they report nothing here.
    files: ['**/*.test.{ts,tsx}'],
    rules: {
      '@typescript-eslint/unbound-method': 'off',
      '@typescript-eslint/require-await': 'off',
      '@typescript-eslint/no-unsafe-assignment': 'off',
      '@typescript-eslint/no-unsafe-argument': 'off',
      '@typescript-eslint/no-unsafe-call': 'off',
      '@typescript-eslint/no-unsafe-member-access': 'off',
      '@typescript-eslint/no-unsafe-return': 'off',
      '@typescript-eslint/restrict-template-expressions': 'off',
      // Off for a different reason, and the reason matters: here this rule and `tsc` disagree.
      // ESLint's program types Testing Library's getByLabelText as the asserted element, so it calls
      // `as HTMLSelectElement` redundant and its autofix removes it — after which `tsc -b` fails on
      // `.value does not exist on type HTMLElement`. Trusting the compiler over the linter.
      '@typescript-eslint/no-unnecessary-type-assertion': 'off',
    },
  },
  {
    // The HTTP client belongs to the segments whose job is HTTP. A store, a hook or a component that
    // imports it is reaching past its own port — which is what twenty I*Api interfaces exist to
    // prevent, and what authStore was doing to say "somebody signed in" (see shared/lib/
    // sessionCallbacks for where that conversation lives now). The api/ segments are the exception,
    // because implementing the port is precisely their job.
    files: ['src/{entities,features,pages}/**/*.{ts,tsx}'],
    ignores: ['src/*/*/api/**', 'src/**/*.test.{ts,tsx}'],
    rules: {
      'no-restricted-imports': ['error', {
        patterns: [{
          group: ['@/shared/api', '@/shared/api/*'],
          message: 'Only an api/ segment may reach the HTTP client. Depend on your port, or route the call through shared/lib.',
        }],
      }],
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