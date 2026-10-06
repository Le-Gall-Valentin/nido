import { useState } from 'react'
import { copyText } from './copyText'

/**
 * A "Copy" button's state: copied for a moment, or failed — the browser could copy nothing, and the person
 * should hear it rather than paste something else. A copy that works clears an earlier failure.
 */
export function useCopy(copiedForMs = 1500) {
  const [state, setState] = useState<'idle' | 'copied' | 'failed'>('idle')

  async function copy(text: string) {
    if (await copyText(text)) {
      setState('copied')
      setTimeout(() => setState((current) => (current === 'copied' ? 'idle' : current)), copiedForMs)
    } else {
      setState('failed')
    }
  }

  return { copy, copied: state === 'copied', failed: state === 'failed' }
}
