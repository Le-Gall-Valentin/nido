export function getInitials(username: string): string {
  return (
    username
      .split(/[.\s_-]/)
      .map((s) => s[0] ?? '')
      .join('')
      .slice(0, 2)
      .toUpperCase() || '??'
  )
}