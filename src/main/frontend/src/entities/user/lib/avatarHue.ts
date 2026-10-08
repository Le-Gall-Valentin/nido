/**
 * Where someone's avatar sits on the colour wheel, 0 to 359: FNV-1a over their id, so the same person
 * has the same colour on every screen and every visit, with nothing stored anywhere.
 */
export function avatarHue(userId: string): number {
  let hash = 0x811c9dc5
  for (let i = 0; i < userId.length; i++) {
    hash ^= userId.charCodeAt(i)
    hash = Math.imul(hash, 0x01000193)
  }
  return (hash >>> 0) % 360
}
