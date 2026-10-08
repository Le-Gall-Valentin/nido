import { useState } from 'react'
import type { TwoFactorMethod, User } from '@/entities/user'
import type { LoginOutcome, TwoFactorChallenge } from '@/features/auth'
import { useAuth } from '@/features/auth'
import type { CodeChoice, MailSetupData } from '@/features/two-factor'

type LoginStep =
  | { name: 'credentials' }
  | { name: 'choose' }
  | { name: 'code'; method: TwoFactorMethod; resendAfterSeconds: number; mailLimitSeconds: number | null }
  | { name: 'propose' }
  | { name: 'setup_app' }
  | { name: 'setup_mail'; sentTo: string; resendAfterSeconds: number }

const CREDENTIALS: LoginStep = { name: 'credentials' }

export function useLoginFlow() {
  const finalizeLogin = useAuth(s => s.finalizeLogin)
  const [step, setStep] = useState<LoginStep>(CREDENTIALS)
  const [challenge, setChallenge] = useState<TwoFactorChallenge | null>(null)
  const [pendingUser, setPendingUser] = useState<User | null>(null)

  // 'authenticated' never reaches here: LoginForm keeps it, the store has already signed the account in.
  function handleLoginOutcome(outcome: Exclude<LoginOutcome, { kind: 'authenticated' }>) {
    if (outcome.kind === 'enrollment_proposed') {
      setPendingUser(outcome.user)
      setStep({ name: 'propose' })
      return
    }
    const { challenge: next } = outcome
    setChallenge(next)
    if (next.methods.length > 1) {
      setStep({ name: 'choose' })
      return
    }
    const method = next.methods[0]
    // The server never asks for a second factor without naming one; were it to, the identifiers are the way on.
    if (method === undefined) {
      setStep({ name: 'credentials' })
      return
    }
    const mailCode = next.mailCode
    setStep({
      name: 'code',
      method,
      resendAfterSeconds: mailCode?.sent ? mailCode.resendAfterSeconds : 0,
      mailLimitSeconds: mailCode && !mailCode.sent ? mailCode.retryAfterSeconds : null,
    })
  }

  // The mail's code answers after it left: by then the person may have gone back to the identifiers, and a
  // code screen without its sign-in would leave the column empty.
  function handleChoice(choice: CodeChoice) {
    setStep(current => {
      if (current.name !== 'choose') return current
      return choice.method === 'APP'
        ? { name: 'code', method: 'APP', resendAfterSeconds: 0, mailLimitSeconds: null }
        : { name: 'code', method: 'MAIL', resendAfterSeconds: choice.resendAfterSeconds, mailLimitSeconds: null }
    })
  }

  function handleChooseAnother() {
    setStep({ name: 'choose' })
  }

  function handleVerified(user: User) {
    finalizeLogin(user)
  }

  function handleBack() {
    setStep(CREDENTIALS)
    setChallenge(null)
    setPendingUser(null)
  }

  function handleAppChosen() {
    setStep({ name: 'setup_app' })
  }

  function handleMailStarted(setup: MailSetupData) {
    setStep({ name: 'setup_mail', sentTo: setup.sentTo, resendAfterSeconds: setup.resendAfterSeconds })
  }

  function handleBackToProposal() {
    setStep({ name: 'propose' })
  }

  function handleSkip() {
    if (pendingUser) finalizeLogin(pendingUser)
  }

  function handleSetupSuccess(method: TwoFactorMethod) {
    if (pendingUser) finalizeLogin({ ...pendingUser, twoFactorMethods: [method] })
  }

  function handleSetupDismiss() {
    if (pendingUser) finalizeLogin(pendingUser)
  }

  return {
    step, challenge, pendingUser,
    handleLoginOutcome, handleChoice, handleChooseAnother, handleVerified, handleBack,
    handleAppChosen, handleMailStarted, handleBackToProposal, handleSkip, handleSetupSuccess, handleSetupDismiss,
  }
}
