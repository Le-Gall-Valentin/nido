import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import type { SettingsGroup } from '@/entities/instance-settings'
import { Alert, Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { ISettingsApi } from '../model/ISettingsApi'
import { useSettingsGroup } from '../model/useSettingsGroup'
import { GROUP_EXTRAS } from './extras'
import { SettingFieldRow } from './SettingFieldRow'

/** One block of the settings page, saved on its own. What a block adds of its own comes from GROUP_EXTRAS. */
export function SettingsGroupCard({ group, api }: { group: SettingsGroup; api: ISettingsApi }) {
  const { t } = useTranslation('adminSettings')
  const titleId = useId()
  const state = useSettingsGroup(group, api)
  const { Notes, Actions } = GROUP_EXTRAS[group.group] ?? {}

  return (
    <section aria-labelledby={titleId} className="rounded-2xl border border-border bg-bg-1 p-5 sm:p-6">
      <h2 id={titleId} className="text-[17px] font-semibold text-fg-0">{t(`group.${group.group}.title`)}</h2>
      <p className="mt-1 text-[13.5px] leading-relaxed text-fg-2">{t(`group.${group.group}.description`)}</p>

      {/* A form, so that Enter saves the block and the browser knows what the password belongs to. */}
      <form onSubmit={(event) => { event.preventDefault(); state.save() }} noValidate>
        <div className="mt-5 flex flex-col gap-4">
          {group.fields.map((field) => (
            <SettingFieldRow
              key={field.key}
              field={field}
              value={state.values[field.key] ?? ''}
              problem={state.problems[field.key]}
              busy={state.busy}
              onChange={(value) => state.setValue(field.key, value)}
              onReset={() => state.reset(field.key)}
            />
          ))}
        </div>

        {Notes && <Notes group={group} state={state} />}
        {state.notice && <Alert variant="success" className="mt-4">{state.notice}</Alert>}
        {state.failure && <Alert variant="error" className="mt-4">{state.failure}</Alert>}

        {(state.editable || Actions) && (
          <div className="mt-5 flex flex-wrap items-center justify-end gap-2">
            {Actions && <Actions group={group} state={state} />}
            {state.editable && (
              <Button type="submit" className="border-transparent" style={CTA_BUTTON_STYLE} isLoading={state.busy}>
                {t('action.save')}
              </Button>
            )}
          </div>
        )}
      </form>
    </section>
  )
}
