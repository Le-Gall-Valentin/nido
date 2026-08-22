import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check } from 'lucide-react'
import { Alert, Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { SPACE_ACCENTS, SPACE_GLYPHS, type SpaceDetail } from '@/entities/space'
import type { CreateSpaceInput } from '../model/ISpacesPageApi'
import { mapSpaceErrorToKey } from '../lib/mapSpaceErrorToKey'

const NAME_MAX = 80
const DESCRIPTION_MAX = 280

interface CreateSpaceModalProps {
  onClose: () => void
  onCreate: (input: CreateSpaceInput) => Promise<SpaceDetail>
  onSuccess: (created: SpaceDetail) => void
}

export function CreateSpaceModal({ onClose, onCreate, onSuccess }: CreateSpaceModalProps) {
  const { t } = useTranslation('spaces')

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [accent, setAccent] = useState<string>(SPACE_ACCENTS[0])
  const [glyph, setGlyph] = useState<string>(SPACE_GLYPHS[0])
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const pendingRef = useRef(false)

  const trimmedName = name.trim()
  const nameInvalid = trimmedName.length > NAME_MAX
  const descriptionInvalid = description.length > DESCRIPTION_MAX
  const canSubmit = trimmedName.length > 0 && trimmedName.length <= NAME_MAX && description.length <= DESCRIPTION_MAX

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit || pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      const created = await onCreate({
        name: trimmedName,
        description: description.trim() || undefined,
        accent,
        glyph,
      })
      onSuccess(created)
    } catch (error) {
      setErrorKey(mapSpaceErrorToKey(error, 'create'))
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  function handleClose() {
    setErrorKey(null)
    onClose()
  }

  return (
    <Dialog open onClose={handleClose} title={t('create.title')} maxWidth="max-w-lg">
      <div className="mb-5">
        <h3 className="text-xl font-semibold text-fg-0">{t('create.title')}</h3>
      </div>
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <Input
            label={t('create.name')}
            name="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={t('create.name_placeholder')}
            disabled={isLoading}
            autoFocus
          />
        </div>

        <div className="mb-1">
          <Input
            label={t('create.description')}
            name="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder={t('create.description_placeholder')}
            disabled={isLoading}
          />
        </div>

        <div aria-live="polite" className="mb-3">
          {nameInvalid && <p className="text-xs text-status-orange mb-1">{t('create.error.name_length')}</p>}
          {descriptionInvalid && <p className="text-xs text-status-orange mb-1">{t('create.error.description_length')}</p>}
        </div>

        <div className="mb-4">
          <span className="mb-2 block text-[13px] font-semibold text-fg-1">{t('create.accent')}</span>
          <div className="flex flex-wrap gap-2">
            {SPACE_ACCENTS.map((a) => (
              <button
                key={a}
                type="button"
                aria-label={t('create.accent_option', { color: a })}
                aria-pressed={accent === a}
                disabled={isLoading}
                onClick={() => setAccent(a)}
                className="flex size-9 items-center justify-center rounded-full disabled:opacity-50"
                style={{ background: a }}
              >
                {accent === a && <Check className="size-4 text-white" />}
              </button>
            ))}
          </div>
        </div>

        <div className="mb-1">
          <span className="mb-2 block text-[13px] font-semibold text-fg-1">{t('create.glyph')}</span>
          <div className="flex flex-wrap gap-2">
            {SPACE_GLYPHS.map((g) => (
              <button
                key={g}
                type="button"
                aria-label={t('create.glyph_option', { glyph: g })}
                aria-pressed={glyph === g}
                disabled={isLoading}
                onClick={() => setGlyph(g)}
                className={`flex size-9 items-center justify-center rounded-[10px] border-[1.5px] text-lg transition-colors disabled:opacity-50 ${glyph === g ? 'border-accent bg-accent-dim' : 'border-border bg-bg-1 hover:bg-bg-2'}`}
              >
                {g}
              </button>
            ))}
          </div>
        </div>

        {errorKey && (
          <Alert variant="error" className="mt-4">{t(errorKey)}</Alert>
        )}

        <div className="flex justify-end gap-2 mt-5">
          <Button type="button" onClick={handleClose} disabled={isLoading}>
            {t('create.cancel')}
          </Button>
          <Button
            type="submit"
            disabled={!canSubmit}
            isLoading={isLoading}
            className="border-transparent font-semibold"
            style={CTA_BUTTON_STYLE}
          >
            {t('create.submit')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
