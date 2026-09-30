import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { useState } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { StoreApi } from 'zustand'
import * as auth from '../model/authStoreContext'
import type { User } from '@/entities/user'
import { LanguageContext } from '@/shared/lib/language'
import type { Language } from '@/shared/lib'
import { AccountLanguageSync } from './AccountLanguageSync'

interface FakeAuth {
  user: User | null
  patchUser: (partial: Partial<User>) => void
}

// A real store behind the mocked hook: signing in and out re-renders the component the way the app
// does, instead of remounting it the way rerender() would.
vi.mock('../model/authStoreContext', async () => {
  const { create, useStore } = await import('zustand')
  const store = create<FakeAuth>((set) => ({
    user: null,
    patchUser: (partial) => set((state) => ({ user: state.user ? { ...state.user, ...partial } : null })),
  }))
  return {
    useAuth: <T,>(selector: (state: FakeAuth) => T) => useStore(store, selector),
    __store: store,
  }
})

const authStore = (auth as unknown as { __store: StoreApi<FakeAuth> }).__store
const saveLanguage = vi.fn<(language: Language) => Promise<void>>()

function user(id: string, language: Language | null): User {
  return { id, username: id, email: `${id}@test.com`, role: 'USER', createdAt: '2026-01-01T00:00:00Z', totpEnabled: false, language }
}

/** Owns the language on screen, like LanguageProvider; the button is Preferences' switch. */
function Screen({ initial }: { initial: Language }) {
  const [language, setLanguage] = useState<Language>(initial)
  return (
    <LanguageContext.Provider value={{ language, setLanguage }}>
      <AccountLanguageSync api={{ saveLanguage }} />
      <output data-testid="on-screen">{language}</output>
      <button onClick={() => setLanguage(language === 'fr' ? 'en' : 'fr')}>switch</button>
    </LanguageContext.Provider>
  )
}

const onScreen = () => screen.getByTestId('on-screen').textContent

beforeEach(() => {
  saveLanguage.mockReset().mockResolvedValue(undefined)
  act(() => authStore.setState({ user: null }))
})

describe('AccountLanguageSync', () => {
  it('does nothing while signed out', () => {
    render(<Screen initial="fr" />)

    expect(saveLanguage).not.toHaveBeenCalled()
    expect(onScreen()).toBe('fr')
  })

  it('applies the account language at sign-in, then records a later switch', async () => {
    render(<Screen initial="fr" />)

    act(() => authStore.setState({ user: user('alice', 'en') }))
    expect(onScreen()).toBe('en')
    expect(saveLanguage).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'switch' }))

    expect(onScreen()).toBe('fr')
    await waitFor(() => expect(authStore.getState().user?.language).toBe('fr'))
    expect(saveLanguage).toHaveBeenCalledTimes(1)
    expect(saveLanguage).toHaveBeenCalledWith('fr')
  })

  it('records the detected language on an account that never had one', async () => {
    render(<Screen initial="fr" />)

    act(() => authStore.setState({ user: user('alice', null) }))

    await waitFor(() => expect(authStore.getState().user?.language).toBe('fr'))
    expect(saveLanguage).toHaveBeenCalledWith('fr')
    expect(onScreen()).toBe('fr')
  })

  it('keeps the language on this device when recording it fails', async () => {
    saveLanguage.mockRejectedValue(new Error('network'))
    render(<Screen initial="fr" />)

    act(() => authStore.setState({ user: user('alice', null) }))

    await waitFor(() => expect(saveLanguage).toHaveBeenCalled())
    expect(authStore.getState().user?.language).toBeNull()
    expect(onScreen()).toBe('fr')
  })

  it('applies the next account’s language after a sign-out', () => {
    render(<Screen initial="fr" />)
    act(() => authStore.setState({ user: user('alice', 'fr') }))
    act(() => authStore.setState({ user: null }))

    act(() => authStore.setState({ user: user('bob', 'en') }))

    expect(onScreen()).toBe('en')
    expect(saveLanguage).not.toHaveBeenCalled()
  })
})
