import { useEffect, useState, type ReactNode } from 'react'
import { ThemeContext, type Theme } from '@/shared/lib/theme'
export { useTheme, type Theme } from '@/shared/lib/theme'

const STORAGE_KEY = 'boilerplate:theme'

function isTheme(value: string | null): value is Theme {
  return value === 'light' || value === 'dark' || value === 'system'
}

function applyTheme(theme: Theme): void {
  if (theme === 'system') {
    document.documentElement.removeAttribute('data-theme')
  } else {
    document.documentElement.setAttribute('data-theme', theme)
  }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(() => {
    const stored = localStorage.getItem(STORAGE_KEY)
    return isTheme(stored) ? stored : 'system'
  })

  useEffect(() => {
    applyTheme(theme)
  }, [theme])

  function setTheme(newTheme: Theme): void {
    localStorage.setItem(STORAGE_KEY, newTheme)
    setThemeState(newTheme)
  }

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}