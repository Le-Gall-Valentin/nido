const KEY = 'nido.installationSetUp'

/**
 * Whether this browser saw the installation set up. An installation set up stays so — until a database
 * is wiped for a new start, which the answer of the server still catches. Storage refused: no hint.
 */
export function knownSetUp(): boolean {
  try {
    return localStorage.getItem(KEY) === '1'
  } catch {
    return false
  }
}

export function rememberSetUp(setUp: boolean): void {
  try {
    if (setUp) localStorage.setItem(KEY, '1')
    else localStorage.removeItem(KEY)
  } catch {
    // Storage refused (private mode): the application simply waits for the server each time.
  }
}
