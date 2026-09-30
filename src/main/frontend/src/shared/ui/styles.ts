export const CTA_BUTTON_STYLE = {
  background: 'var(--btn-primary-bg)',
  color: 'var(--btn-primary-fg)',
} as const

export const CTA_BUTTON_SHADOW = 'var(--btn-primary-shadow)'

/** The primary button lifted off the page, as the signed-out pages and the dashboard's add menu show it. */
export const CTA_ELEVATED_STYLE = { ...CTA_BUTTON_STYLE, boxShadow: CTA_BUTTON_SHADOW } as const

/** The larger fields of the signed-out pages (login, forgot and reset password). */
export const AUTH_FIELD_CLASS = 'rounded-[11px] px-[15px] py-[13px] text-[15px]'

/** Their full-width submit button; the cursor while it is disabled is the caller's to say. */
export const AUTH_SUBMIT_CLASS = 'mt-2 w-full rounded-[11px] border-transparent py-3.5 text-[15px] font-semibold active:translate-y-px'
