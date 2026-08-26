import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { SPACE_ACCENTS, SPACE_GLYPHS, type SpaceDetail } from '@/entities/space'
import type { CreateSpaceInput } from '../model/ISpacesPageApi'
import { mapSpaceErrorToKey } from '../lib/mapSpaceErrorToKey'
import { AppearancePicker, NAME_MAX, DESCRIPTION_MAX } from './AppearancePicker'

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
  const [errorKey, setErrorKey] = useState<string[] | null>(null)
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

        <AppearancePicker
          prefix="create"
          accent={accent}
          onAccentChange={setAccent}
          glyph={glyph}
          onGlyphChange={setGlyph}
          disabled={isLoading}
        />

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
