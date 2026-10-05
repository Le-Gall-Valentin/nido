import { useState } from 'react'
import { SettingsInvalidError, useLanguage } from '@/shared/lib'
import { setupApi } from '../api/setupApi'
import type { ISetupApi } from '../model/ISetupApi'
import type { SettingValues, SetupAdmin, SetupStatus } from '../model/types'
import { describeSetupError } from '../lib/describeSetupError'
import { signInAfterSetup } from '../lib/signInAfterSetup'
import { AddressStep } from './AddressStep'
import { AdminStep } from './AdminStep'
import { CodeStep } from './CodeStep'
import { DoneStep } from './DoneStep'
import { KeyStep } from './KeyStep'
import { MailStep } from './MailStep'
import { SetupFrame, type SetupStep } from './SetupFrame'

interface Props {
  status: SetupStatus
  /** Composition seams: tests inject fakes. */
  api?: ISetupApi
  signIn?: (identifier: string, password: string) => Promise<boolean>
  goTo?: (url: string) => void
}

/**
 * The first-run setup. Nothing is saved before the last click: the answers stay in this page and go
 * in one call, with the code. A reload starts over — only the generated key persists, and the same
 * one is shown again.
 */
export function SetupPage({ status, api = setupApi, signIn = signInAfterSetup, goTo = (url) => window.location.assign(url) }: Props) {
  const { language } = useLanguage()
  const [step, setStep] = useState<SetupStep | 'done'>('code')
  const [code, setCode] = useState('')
  const [admin, setAdmin] = useState<SetupAdmin>({ username: '', email: '', password: '', language })
  const [publicUrl, setPublicUrl] = useState(status.lockedPublicUrl ?? window.location.origin)
  const [mail, setMail] = useState<SettingValues | null>(null)
  const [problems, setProblems] = useState<Record<string, string>>({})
  const [finishing, setFinishing] = useState(false)
  const [finishError, setFinishError] = useState<string | null>(null)

  async function finish(keySaved: boolean) {
    setFinishing(true)
    setFinishError(null)
    try {
      await api.complete({ code, admin, publicUrl, mail, encryptionKeySaved: keySaved })
    } catch (error) {
      setFinishing(false)
      if (error instanceof SettingsInvalidError) {
        setProblems(error.errors)
        setStep(Object.keys(error.errors).some((key) => key.startsWith('mail.')) ? 'mail' : 'address')
      } else {
        setFinishError(describeSetupError(error))
      }
      return
    }
    if (new URL(publicUrl).origin === window.location.origin && (await signIn(admin.username, admin.password))) {
      goTo('/')
      return
    }
    setFinishing(false)
    setStep('done')
  }

  return (
    <SetupFrame step={step}>
      {step === 'code' && <CodeStep api={api} onVerified={(verified) => { setCode(verified); setStep('admin') }} />}
      {step === 'admin' && <AdminStep initial={admin} onNext={(next) => { setAdmin(next); setStep('address') }} />}
      {step === 'address' && (
        <AddressStep
          initial={publicUrl}
          locked={status.lockedPublicUrl}
          serverProblem={problems['public-url']}
          onBack={() => setStep('admin')}
          onNext={(url) => { setPublicUrl(url); setProblems({}); setStep('mail') }}
        />
      )}
      {step === 'mail' && (
        <MailStep
          api={api}
          code={code}
          publicUrl={publicUrl}
          recipient={admin.email}
          locked={status.mailLocked}
          initial={mail}
          serverProblems={problems}
          onBack={() => setStep('address')}
          onNext={(values) => { setMail(values); setProblems({}); setStep('key') }}
        />
      )}
      {step === 'key' && (
        <KeyStep
          api={api}
          code={code}
          publicUrl={publicUrl}
          finishing={finishing}
          finishError={finishError}
          onBack={() => setStep('mail')}
          onFinish={(keySaved) => void finish(keySaved)}
        />
      )}
      {step === 'done' && <DoneStep url={publicUrl} />}
    </SetupFrame>
  )
}
