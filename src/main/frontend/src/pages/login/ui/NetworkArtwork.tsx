import './NetworkArtwork.css'

const nodes = [
  { cx: 60, cy: 60, delay: '0s' },
  { cx: 260, cy: 50, delay: '0.4s' },
  { cx: 40, cy: 180, delay: '0.8s' },
  { cx: 280, cy: 170, delay: '1.2s' },
  { cx: 160, cy: 20, delay: '1.6s' },
  { cx: 160, cy: 220, delay: '2.0s' },
] as const

const particles = [
  { x1: 60, y1: 60, delay: '0s' },
  { x1: 260, y1: 50, delay: '0.7s' },
  { x1: 40, y1: 180, delay: '1.4s' },
  { x1: 280, y1: 170, delay: '2.1s' },
] as const


export function NetworkArtwork() {
  return (
    <svg
      viewBox="0 0 320 240"
      xmlns="http://www.w3.org/2000/svg"
      className="h-auto w-full network-artwork"
      aria-hidden="true"
    >
      <g className="na-edges">
        {nodes.map((n) => (
          <line key={n.delay} x1="160" y1="120" x2={n.cx} y2={n.cy} />
        ))}
      </g>
      <g>
        {nodes.map((n) => (
          <g key={n.delay}>
            <circle cx={n.cx} cy={n.cy} r="14" className="na-pulse">
              <animate attributeName="r" values="12;18;12" dur="3s" begin={n.delay} repeatCount="indefinite" />
              <animate attributeName="opacity" values="0.4;0;0.4" dur="3s" begin={n.delay} repeatCount="indefinite" />
            </circle>
            <circle cx={n.cx} cy={n.cy} r="5" className="na-dot">
              <animate attributeName="opacity" values="0.6;1;0.6" dur="3s" begin={n.delay} repeatCount="indefinite" />
            </circle>
          </g>
        ))}
      </g>
      <circle cx="160" cy="120" r="22" className="na-hub-halo" />
      <circle cx="160" cy="120" r="14" className="na-hub-core" />
      <text x="160" y="126" textAnchor="middle" className="na-hub-label">
        S
      </text>
      {particles.map((p) => (
        <circle key={p.delay} r="2.5" className="na-particle">
          <animate attributeName="cx" values={`${p.x1};160`} dur="2.8s" begin={p.delay} repeatCount="indefinite" />
          <animate attributeName="cy" values={`${p.y1};120`} dur="2.8s" begin={p.delay} repeatCount="indefinite" />
          <animate attributeName="opacity" values="0;1;0" dur="2.8s" begin={p.delay} repeatCount="indefinite" />
        </circle>
      ))}
    </svg>
  )
}