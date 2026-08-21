interface NidoMarkProps {
  size?: number
  className?: string
}

/** Marque Nido : maison/nid en trait, couleur héritée via currentColor. */
export function NidoMark({ size = 22, className }: NidoMarkProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      <path d="M3 11.5 12 4l9 7.5" />
      <path d="M5 10v9a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-9" />
      <path d="M9.5 20v-5a2.5 2.5 0 0 1 5 0v5" />
    </svg>
  )
}
