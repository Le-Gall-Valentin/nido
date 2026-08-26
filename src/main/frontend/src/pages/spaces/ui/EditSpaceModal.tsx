import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { safeAccent, safeGlyph, type SpaceDetail } from '@/entities/space'
import type { UpdateSpaceInput } from '../model/ISpacesPageApi'
import { mapSpaceErrorToKey } from '../lib/mapSpaceErrorToKey'
import { AppearancePicker, NAME_MAX, DESCRIPTION_MAX } from './AppearancePicker'

interface EditSpaceModalProps {
  space: SpaceDetail
  onClose: () => void
  onUpdate: (patch: UpdateSpaceInput) => Promise<void>
  onSuccess: () => void
}

export function EditSpaceModal({ space, onClose, onUpdate, onSuccess }: EditSpaceModalProps) {
  const { t } = useTranslation('spaces')

  const originalDescription = space.description ?? ''
  const originalAccent = safeAccent(space.accent)
  const originalGlyph = safeGlyph(space.glyph)

  const [name, setName] = useState(space.name)
  const [description, setDescription] = useState(originalDescription)
  const [accent, setAccent] = useState<string>(originalAccent)
  const [glyph, setGlyph] = useState<string>(originalGlyph)
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string[] | null>(null)
  const pendingRef = useRef(false)

  const trimmedName = name.trim()
  const trimmedDescription = description.trim()
  const nameInvalid = trimmedName.length > NAME_MAX
  const descriptionInvalid = trimmedDescription.length > DESCRIPTION_MAX
  const valid = trimmedName.length > 0 && trimmedName.length <= NAME_MAX && trimmedDescription.length <= DESCRIPTION_MAX

  function buildPatch(): UpdateSpaceInput {
    const patch: UpdateSpaceInput = {}
    if (trimmedName !== space.name) patch.name = trimmedName
    if (trimmedDescription !== originalDescription) patch.description = trimmedDescription
    if (accent !== originalAccent) patch.accent = accent
    if (glyph !== originalGlyph) patch.glyph = glyph
    return patch
  }

  const patch = buildPatch()
  const isDirty = Object.keys(patch).length > 0
  const canSubmit = valid && isDirty

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit || pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onUpdate(patch)
      onSuccess()
    } catch (error) {
      setErrorKey(mapSpaceErrorToKey(error, 'edit'))
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
    <Dialog open onClose={handleClose} title={t('edit.title', { name: space.name })} maxWidth="max-w-lg">
      <div className="mb-5">
        <h3 className="text-xl font-semibold text-fg-0">{t('edit.title', { name: space.name })}</h3>
      </div>
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <Input
            label={t('edit.name')}
            name="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={t('edit.name_placeholder')}
            disabled={isLoading}
            autoFocus
          />
        </div>

        <div className="mb-1">
          <Input
            label={t('edit.description')}
            name="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder={t('edit.description_placeholder')}
            disabled={isLoading}
          />
        </div>

        <div aria-live="polite" className="mb-3">
          {nameInvalid && <p className="text-xs text-status-orange mb-1">{t('edit.error.name_length')}</p>}
          {descriptionInvalid && <p className="text-xs text-status-orange mb-1">{t('edit.error.description_length')}</p>}
        </div>

        <AppearancePicker
          prefix="edit"
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
            {t('edit.cancel')}
          </Button>
          <Button
            type="submit"
            disabled={!canSubmit}
            isLoading={isLoading}
            className="border-transparent font-semibold"
            style={CTA_BUTTON_STYLE}
          >
            {t('edit.submit')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
