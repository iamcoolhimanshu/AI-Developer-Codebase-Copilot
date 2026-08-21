import { useMemo } from 'react'
import type { ArchitectureGraph } from '../lib/types'

const STEREOTYPE_COLORS: Record<string, string> = {
  Controller: '#38bdf8',
  ControllerAdvice: '#f472b6',
  Service: '#34d399',
  Repository: '#a78bfa',
  Entity: '#fbbf24',
  Configuration: '#f87171',
  Component: '#22d3ee',
  Interface: '#c084fc',
  Model: '#fb923c',
  Other: '#94a3b8',
}

/**
 * Lightweight force-ish layout: shells by stereotype with deterministic
 * placement, edges as cubic beziers. Scales to a few hundred nodes.
 */
export default function ArchGraph({ graph }: { graph: ArchitectureGraph }) {
  const { nodes, edges } = graph

  const { positioned, width, height } = useMemo(() => {
    const groups = new Map<string, typeof nodes>()
    for (const n of nodes) {
      const list = groups.get(n.stereotype) ?? []
      list.push(n)
      groups.set(n.stereotype, list)
    }
    const stereotypes = [...groups.keys()].sort()
    const shellCount = Math.max(1, stereotypes.length)
    const cols = Math.ceil(Math.sqrt(nodes.length || 1))
    const cell = 140
    const width = Math.max(900, cols * cell + 120)
    const height = Math.max(600, shellCount * 260 + 80)

    const pos = new Map<number, { x: number; y: number }>()
    stereotypes.forEach((st, shell) => {
      const list = groups.get(st) ?? []
      const ring = Math.max(90, 70 + shell * 60)
      const cx = width / 2
      const cy = height / 2
      list.forEach((n, i) => {
        const angle = (i / Math.max(1, list.length)) * Math.PI * 2 - Math.PI / 2
        pos.set(n.id, {
          x: cx + Math.cos(angle) * ring,
          y: cy + Math.sin(angle) * ring * 0.8,
        })
      })
    })
    return { positioned: pos, width, height }
  }, [nodes])

  return (
    <div className="overflow-auto rounded-xl border border-slate-800 bg-slate-950/40">
      <svg viewBox={`0 0 ${width} ${height}`} className="min-h-[560px] w-full" style={{ minWidth: width }}>
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="18" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#475569" />
          </marker>
        </defs>
        {edges.map((e, i) => {
          const a = positioned.get(e.source)
          const b = positioned.get(e.target)
          if (!a || !b) return null
          const mx = (a.x + b.x) / 2
          const my = (a.y + b.y) / 2
          const bend = Math.min(40, Math.max(-40, (i % 5) - 2) * 8)
          const d = `M ${a.x} ${a.y} Q ${mx} ${my + bend} ${b.x} ${b.y}`
          return (
            <path key={i} d={d} fill="none" stroke="#475569" strokeWidth={1} markerEnd="url(#arrow)" opacity={0.7} />
          )
        })}
        {nodes.map((n) => {
          const p = positioned.get(n.id)
          if (!p) return null
          const color = STEREOTYPE_COLORS[n.stereotype] ?? STEREOTYPE_COLORS.Other
          const w = Math.min(120, Math.max(64, n.name.length * 7 + 16))
          const h = 30
          return (
            <g key={n.id}>
              <rect
                x={p.x - w / 2}
                y={p.y - h / 2}
                width={w}
                height={h}
                rx={7}
                fill="#0f172a"
                stroke={color}
                strokeWidth={1.4}
              />
              <circle cx={p.x - w / 2 + 11} cy={p.y} r={4} fill={color} />
              <text
                x={p.x + 4}
                y={p.y + 4}
                fontSize={10}
                fill="#e2e8f0"
                textAnchor="middle"
                style={{ pointerEvents: 'none' }}
              >
                {n.name.length > 18 ? n.name.slice(0, 17) + '…' : n.name}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}