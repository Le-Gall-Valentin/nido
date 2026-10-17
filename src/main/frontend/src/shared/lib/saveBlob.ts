/** How long the file stays fetchable after the click: Firefox and Safari read it after click() returns. */
const KEEP_FILE_MS = 60_000

/**
 * Saves a file the page made. The link is put in the page for the click — some browsers ignore a click on a detached
 * link — and the file is released only later: released at once, the download can fail.
 */
export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.hidden = true
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), KEEP_FILE_MS)
}
