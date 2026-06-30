import { useState } from 'react'
import type { User } from '@/entities/user'
import type { LoginOutcome } from '@/features/auth'
import { useAuth } from '@/features/auth'

type LoginStep = 'credentials' | 'totp' | 'enroll' | 'setup'

export function useLoginFlow() {
  const finalizeLogin = useAuth(s => s.finalizeLogin)
  const [step, setStep] = useState<LoginStep>('credentials')
  const [pendingUser, setPendingUser] = useState<User | null>(null)
  const [pendingUsername, setPendingUsername] = useState('')

  // 'authenticated' is intentionally excluded: login() never returns it because
  // a successful non-TOTP login always proposes enrollment first (finalizeLogin
  // is called separately). If LoginOutcome ever gains new variants, TypeScript
  // will surface a compile error here via the exhaustive else-if chain below.
  function handleLoginOutcome(outcome: Exclude<LoginOutcome, { kind: 'authenticated' }>) {
    if (outcome.kind === 'totp_required') {
      setPendingUsername(outcome.username)
      setStep('totp')
    } else if (outcome.kind === 'enrollment_proposed') {
      setPendingUser(outcome.user)
      setStep('enroll')
    }
  }

  function handleVerified(user: User) {
    finalizeLogin(user)
  }

  function handleBack() {
    setStep('credentials')
    setPendingUser(null)
    setPendingUsername('')
  }

  function handleActivate() {
    setStep('setup')
  }

  function handleSkip() {
    if (pendingUser) finalizeLogin(pendingUser)
  }

  function handleSetupSuccess() {
    if (pendingUser) finalizeLogin(pendingUser)
  }

  function handleSetupDismiss() {
    if (pendingUser) finalizeLogin(pendingUser)
  }

  return {
    step,
    pendingUser,
    pendingUsername,
    handleLoginOutcome,
    handleVerified,
    handleBack,
    handleActivate,
    handleSkip,
    handleSetupSuccess,
    handleSetupDismiss,
  }
}