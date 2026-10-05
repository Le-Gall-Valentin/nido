/**
 * Puts `text` in the clipboard; false when nothing could. The browser exposes the clipboard API only in
 * a secure context — https or localhost — and Nido is often reached at http://<address on the network>:
 * there, the old copy command on a selected, invisible field is the only way left.
 */
export async function copyText(text: string): Promise<boolean> {
  if (navigator.clipboard) {
    try {
      await navigator.clipboard.writeText(text)
      return true
    } catch {
      // Refused (permissions): try the copy command below.
    }
  }
  const field = document.createElement('textarea')
  field.value = text
  field.setAttribute('readonly', '')
  field.style.position = 'fixed'
  field.style.opacity = '0'
  document.body.appendChild(field)
  field.focus()
  field.select()
  try {
    // Deprecated, but the one copy a plain-http page can still make.
    return document.execCommand('copy')
  } catch {
    return false
  } finally {
    field.remove()
  }
}
