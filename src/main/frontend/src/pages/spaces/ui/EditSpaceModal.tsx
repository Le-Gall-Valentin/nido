import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check } from 'lucide-react'
import { Alert, Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { SPACE_ACCENTS, SPACE_GLYPHS, safeAccent, safeGlyph, type SpaceDetail } from '@/entities/space'
import type { UpdateSpaceInput } from '../model/ISpacesPageApi'
import { mapSpaceErrorToKey } from '../lib/mapSpaceErrorToKey'

const NAME_MAX = 80
const DESCRIPTION_MAX = 280

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
  const [errorKey, setErrorKey] = useState<string | null>(null)
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

        <div className="mb-4">
          <span className="mb-2 block text-[13px] font-semibold text-fg-1">{t('edit.accent')}</span>
          <div className="flex flex-wrap gap-2">
            {SPACE_ACCENTS.map((a) => (
              <button
                key={a}
                type="button"
                aria-label={t('edit.accent_option', { color: a })}
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
          <span className="mb-2 block text-[13px] font-semibold text-fg-1">{t('edit.glyph')}</span>
          <div className="flex flex-wrap gap-2">
            {SPACE_GLYPHS.map((g) => (
              <button
                key={g}
                type="button"
                aria-label={t('edit.glyph_option', { glyph: g })}
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
