import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { join, relative, resolve } from 'node:path'

// Read from disk: vitest hands CSS imports over empty, `?raw` included.
const root = resolve(process.cwd(), 'src')
const css = readFileSync(join(root, 'index.css'), 'utf-8')

/** The declarations between a selector and the first closing brace after it: no theme block nests another. */
function block(selector: string) {
  const start = css.indexOf(selector)
  expect(start, selector).toBeGreaterThanOrEqual(0)
  return css.slice(start, css.indexOf('}', start))
}

function token(declarations: string, name: string) {
  const value = declarations.match(new RegExp(`${name}:\\s*(#[0-9a-f]{6})`, 'i'))?.[1]
  expect(value, name).toBeDefined()
  return value as string
}

/** WCAG 2 contrast ratio between two #rrggbb colours. */
function contrast(a: string, b: string) {
  const luminance = (hex: string) => {
    const [r, g, bl] = [1, 3, 5].map((i) => {
      const c = parseInt(hex.slice(i, i + 2), 16) / 255
      return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
    })
    return 0.2126 * r + 0.7152 * g + 0.0722 * bl
  }
  const [high, low] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (high + 0.05) / (low + 0.05)
}

const THEMES = {
  light: block('@theme {'),
  'dark, as the system asks': block(':root:not([data-theme="light"]) {'),
  'dark, as the person forces it': block(':root[data-theme="dark"] {'),
}

function sources(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name)
    if (entry.isDirectory()) return sources(path)
    return /\.tsx?$/.test(entry.name) && !/\.test\.tsx?$/.test(entry.name) ? [path] : []
  })
}

describe('contrast of text on the green', () => {
  it.each(Object.entries(THEMES))('keeps the text on a solid accent readable in the %s theme', (_, declarations) => {
    expect(contrast(token(declarations, '--color-on-accent'), token(declarations, '--color-accent')))
      .toBeGreaterThanOrEqual(4.5)
  })

  it.each(Object.entries(THEMES))('keeps a danger button\'s label readable in the %s theme', (_, declarations) => {
    expect(contrast(token(declarations, '--color-bg-0'), token(declarations, '--color-status-red')))
      .toBeGreaterThanOrEqual(4.5)
  })

  it('keeps the primary button\'s label readable, in its colours shared by both themes', () => {
    const primary = block('/* Primary button')
    expect(contrast(token(primary, '--btn-primary-fg'), token(primary, '--btn-primary-bg'))).toBeGreaterThanOrEqual(4.5)
  })

  it('never writes white, nor the page\'s own background, on the accent: the dark theme\'s green is too light for it', () => {
    // The dark accent under white is 2.1:1; the light one under the beige background, 4.0:1. `text-on-accent` fits both.
    const offending = sources(root).flatMap((file) => readFileSync(file, 'utf-8').split('\n')
      .map((line, index) => ({ line, at: `${relative(root, file)}:${index + 1}` }))
      .filter(({ line }) => (/\bbg-accent(?![-/\w])/.test(line) && /\btext-white\b/.test(line))
        || /background(Color)?: 'var\(--color-accent\)'/.test(line))
      .map(({ at }) => at))
    expect(offending).toEqual([])
  })
})
